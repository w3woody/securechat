//
//  SCNetworkCookies.m
//  SecureChat
//
//  Created by William Woody on 3/8/16.
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

#import "SCNetworkCookies.h"

@interface SCNetworkCookies ()
@property (strong) NSMutableDictionary<NSString *, NSString *> *cookies;
@end

/*
 *	iOS 8 ephemeral connections do not store cookies. So this handles a
 *	rather primitive cookie store for our system
 */

@implementation SCNetworkCookies

- (id)init
{
	if (nil != (self = [super init])) {
		self.cookies = [[NSMutableDictionary alloc] init];
	}
	return self;
}

- (NSString *)sendCookieValue
{
	if (self.cookies.count == 0) return nil;

	NSMutableString *str = [[NSMutableString alloc] init];
	[self.cookies enumerateKeysAndObjectsUsingBlock:^(NSString *key, NSString *obj, BOOL *stop) {
		if ([str length]) {
			[str appendString:@";"];
		}
		[str appendFormat:@"%@=%@",key,obj];
	}];
	return str;
}

- (void)processCookie:(NSString *)str
{
	str = [str stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];

	NSRange range = [str rangeOfString:@";"];	// peel off ';'
	if (range.location != NSNotFound) {
		str = [str substringToIndex:range.location];
		str = [str stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
	}

	/*
	 *	Now we should be left with "name=value"; store as pair.
	 */

	NSString *key;
	NSString *value;
	range = [str rangeOfString:@"="];
	if (range.location != NSNotFound) {
		key = [str substringToIndex:range.location];
		key = [key stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];

		range.location++;
		range.length = str.length - range.location;
		value = [str substringWithRange:range];
		value = [value stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
	} else {
		key = str;
		value = @"";
	}

	/*
	 *	Key/value store
	 */

	self.cookies[key] = value;
}

- (void)processReceivedHeader:(NSDictionary *)headers
{
	NSString *val = headers[@"Set-Cookie"];
	if (val == nil) return;

	/*
	 *	Split across ','
	 */

	for (;;) {
		NSRange range = [val rangeOfString:@","];
		if (range.location != NSNotFound) {
			NSString *front = [val substringToIndex:range.location];
			[self processCookie:front];

			range.location++;
			range.length = val.length - range.location;
			val = [val substringWithRange:range];
		} else {
			[self processCookie:val];
			break;
		}
	}
}

@end
