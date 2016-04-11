//
//  SCMessageQueue.m
//  SecureChat
//
//  Created by William Woody on 3/10/16.
//  Copyright © 2016 by William Edward Woody.
//

/*	SecureChat: A secure chat system which permits secure communications 
 *  between iOS devices and a back-end server.
 *
 *	Copyright © 2016 by William Edward Woody
 *
 *	This program is free software: you can redistribute it and/or modify it 
 *	under the terms of the GNU General Public License as published by the 
 *	Free Software Foundation, either version 3 of the License, or (at your 
 *	option) any later version.
 *
 *	This program is distributed in the hope that it will be useful, but 
 *	WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *	or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *	for more details.
 *
 *	You should have received a copy of the GNU General Public License along 
 *	with this program. If not, see <http://www.gnu.org/licenses/>
 */

#import "SCMessageQueue.h"
#import "SCNetwork.h"
#import "SCNetworkResponse.h"
#import "SCInputStream.h"
#import "SCOutputStream.h"
#import "SCRSAManager.h"
#import "SCNetworkCredentials.h"
#import "SCKeychain.h"
#import "SCDateUtils.h"
#import "SCMessageDatabase.h"
#import "SCDeviceCache.h"
#import "SCDevice.h"
#import "SCMessageDeleteQueue.h"

#include "SCSecureHash.h"

#import <sqlite3.h>

/************************************************************************/
/*																		*/
/*	Constants															*/
/*																		*/
/************************************************************************/

#define POLLRATE			5			// If polling, poll every 5 seconds

/************************************************************************/
/*																		*/
/*	Fields																*/
/*																		*/
/************************************************************************/

@interface SCMessageQueue ()

// Polling API fields
@property (assign) NSTimer *timer;
@property (assign) BOOL receiving;

// Asynchronous API fields
@property (strong) SCInputStream *input;
@property (strong) SCOutputStream *output;

// SQLite database of messages
@property (strong) SCMessageDatabase *database;

@end

@implementation SCMessageQueue

/************************************************************************/
/*																		*/
/*	Startup/Shutdown													*/
/*																		*/
/************************************************************************/

+ (SCMessageQueue *)shared
{
	static SCMessageQueue *singleton;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		singleton = [[SCMessageQueue alloc] init];
	});
	return singleton;
}


/************************************************************************/
/*																		*/
/*	Data packet support													*/
/*																		*/
/************************************************************************/

/*	IntegerAtOffset
 *
 *		Pull the 4 byte integer at the offset, in network byte order
 */

static uint32_t IntegerAtOffset(const uint8_t *bytes, int *offset)
{
	uint32_t ret = bytes[(*offset)++];
	ret = (ret << 8) | bytes[(*offset)++];
	ret = (ret << 8) | bytes[(*offset)++];
	ret = (ret << 8) | bytes[(*offset)++];
	return ret;
}

static NSString *StringAtOffset(const uint8_t *bytes, int *offset)
{
	uint16_t ret = bytes[(*offset)++];
	ret = (ret << 8) | bytes[(*offset)++];

	NSString *retStr = [[NSString alloc] initWithBytes:bytes + *offset length:ret encoding:NSUTF8StringEncoding];
	(*offset) += ret;

	return retStr;
}

/************************************************************************/
/*																		*/
/*	Notification Queue													*/
/*																		*/
/************************************************************************/

/*
 *	Internal routine to store the message
 */

- (void)insertMessageFromSenderID:(NSInteger)sender
		name:(NSString *)name
		received:(BOOL)receiveFlag
		withMessageID:(NSInteger)messageID
		timestamp:(NSDate *)timestamp
		message:(NSData *)message
{
	/*
	 *	Determine if this is an admin message (sender == 0), and if it
	 *	is, decrypt and send notification. Otherwise, insert into our
	 *	own database, delete the message from the back end, and
	 *	notify we have a new message.
	 */

	if (sender != 0) {
		if (self.database == nil) return;

		/*
		 *	Step 1: insert into database
		 */

		[self.database insertMessageFromSenderID:sender name:name received:receiveFlag withMessageID:messageID timestamp:timestamp message:message];

		/*
		 *	Step 2: enqueue into delete queue for removal from server
		 */

		[[SCMessageDeleteQueue shared] deleteMessage:messageID withData:message];

		/*
		 *	Step 3: notify the powers that be that we have an update.
		 */

		NSDictionary *d = @{ @"userid": @( sender ),
							 @"username": name };

		[[NSNotificationCenter defaultCenter] postNotificationName:NOTIFICATION_NEWMESSAGE object:self userInfo:d];
	} else {
		/*
		 *	Received message from admin. Process command
		 */

		dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
			/*
			 *	Decrypt the admin message
			 */

			NSData *decrypt = [[SCRSAManager shared] decodeData:message];
			NSDictionary *d = [NSJSONSerialization JSONObjectWithData:decrypt options:0 error:nil];
			if (d) {
				/*
				 *	Post notification for admin message
				 */

				dispatch_async(dispatch_get_main_queue(), ^{
					[[NSNotificationCenter defaultCenter] postNotificationName:NOTIFICATION_ADMINMESSAGE object:self userInfo:d];
				});

				/*
				 *	Step 2: enqueue into delete queue for removal
				 */

				[[SCMessageDeleteQueue shared] deleteMessage:messageID withData:message];
			}
		});
	}
}

