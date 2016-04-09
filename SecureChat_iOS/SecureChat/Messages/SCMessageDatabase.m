//
//  SCMessageDatabase.m
//  SecureChat
//
//  Created by William Woody on 3/14/16.
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

#import "SCMessageDatabase.h"
#import "SCMessageSender.h"
#import "SCMessage.h"
#import <sqlite3.h>

@interface SCMessageDatabase ()

// SQLite database of messages
@property (assign) sqlite3 *database;

// Note: our design assumes no more than perhaps a hundred senders or so,
// as some operations perform an O(N) search against this list. If we have
// more senders than that, we can replace this with something more complex,
// such as a mutable dictionary.
@property (strong) NSMutableArray<SCMessageSender *> *cachedSenders;

// We also cache messages. We assume a single screen showing the messages
// between sender and receiver; the loaded range allows us to avoid
// constant queries to the back end just because the user scrolls down one
// or two rows.
@property (assign) NSInteger senderID;
@property (assign) NSRange loadedRange;
@property (strong) NSMutableArray<SCMessage *> *cachedMessages;

@end

/************************************************************************/
/*																		*/
/*	Startup/Shutdown													*/
/*																		*/
/************************************************************************/

@implementation SCMessageDatabase

- (id)init
{
	if (nil != (self = [super init])) {
	}
	return self;
}

- (void)dealloc
{
	[self closeDatabase];
}

/************************************************************************/
/*																		*/
/*	Database startup/shutdown											*/
/*																		*/
/************************************************************************/

/**
 *	Get the database file path, making sure we have what we expect
 */

+ (NSString *)databaseFileLocation
{
	NSArray<NSString *> *paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES);
	NSString *path = paths[0];
	path = [path stringByAppendingPathComponent:@"SecureChat"];

	/*
	 *	Make sure SecureChat exists
	 */

	if (![[NSFileManager defaultManager] fileExistsAtPath:path]) {
		[[NSFileManager defaultManager] createDirectoryAtPath:path withIntermediateDirectories:YES attributes:nil error:nil];
	}

	path = [path stringByAppendingPathComponent:@"messages.db"];
//#if defined(DEBUG)
//	NSLog(@"Path: %@",path);
//#endif
	return path;
}

/*
 *	Create the tables in our database if they don't exist
 */

- (void)initializeDatabase:(sqlite3 *)db
{
	char *errMsg;
	int err;

	/*
	 *	Create a version row for later use. (In the future we can use the
	 *	version number in order to intelligently upgrade our SQL database.)
	 */

	const char *version = "CREATE TABLE IF NOT EXISTS version ( "
						  "  versionid INTEGER UNIQUE ON CONFLICT IGNORE )";

	err = sqlite3_exec(db, version, NULL, NULL, &errMsg);
	if (err != SQLITE_OK) {
		NSLog(@"SQL Error %s\n",errMsg);
	}

	const char *verval = "INSERT INTO version ( versionid ) VALUES ( 1 )";

	err = sqlite3_exec(db, verval, NULL, NULL, NULL);
	if (err != SQLITE_OK) {
		NSLog(@"SQL Error %s\n",errMsg);
	}


	/*
	 *	Now create the tables (well, *table*). Note that the contents of
	 *	the MESSAGE blob is encoded using this user's public key.
	 */

	const char *create = "CREATE TABLE IF NOT EXISTS messages ( "
						 "  rowid INTEGER PRIMARY KEY AUTOINCREMENT, "
						 "  messageid INTEGER, "
						 "  received INTEGER, "		// 1 if this was a response
						 "  senderid INTEGER, "		// sender or recipient
						 "  timestamp REAL, "		// in seconds past epoch
						 "  sender TEXT, "
						 "  message BLOB )";

	err = sqlite3_exec(db, create, NULL, NULL, NULL);
	if (err != SQLITE_OK) {
		NSLog(@"SQL Error %s\n",errMsg);
	}

	const char *index1 = "CREATE UNIQUE INDEX IF NOT EXISTS messagesix1 "
						 "ON messages ( messageid )";

	err = sqlite3_exec(db, index1, NULL, NULL, NULL);
	if (err != SQLITE_OK) {
		NSLog(@"SQL Error %s\n",errMsg);
	}

	const char *index2 = "CREATE INDEX IF NOT EXISTS messagesix2 "
						 "ON messages ( senderid )";

	err = sqlite3_exec(db, index2, NULL, NULL, NULL);
	if (err != SQLITE_OK) {
		NSLog(@"SQL Error %s\n",errMsg);
	}

	const char *index3 = "CREATE INDEX IF NOT EXISTS messagesix3 "
						 "ON messages ( timestamp )";

	err = sqlite3_exec(db, index3, NULL, NULL, NULL);
	if (err != SQLITE_OK) {
		NSLog(@"SQL Error %s\n",errMsg);
	}
}

