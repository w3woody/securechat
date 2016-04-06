//
//  SCSecuredData.m
//  SecureChat
//
//  Created by William Woody on 2/26/16.
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

#import "SCSecuredData.h"
#import "SCKeychain.h"
#include "SCChecksum.h"

@interface SCSecuredData ()
@property (assign) BOOL isLoaded;
@property (assign) BOOL isDirty;
@property (assign) NSTimer *eraseTimer;
@property (assign) NSTimeInterval loadTime;
@end

#define ERASETIME			5.0			// values erased in 5 seconds

@implementation SCSecuredData

/*
 *	String data
 */

+ (void)appendString:(NSString *)str toData:(NSMutableData *)buffer
{
	if (str == nil) str = @"";

	NSData *data = [str dataUsingEncoding:NSUTF8StringEncoding];
	uint32_t len = (uint32_t)data.length;
	[buffer appendBytes:&len length:sizeof(len)];
	[buffer appendData:data];
}

+ (NSString *)stringFromData:(NSData *)data position:(uint32_t *)pos
{
	uint32_t length = (uint32_t)data.length;
	uint32_t len;

	NSString *retStr = @"";

	uint8_t *bytes = (uint8_t *)data.bytes;
	if (*pos + sizeof(len) < length) {
		[data getBytes:&len range:NSMakeRange(*pos, sizeof(len))];
		*pos += sizeof(len);
		if (len + *pos < length) {
			retStr = [[NSString alloc] initWithBytes:bytes + *pos length:len encoding:NSUTF8StringEncoding];
			*pos += len;
		}
	}

	return retStr;
}

/*
 *	Serialize the data 
 */

- (NSData *)serializeData
{
	NSMutableData *ret = [[NSMutableData alloc] init];

	/*
	 *	Append strings
	 */

	[SCSecuredData appendString:self.uuid toData:ret];
	[SCSecuredData appendString:self.publicKey toData:ret];
	[SCSecuredData appendString:self.privateKey toData:ret];

	[SCSecuredData appendString:self.username toData:ret];
	[SCSecuredData appendString:self.password toData:ret];
	[SCSecuredData appendString:self.serverURL toData:ret];

	/*
	 *	Grow to align to 8 bytes minus one
	 */

	NSUInteger size = ret.length + 1;
	if (size % 8) {
		char buffer[8];
		memset(buffer,0,sizeof(buffer));
		[ret appendBytes:buffer length:8 - size % 8];
	}

	/*
	 *	Append checksum. It's a weak one, deliberately
	 */

	uint8_t checksum = SCCalcCRC8(0, ret.bytes, ret.length);
	[ret appendBytes:&checksum length:sizeof(checksum)];

	return ret;
}

/*
 *	Deserialize the data
 */

+ (SCSecuredData *)deserializeData:(NSData *)data
{
	if (data == nil) return nil;
	
	uint32_t length = (uint32_t)data.length;
	const uint8_t *bytes = data.bytes;

	/*
	 *	Get the checksum as the last byte
	 */

	uint8_t checksum = bytes[length-1];
	if (checksum != SCCalcCRC8(0, bytes, length-1)) return nil;

	/*
	 *	Read the data
	 */

	uint32_t pos = 0;
	SCSecuredData *ret = [[SCSecuredData alloc] init];

	ret.uuid = [SCSecuredData stringFromData:data position:&pos];
	ret.publicKey = [SCSecuredData stringFromData:data position:&pos];
	ret.privateKey = [SCSecuredData stringFromData:data position:&pos];

	ret.username = [SCSecuredData stringFromData:data position:&pos];
	ret.password = [SCSecuredData stringFromData:data position:&pos];
	ret.serverURL = [SCSecuredData stringFromData:data position:&pos];

	return ret;
}

@end
