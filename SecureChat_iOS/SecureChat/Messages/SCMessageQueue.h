//
//  SCMessageQueue.h
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

/*
 *	This is the manager which handles all of the messages that are read
 *	and written on this device.
 */

#import <Foundation/Foundation.h>

@class SCMessageSender;
@class SCMessage;
@class SCMessageObject;

/*
 *	Notification sent when there are new messages to display. This will
 *	get sent as soon as the database is loaded or when the database is
 *	updated with new messages
 */

#define NOTIFICATION_NEWMESSAGE		@"NOTIFICATION_NEWMESSAGE"
#define NOTIFICATION_STARTQUEUE		@"NOTIFICATION_STARTQUEUE"
#define NOTIFICATION_STOPQUEUE		@"NOTIFICATION_STOPQUEUE"
#define NOTIFICATION_ADMINMESSAGE	@"NOTIFICATION_ADMINMESSAGE"

@interface SCMessageQueue : NSObject

+ (SCMessageQueue *)shared;

/*
 *	Message queue startup/shutdown
 */

- (void)startQueue;
- (void)stopQueue;
- (void)clearQueue;

/*
 *	Message queue contents
 */

- (NSArray<SCMessageSender *> *)senders;
- (NSArray<SCMessage *> *)messagesInRange:(NSRange)range fromSender:(NSInteger)senderID;
- (NSInteger)messagesForSender:(NSInteger)senderID;

/*
 *	Delete messages
 */

- (BOOL)deleteSenderForIdent:(NSInteger)ident;
- (BOOL)deleteMessageForIdent:(NSInteger)ident;

/*
 *	Message send. This also handles encryption and enqueuing into our own
 *	internal queue. Note that internally we cache senders and the devices
 *	on which messages are sent for a user, refreshing every 5 minutes.
 */

- (void)sendMessage:(SCMessageObject *)cleartext toSender:(NSString *)sender completion:(void (^)(BOOL success))callback;

@end