/**
 *	Poll for new messages. This makes a query to the back end to get
 *	new messages, and then marks them as deleted
 */

- (void)pollForMessages:(NSTimer *)timer
{
	if (self.receiving) return;
	self.receiving = YES;

	/*
	 *	We're polling.
	 */

	NSDictionary *d = @{ @"deviceid": [[SCRSAManager shared] deviceUUID] };
	[[SCNetwork shared] request:@"messages/getmessages" withParameters:d backgroundRequest:YES caller:self response:^(SCNetworkResponse *response) {
		if (response.success) {
			NSArray *a = response.data[@"messages"];
			for (NSDictionary *d in a) {
				NSInteger messageID = [d[@"messageID"] integerValue];
				NSInteger senderID = [d[@"senderID"] integerValue];
				NSString *senderName = (NSString *)d[@"senderName"];
				NSString *received = (NSString *)d[@"received"];
				BOOL toflag = [d[@"toflag"] boolValue];
				NSDate *timestamp = SCParseServerDate(received);
				NSData *message = [[NSData alloc] initWithBase64EncodedString:(NSString *)d[@"message"] options:NSDataBase64DecodingIgnoreUnknownCharacters];

				[self insertMessageFromSenderID:senderID name:senderName received:toflag withMessageID:messageID timestamp:timestamp message:message];
			}
		}

		self.receiving = NO;
	}];
}

/**
 *	Start polling the connection for messages. This sucks, but we don't
 *	have a network service to talk to
 */

- (void)startPollingServiceWithReason:(NSString *)reason
{
	NSLog(@"Unable to connect to notifications; polling instead. (Reason: %@)",reason);
	self.timer = [NSTimer scheduledTimerWithTimeInterval:POLLRATE target:self selector:@selector(pollForMessages:) userInfo:nil repeats:YES];
}

/************************************************************************/
/*																		*/
/*	Notification Stream													*/
/*																		*/
/************************************************************************/

/*
 *	Notification stream login phase two: this is sent in response to a
 *	token request; this sends the username/password pair for logging in,
 *	as well as the device on this connection that is listening for
 *	messages.
 */

- (void)loginPhaseTwo:(NSString *)token
{
	SCNetworkCredentials *creds = [[SCNetworkCredentials alloc] init];
	creds.username = [[SCRSAManager shared] username];
	creds.password = [[SCRSAManager shared] passwordHash];

	NSDictionary *d = @{ @"cmd": @"login",
						 @"deviceid": [[SCRSAManager shared] deviceUUID],
						 @"username": creds.username,
						 @"password": [creds hashPasswordWithToken:token] };
	[self.output writeData:[NSJSONSerialization dataWithJSONObject:d options:0 error:nil]];
}

/*
 *	Process a data packet from the back end notification service. A data
 *	packet response from the back end has the format:
 *
 *		first byte
 *		0x20		Message (to be defined later)
 *		0x21		Token response
 *		0x22		Login response failure
 *
 *	Note login success is implicit; if login worked, we start receiving
 *	message notifications, starting with the backlog of stored messages
 *	waiting for this.
 */

