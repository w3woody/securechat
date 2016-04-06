//
//  SCMessageDatabase.h
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

#import <Foundation/Foundation.h>

@class SCMessageSender;
@class SCMessage;

@interface SCMessageDatabase : NSObject

- (id)init;

- (BOOL)openDatabase;

+ (NSString *)databaseFileLocation;
+ (void)removeDatabase;

/*
 *	Insert received message.
 */

- (BOOL)insertMessageFromSenderID:(NSInteger)sender
		name:(NSString *)name
		received:(BOOL)receiveFlag
		withMessageID:(NSInteger)messageID
		timestamp:(NSDate *)timestamp
		message:(NSData *)message;

/*
 *	Contents of database
 */

- (NSArray<SCMessageSender *> *)senders;
- (NSInteger)messageCountForSender:(NSInteger)senderID;
- (NSArray<SCMessage *> *)messagesInRange:(NSRange)range fromSender:(NSInteger)senderID;

/*
 *	Delete messages
 */

- (BOOL)deleteSenderForIdent:(NSInteger)ident;
- (BOOL)deleteMessageForIdent:(NSInteger)ident;

@end
