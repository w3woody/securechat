//
//  SCMessageObject.m
//  SecureChat
//
//  Created by William Woody on 4/26/16.
//  Copyright © 2016 William Edward Woody. All rights reserved.
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


#import "SCMessageObject.h"

@interface SCMessageObject ()
@property (copy) NSString *message;
@end

@implementation SCMessageObject

- (id)initWithData:(NSData *)data
{
	if (nil != (self = [super init])) {
		// TODO: Figure out a better way to handle this.
		self.message = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
	}
	return self;
}

// Create a message to be sent
- (id)initWithString:(NSString *)msg
{
	if (nil != (self = [super init])) {
		self.message = msg;
	}
	return self;
}

// Encode this message as a data blob
- (NSData *)dataFromMessage
{
	return [self.message dataUsingEncoding:NSUTF8StringEncoding];
}

// Message Data
- (NSString *)messageAsText
{
	return self.message;
}

// Summary view content
- (NSString *)summaryMessageText
{
	return self.message;
}


@end