- (void)processDataPacket:(NSData *)data
{
	if (data.length == 0) return;
	const uint8_t *buffer = (const uint8_t *)data.bytes;

	if (buffer[0] == 0x20) {
		/*
		 *	Process received message
		 *
		 *	Format is:
		 *
		 *	offset	length	value
		 *	0		1		0x20
		 *	1		1		toflag
		 *	2		4		messageid
		 *	6		4		senderid
		 *	10		var		timestamp (as string, preceeded with 2 byte len)
		 *			var		sender (as string, preceeded with 2 byte len)
		 *			4		message length
		 *			len		message (as binary)
		 */

		int offset = 1;
		BOOL toflag = (buffer[offset++]) ? YES : NO;
		uint32_t messageID = IntegerAtOffset(buffer, &offset);
		uint32_t senderID = IntegerAtOffset(buffer, &offset);
		NSString *ts = StringAtOffset(buffer, &offset);
		NSString *senderName = StringAtOffset(buffer, &offset);
		uint32_t messagelen = IntegerAtOffset(buffer, &offset);
		NSData *data = [[NSData alloc] initWithBytes:buffer + offset length:messagelen];
		offset += messagelen;

		// additional fields would appear here.

		[self insertMessageFromSenderID:senderID name:senderName received:toflag withMessageID:messageID timestamp:SCParseServerDate(ts) message:data];


	} else if (buffer[0] == 0x21) {
		/*
		 *	Received token. Rest is a string.
		 */

		NSString *token = [[NSString alloc] initWithBytes:buffer+1 length:data.length-1 encoding:NSUTF8StringEncoding];
		[self loginPhaseTwo:token];

	} else if (buffer[0] == 0x22) {
		/*
		 *	Login failure. Could not log in. This shuts down the
		 *	notification service and reverts to a polled connection
		 */

		[self closeConnection];
		[self startPollingServiceWithReason:@"Login failure"];
	}
}

/*
 *	Close connection
 */

- (void)closeConnection
{
	[self.input close];
	[self.output close];
	self.input = nil;
	self.output = nil;
}

/**
 *	The back end is advertising a port we can connect to for asynchronous
 *	networking. Attempt to open a connection. If this fails we fall back
 *	to polling
 */

- (void)openConnection:(NSString *)host port:(NSInteger)port ssl:(BOOL)flag
{
	CFReadStreamRef readStream;
	CFWriteStreamRef writeStream;

	CFStreamCreatePairWithSocketToHost(kCFAllocatorDefault, (__bridge CFStringRef)host, (uint32_t)port, &readStream, &writeStream);

	NSInputStream *inStream = (__bridge_transfer NSInputStream *)readStream;
	NSOutputStream *outStream = (__bridge_transfer NSOutputStream *)writeStream;

	/*
	 *	Set as secure if the flag is set to true
	 */

	if (flag) {
		/*
		 *	This was a pain, but it turns out you need to set the SSL settings
		 *	which skip validation *AFTER* setting the negotiated SSL level.
		 *	Otherwise, the settings are overwritten.
		 */
		
		NSDictionary *d = @{ (NSString *)kCFStreamSSLValidatesCertificateChain: (id)kCFBooleanFalse };
		[inStream setProperty:NSStreamSocketSecurityLevelNegotiatedSSL forKey:NSStreamSocketSecurityLevelKey];
		[inStream setProperty:d forKey:(id)kCFStreamPropertySSLSettings];
	}

	/*
	 *	Discussion: 
	 *
	 *		The input and output streams are both connected to the same
	 *	TCP/IP endpoint, even though we treat them as separate objects.
	 *	Further, our input stream is handled synchronously on a separate
	 *	thread, while our output stream runs on the main thread using
	 *	the main thread run loop for processing.
	 *
	 *		So we basically see if we failed by listening to the writer
	 *	on the main thread. The read thread, on the other hand, we just
	 *	plop into a background thread. This also makes logging in a little
	 *	interesting, but we handle that by assuming that the user's password
	 *	hasn't changed since asking for the notifications port.
	 */

	/*
	 *	Kick off the background thread for read processing
	 */

	__weak SCMessageQueue *ref = self;

	self.input = [[SCInputStream alloc] initWithInputStream:inStream];
	self.input.processPacket = ^(NSData *data) {
		[ref processDataPacket:data];
	};
	dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
		// Start the reader. When the reader finishes we simply allow the
		// thread to die. We use the writer status to determine if the
		// connection couldn't be opened.
		[inStream open];
		[self.input processStream];
	});

	/*
	 *	Kick off the writer for packet writing
	 */

	self.output = [[SCOutputStream alloc] initWithOutputStream:outStream];
	self.output.eventCallback = ^(NSStreamEvent event) {
		if (event == NSStreamEventErrorOccurred) {
			// Problem connecting. Presume the reader will fail as well,
			// and kick off synchronous polling
			ref.input = nil;
			ref.output = nil;
			[ref startPollingServiceWithReason:@"Unable to open connection"];
		}
	};
	[self.output open];

	/*
	 *	Now the first packet we need to send to the writer (and our
	 *	output stream will cache this) is a JSON request to log in.
	 *
	 *	On the off chance logging in fails, the back end will simply
	 *	close the connection.
	 *
	 *	Because there is no one-to-one (in theory) of data sent and
	 *	received, we drive this through a state machine.
	 */

	NSDictionary *d = @{ @"cmd": @"token" };
	[self.output writeData:[NSJSONSerialization dataWithJSONObject:d options:0 error:nil]];
}