/**
 *	Opens or creates the message database. We do this in a directory that
 *	is not backed up
 */

- (BOOL)openDatabase
{
	sqlite3 *db;
	int err;

	/*
	 *	Check if database is still open
	 */

	if (self.database) return YES;

	/*
	 *	Determine if the database directory exists or not
	 */

	NSString *path = [SCMessageDatabase databaseFileLocation];

	/*
	 *	Create or open the database.
	 */

	err = sqlite3_open([path UTF8String], &db);
	if (err != SQLITE_OK) {
		NSLog(@"Unable to open database file. Deleting and retrying");
		sqlite3_close(db);

		[[NSFileManager defaultManager] removeItemAtPath:path error:nil];

		err = sqlite3_open([path UTF8String], &db);
		if (err != SQLITE_OK) {
			NSLog(@"Okay, this is tragic. We cannot open even after erasing the old file. Panic.");
			return NO;
		}
	}

	/*
	 *	Store away database and initialize it.
	 */

	self.database = db;
	[self initializeDatabase:db];
	return YES;
}

/*
 *	Close the message database
 */

- (void)closeDatabase
{
	if (self.database) {
		sqlite3_close(self.database);
		self.database = nil;
	}
}

/*
 *	Close and delete the database
 */

+ (void)removeDatabase
{
	const char *name = [[SCMessageDatabase databaseFileLocation] UTF8String];
	unlink(name);
}

/************************************************************************/
/*																		*/
/*	Basic operations													*/
/*																		*/
/************************************************************************/

/*
 *	Insert a received message.
 */

- (BOOL)insertMessageFromSenderID:(NSInteger)sender
		name:(NSString *)name
		received:(BOOL)receiveFlag
		withMessageID:(NSInteger)messageID
		timestamp:(NSDate *)timestamp
		message:(NSData *)message
{
	if (self.database == nil) return NO;	// database not open.

	int err;
	sqlite3_stmt *stmt;

	/*
	 *	Preflight: if we don't have a timestamp, use now as the timestamp.
	 */

	if (timestamp == nil) timestamp = [[NSDate alloc] init];

	/*
	 *	Prepare the insert
	 */

	const char *statement = "INSERT OR ABORT INTO Messages "
							"    ( messageid, received, senderid, sender, "
							"      timestamp, message ) "
							"VALUES "
							"    ( ?, ?, ?, ?, ?, ? )";

	err = sqlite3_prepare(self.database, statement, -1, &stmt, NULL);
	if (err) {
		NSLog(@"Prepare statement SQL error");
		return NO;
	}

	const char *cname = name.UTF8String;

	sqlite3_bind_int(stmt, 1, (int)messageID);
	sqlite3_bind_int(stmt, 2, receiveFlag ? 1 : 0);
	sqlite3_bind_int(stmt, 3, (int)sender);
	sqlite3_bind_text(stmt, 4, cname, -1, SQLITE_STATIC);
	sqlite3_bind_double(stmt, 5, (double)[timestamp timeIntervalSince1970]);
	sqlite3_bind_blob(stmt, 6, message.bytes, (int)message.length, SQLITE_STATIC);

	err = sqlite3_step(stmt);
	if (err != SQLITE_DONE) {
		sqlite3_finalize(stmt);
		return NO;

	}
	sqlite3_finalize(stmt);

	/*
	 *	Delete cached messages
	 */

	self.cachedMessages = nil;

	/*
	 *	Now if we have a new message, update the sender state. If the sender
	 *	is not in the list, we wipe out the cached list of senders, so that
	 *	the senders call requeries the database.
	 */

	NSUInteger i,len = self.cachedSenders.count;
	for (i = 0; i < len; ++i) {
		SCMessageSender *s = self.cachedSenders[i];
		if (s.senderID == sender) break;
	}

	if (i < len) {
		SCMessageSender *s = self.cachedSenders[i];
		if (messageID > s.messageID) {
			s.lastMessage = message;
			s.messageID = messageID;
			s.lastSent = timestamp;
			s.receiveFlag = receiveFlag;

			// Move to top of list. Keep in mind we're assuming the
			// number of senders here is small.

			[self.cachedSenders removeObjectAtIndex:i];
			[self.cachedSenders insertObject:s atIndex:0];
		}
	} else {
		self.cachedSenders = nil;
	}

	return YES;
}

