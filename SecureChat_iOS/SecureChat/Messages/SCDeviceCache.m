//
//  SCDeviceCache.m
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

#import <UIKit/UIKit.h>

#import "SCDeviceCache.h"
#import "SCDevice.h"
#import "SCNetwork.h"
#import "SCNetworkResponse.h"
#import "SCDeviceCacheEntry.h"

@interface SCDeviceCache ()
@property (strong) NSMutableDictionary<NSString *, SCDeviceCacheEntry *> *store;
@end

@implementation SCDeviceCache

+ (SCDeviceCache *)shared
{
	static SCDeviceCache *instance;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		instance = [[SCDeviceCache alloc] init];
	});
	return instance;
}

- (id)init
{
	if (nil != (self = [super init])) {

	}
	return self;
}

- (void)devicesFor:(NSString *)sender
		withCallback:(void (^)(NSInteger userID, NSArray<SCDevice *> *array))callback
{
	/*
	 *	See if it is cached
	 */

	NSTimeInterval t = CACurrentMediaTime();
	SCDeviceCacheEntry *e = self.store[sender];
	if (e && (e.expires > t)) {
		callback(e.userid,e.devices);
	}

	/*
	 *	If not, ask the back end
	 */

	void (^copyCallback)(NSInteger userid, NSArray<SCDevice *> *array) = [callback copy];
	NSDictionary *d = @{ @"username": sender };
	[[SCNetwork shared] request:@"device/devices" withParameters:d backgroundRequest:YES caller:self response:^(SCNetworkResponse *response) {
		if (response.success) {
			NSMutableArray<SCDevice *> *devices = [[NSMutableArray alloc] init];

			NSArray<NSDictionary *> *data = response.data[@"devices"];
			for (NSDictionary *d in data) {
				NSString *deviceid = d[@"deviceid"];
				NSString *publickey = d[@"publickey"];

				SCDevice *dev = [[SCDevice alloc] init];
				dev.deviceid = deviceid;
				dev.pubkeytext = publickey;
				dev.publickey = [[SCRSAEncoder alloc] initWithEncoderKey:publickey];

				[devices addObject:dev];
			}

			SCDeviceCacheEntry *entry = [[SCDeviceCacheEntry alloc] init];
			entry.expires = CACurrentMediaTime() + 300;	// 5 minute expiry
			entry.devices = devices;
			entry.userid = [response.data[@"userid"] integerValue];

			self.store[sender] = entry;

			copyCallback(entry.userid,devices);
		} else {
			copyCallback(0,nil);
		}
	}];
}

@end
