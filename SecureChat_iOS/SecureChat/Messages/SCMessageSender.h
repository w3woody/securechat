//
//  SCMessageSender.h
//  SecureChat
//
//  Created by William Woody on 3/13/16.
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

/*
 *	The sender for each of the messages, with the last message that they
 *	sent.
 */

@interface SCMessageSender : NSObject

@property (assign) NSInteger senderID;
@property (copy) NSString *senderName;
@property (copy) NSData *lastMessage;
@property (assign) NSDate *lastSent;
@property (assign) BOOL receiveFlag;
@property (assign) NSInteger messageID;

@end