/**
 *	Perform a "group by" query to get the senders.
 */

- (NSArray<SCMessageSender *> *)senders
{
	int err;
	sqlite3_stmt *stmt;

	if (self.cachedSenders) return self.cachedSenders;

	/*
	 *	Run grouped query to get the list of senders and the last messages
	 *	associated with them. Note we order by the messageid from the
	 *	server (even if messages have been removed from the server) as this
	 *	assures the order in which messages were really received.
	 */

	const char *statement = "SELECT messageid, received, senderid, "
							"    sender, timestamp, message "
							"FROM messages "
							"WHERE messageid IN ( "
							"    SELECT max(messageid) "
							"    FROM messages "
							"    GROUP BY senderid ) "
							"ORDER BY timestamp";

	err = sqlite3_prepare(self.database, statement, -1, &stmt, NULL);
	if (err) {
		NSLog(@"Prepare statement SQL error");
		return nil;
	}

	self.cachedSenders = [[NSMutableArray alloc] init];
	while (SQLITE_ROW == (err = sqlite3_step(stmt))) {
		int messageID = sqlite3_column_int(stmt, 0);
		int received = sqlite3_column_int(stmt, 1);
		int senderID = sqlite3_column_int(stmt, 2);

		const unsigned char *sender = sqlite3_column_text(stmt, 3);
		NSString *sendername = [[NSString alloc] initWithUTF8String:(const char *)sender];

		NSDate *date = [NSDate dateWithTimeIntervalSince1970:sqlite3_column_double(stmt, 4)];

		const void *data = sqlite3_column_blob(stmt, 5);
		int dataSize = sqlite3_column_bytes(stmt, 5);
		NSData *message = [[NSData alloc] initWithBytes:data length:dataSize];

		SCMessageSender *s = [[SCMessageSender alloc] init];
		s.senderID = senderID;
		s.senderName = sendername;
		s.lastMessage = message;
		s.lastSent = date;
		s.receiveFlag = received ? YES : NO;
		s.messageID = messageID;

		[self.cachedSenders addObject:s];
	}

	sqlite3_finalize(stmt);
	return self.cachedSenders;
}

- (NSInteger)messageCountForSender:(NSInteger)senderID
{
	int err;
	sqlite3_stmt *stmt;

	const char *statement = "SELECT COUNT(*) "
							"FROM messages "
							"WHERE senderid = ? ";

	err = sqlite3_prepare(self.database, statement, -1, &stmt, NULL);
	if (err) {
		NSLog(@"Prepare statement SQL error");
		return 0;
	}

	sqlite3_bind_int(stmt, 1, (int)senderID);

	NSInteger count = 0;
	if (SQLITE_ROW == (err = sqlite3_step(stmt))) {
		count = sqlite3_column_int(stmt, 0);
	}
	sqlite3_finalize(stmt);

	return count;
}

