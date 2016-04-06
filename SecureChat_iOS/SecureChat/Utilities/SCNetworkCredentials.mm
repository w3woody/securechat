//
//  SCNetworkCredentials.m
//  SecureChat
//
//  Created by William Woody on 3/6/16.
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

#import "SCNetworkCredentials.h"
#include "SCSecureHash.h"

/*
 *	Internal support for calculating the hash of a string.
 */

static NSString *SHAHash(NSString *src)
{
	SCSHA256Context ctx;
	const char *utf = [src UTF8String];

	ctx.Start();
	ctx.Update(strlen(utf), (const uint8_t *)utf);

	uint8_t output[32];
	ctx.Finish(output);

	// Convert to string
	char buffer[80];
	for (int i = 0; i < 32; ++i) {
		sprintf(buffer + i*2,"%02x",output[i]);
	}
	return [NSString stringWithUTF8String:buffer];
}

@implementation SCNetworkCredentials

- (void)setPasswordFromClearText:(NSString *)plainText
{
	NSString *p = [NSString stringWithFormat:@"%@%@",plainText,@"PwdSalt134"];
	self.password = SHAHash(p);
}

- (NSString *)hashPasswordWithToken:(NSString *)token
{
	NSString *p = [NSString stringWithFormat:@"%@PEnSalt194%@",self.password,token];
	NSString *ret = SHAHash(p);
	return ret;
}

@end