/**
 *	Ask the back end if we can connect to a separate port for async
 *	message handling; if not, start a background polling timer
 */

- (void)startNetworkQueue
{
	[[SCNetwork shared] request:@"messages/notifications" withParameters:nil backgroundRequest:YES caller:self response:^(SCNetworkResponse *response) {
		/*
		 *	If we get here but we're already running, bail. This is a
		 *	sanity check if we get two start requests in a row on a slow
		 *	network connection.
		 */

		if (self.input || self.output || self.timer) return;

		/*
		 *	Success or error?
		 */

		if (response.success) {
			/*
			 *	Pull the parameters and attempt to open a connection for
			 *	async messages
			 */

			NSString *host = response.data[@"host"];
			NSNumber *port = response.data[@"port"];
			NSNumber *useSSL = response.data[@"ssl"];
			[self openConnection:host port:port.integerValue ssl:useSSL.boolValue];

		} else {
			/*
			 *	Async services are not available
			 */

			[self startPollingServiceWithReason:@"Server reported unavailable"];
		}
	}];
}


/************************************************************************/
/*																		*/
/*	External Methods													*/
/*																		*/
/************************************************************************/

/**
 *	Start the message queue. This makes sure the messages are loaded from
 *	memory and starts either the periodic timer or the direct network
 *	connection to the server to send and receive messages
 */

- (void)startQueue
{
	if (![[SCRSAManager shared] canStartServices]) return;

	/*
	 *	Open the database
	 */

	if (self.database == nil) {
		self.database = [[SCMessageDatabase alloc] init];
		[self.database openDatabase];
	}

	/*
	 *	Start network queue by asking if there is a port we can directly
	 *	connect to.
	 */

	[self startNetworkQueue];

	[[NSNotificationCenter defaultCenter] postNotificationName:NOTIFICATION_STARTQUEUE object:self];
}

- (void)stopQueue
{
	[[NSNotificationCenter defaultCenter] postNotificationName:NOTIFICATION_STOPQUEUE object:self];

	/*
	 *	If we have a connection, close. Otherwise stop polling. Note
	 *	if we don't have the queue in progress this does nothing.
	 */

	if (self.input || self.output) {
		/*
		 *	This is a bit brute force
		 */

		[self.input close];
		[self.output close];
		self.input = nil;
		self.output = nil;
	} else {
		/*
		 *	Polling close
		 */

		[self.timer invalidate];
		self.timer = nil;
	}

	/*
	 *	Close the database
	 */

	self.database = nil;
}

- (void)clearQueue
{
	self.database = nil;
	if (self.timer) {
		[self.timer invalidate];
		self.timer = nil;
	}
	[self closeConnection];

	[SCMessageDatabase removeDatabase];
}


/************************************************************************/
/*																		*/
/*	Database Access														*/
/*																		*/
/************************************************************************/

- (NSArray<SCMessageSender *> *)senders
{
	return self.database.senders;
}

- (NSInteger)messagesForSender:(NSInteger)senderID
{
	return [self.database messageCountForSender:senderID];
}

- (NSArray<SCMessage *> *)messagesInRange:(NSRange)range fromSender:(NSInteger)senderID
{
	return [self.database messagesInRange:range fromSender:senderID];
}

- (BOOL)deleteSenderForIdent:(NSInteger)ident
{
	return [self.database deleteSenderForIdent:ident];
}

- (BOOL)deleteMessageForIdent:(NSInteger)ident
{
	return [self.database deleteMessageForIdent:ident];
}

/************************************************************************/
/*																		*/
/*	Sending Messages													*/
/*																		*/
/************************************************************************/