/**
 *	Get the rows by range for this sender, in order
 */

- (NSArray<SCMessage *> *)messagesInRange:(NSRange)range fromSender:(NSInteger)senderID
{
	int err;
	sqlite3_stmt *stmt;

	if (self.cachedMessages && (self.senderID == senderID) && NSEqualRanges(range, self.loadedRange)) {
		return self.cachedMessages;
	}

	/*
	 *	Run the query
	 */

	const char *statement = "SELECT messageid, message, received, timestamp "
							"FROM messages "
							"WHERE senderid = ? "
							"ORDER BY messageid ASC "
							"LIMIT ? OFFSET ?";

	err = sqlite3_prepare(self.database, statement, -1, &stmt, NULL);
	if (err) {
		NSLog(@"Prepare statement SQL error");
		return nil;
	}

	sqlite3_bind_int(stmt, 1, (int)senderID);
	sqlite3_bind_int(stmt, 2, (int)range.length);
	sqlite3_bind_int(stmt, 3, (int)range.location);

	self.cachedMessages = [[NSMutableArray alloc] init];
	while (SQLITE_ROW == (err = sqlite3_step(stmt))) {
		int messageID = sqlite3_column_int(stmt, 0);

		const void *data = sqlite3_column_blob(stmt, 1);
		int dataSize = sqlite3_column_bytes(stmt, 1);
		NSData *message = [[NSData alloc] initWithBytes:data length:dataSize];

		int received = sqlite3_column_int(stmt, 2);

		NSDate *date = [NSDate dateWithTimeIntervalSince1970:sqlite3_column_double(stmt, 3)];

		SCMessage *s = [[SCMessage alloc] init];
		s.receiveFlag = received ? YES : NO;
		s.messageID = messageID;
		s.message = message;
		s.timestamp = date;

		[self.cachedMessages addObject:s];
	}
	sqlite3_finalize(stmt);

	/*
	 *	Store the parameters which achieved this result and return
	 */
	
	self.senderID = senderID;
	self.loadedRange = range;
	return self.cachedMessages;
}

- (BOOL)deleteSenderForIdent:(NSInteger)ident
{
	if (self.database == nil) return NO;	// database not open.

	int err;
	sqlite3_stmt *stmt;

	/*
	 *	Prepare the delete
	 */

	const char *statement = "DELETE FROM Messages "
							"WHERE senderid = ?";

	err = sqlite3_prepare(self.database, statement, -1, &stmt, NULL);
	if (err) {
		NSLog(@"Prepare statement SQL error");
		return NO;
	}

	sqlite3_bind_int(stmt, 1, (int)ident);

	err = sqlite3_step(stmt);
	if (err != SQLITE_DONE) {
		sqlite3_finalize(stmt);
		return NO;
	}
	sqlite3_finalize(stmt);

	/*
	 *	Delete cached messages
	 */

	self.cachedSenders = nil;
	self.cachedMessages = nil;

	return YES;
}

- (BOOL)deleteMessageForIdent:(NSInteger)ident
{
	if (self.database == nil) return NO;	// database not open.

	int err;
	sqlite3_stmt *stmt;

	/*
	 *	Prepare the delete
	 */

	const char *statement = "DELETE FROM Messages "
							"WHERE messageid = ?";

	err = sqlite3_prepare(self.database, statement, -1, &stmt, NULL);
	if (err) {
		NSLog(@"Prepare statement SQL error");
		return NO;
	}

	sqlite3_bind_int(stmt, 1, (int)ident);

	err = sqlite3_step(stmt);
	if (err != SQLITE_DONE) {
		sqlite3_finalize(stmt);
		return NO;
	}
	sqlite3_finalize(stmt);

	/*
	 *	Delete cached messages
	 */

	self.cachedSenders = nil;
	self.cachedMessages = nil;

	return YES;
}


@end
