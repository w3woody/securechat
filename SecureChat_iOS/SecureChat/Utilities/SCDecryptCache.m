//
//  SCDecryptCache.m
//  SecureChat
//
//  Created by William Woody on 3/20/16.
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

#import "SCDecryptCache.h"
#import "SCRSAManager.h"
#import "SCMessageObject.h"

@interface SCDecryptCache ()
@property (strong) NSCache<NSNumber *, SCMessageObject *> *cache;
@end

@implementation SCDecryptCache

+ (SCDecryptCache *)shared
{
	static SCDecryptCache *cache;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		cache = [[SCDecryptCache alloc] init];
	});
	return cache;
}

- (id)init
{
	if (nil != (self = [super init])) {
		self.cache = [[NSCache alloc] init];
	}
	return self;
}

- (SCMessageObject *)decrypt:(NSData *)data atIndex:(NSInteger)index withCallback:(void (^)(NSInteger ident, SCMessageObject *msg))callback;
{
	NSNumber *n = @( index );
	SCMessageObject *ret = [self.cache objectForKey:n];
	if (ret) {
		return ret;
	} else if (callback) {
		void (^copyCallback)(NSInteger ident, SCMessageObject *msg) = [callback copy];

		// async decode
		dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
			NSData *decrypt = [[SCRSAManager shared] decodeData:data];
			SCMessageObject *obj = [[SCMessageObject alloc] initWithData:decrypt];
			dispatch_async(dispatch_get_main_queue(), ^{
				[self.cache setObject:obj forKey:n];
				copyCallback(index,obj);
			});
		});
	}
		
	return nil;
}


@end