/*
 *	Message send. This also handles encryption and enqueuing into our own
 *	internal queue. Note that internally we cache senders and the devices
 *	on which messages are sent for a user, refreshing every 5 minutes.
 */

- (void)sendMessage:(NSString *)cleartext toSender:(NSString *)sender completion:(void (^)(BOOL success))callback
{
	void (^copyCallback)(BOOL success) = [callback copy];

	/*
	 *	First, get the devices for this sender
	 */

	[[SCDeviceCache shared] devicesFor:sender withCallback:^(NSInteger userID, NSArray<SCDevice *> *devarray) {
		if (devarray == nil) {
			copyCallback(NO);
		}

		/*
		 *	Get our devices. If this goes haywire, we don't know so we
		 *	bail.
		 */

		NSString *me = [[SCRSAManager shared] username];
		[[SCDeviceCache shared] devicesFor:me withCallback:^(NSInteger myUserID, NSArray<SCDevice *> *myarray) {
			if (myarray == nil) {
				copyCallback(NO);
			}


			/*
			 *	Now encode the message and send to the back end. Note that we
			 *	run this on a background thread
			 */


			dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{

				/*
				 *	Calculate message checksum
				 */

				NSData *cdata = [cleartext dataUsingEncoding:NSUTF8StringEncoding];
				SCSHA256Context hasher;
				hasher.Start();
				hasher.Update(cdata.length, (const uint8_t *)cdata.bytes);
				uint8_t output[32];
				hasher.Finish(output);

				char buffer[80];
				for (int i = 0; i < 32; ++i) {
					sprintf(buffer + i*2,"%02x",output[i]);
				}
				NSString *checksum = [NSString stringWithUTF8String:buffer];

				/*
				 *	Now build the encoding list to encode all the sent
				 *	messages
				 */

				NSMutableArray *messages = [[NSMutableArray alloc] init];

				// Devices of the person we're sending messages to
				for (SCDevice *d in devarray) {
					SCRSAEncoder *encoder = d.publickey;
					NSData *encoded = [encoder encodeData:cdata];
					NSString *message = [encoded base64EncodedStringWithOptions:0];

					NSDictionary *ds = @{ @"checksum": checksum,
										  @"message": message,
										  @"deviceid": d.deviceid };
					[messages addObject:ds];
				}

				// My own devices, so I can track the conversations.
				for (SCDevice *d in myarray) {
					NSString *deviceid = d.deviceid;
					if (![deviceid isEqualToString:[[SCRSAManager shared] deviceUUID]]) {
						SCRSAEncoder *encoder = d.publickey;
						NSData *encoded = [encoder encodeData:cdata];
						NSString *message = [encoded base64EncodedStringWithOptions:0];

						NSDictionary *ds = @{ @"checksum": checksum,
											  @"message": message,
											  @"deviceid": deviceid,
											  @"destuser": @( userID ) };
						[messages addObject:ds];
					}
				}

				// Encode for myself. This is kind of a kludge; we need
				// the message ID from the back end to assure proper sorting.
				// But we only get that if this is the last message in the
				// array of messages. (See SendMessages.java.)

				SCRSAEncoder *encoder = [[SCRSAEncoder alloc] initWithEncoderKey:[[SCRSAManager shared] publicKey]];
				NSData *encoded = [encoder encodeData:cdata];
				NSString *message = [encoded base64EncodedStringWithOptions:0];
				NSDictionary *ds = @{ @"checksum": checksum,
									  @"message": message,
									  @"deviceid": [[SCRSAManager shared] deviceUUID],
									  @"destuser": @( userID ) };
				[messages addObject:ds];

				/*
				 *	And send all the message data to the back end.
				 */

				NSDictionary *params = @{ @"messages": messages };
				[[SCNetwork shared] request:@"messages/sendmessages" withParameters:params backgroundRequest:YES caller:self response:^(SCNetworkResponse *response) {

					if (response.success) {
						NSInteger messageID = [response.data[@"messageid"] integerValue];

						// Insert sent message into myself. This is so we
						// immediately see the sent message right away.
						// Note we may have a race condition but we don't
						// care; the messageID will screen out duplicates.

						[self insertMessageFromSenderID:userID name:sender received:YES withMessageID:messageID timestamp:[NSDate date] message:encoded];
					}

					copyCallback(response.success);
				}];
			});
		}];
	}];
}


@end
