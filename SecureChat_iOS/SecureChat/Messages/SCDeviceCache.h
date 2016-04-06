//
//  SCDeviceCache.h
//  SecureChat
//
//  Created by William Woody on 3/15/16.
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

@class SCDevice;

/*
 *	This maintains a cache of sender devices that we are encrypting to
 *	send for. We refresh each entry every 2 minutes or so.
 */

@interface SCDeviceCache : NSObject

+ (SCDeviceCache *)shared;

- (void)devicesFor:(NSString *)sender
		withCallback:(void (^)(NSInteger userID, NSArray<SCDevice *> *array))callback;

@end
