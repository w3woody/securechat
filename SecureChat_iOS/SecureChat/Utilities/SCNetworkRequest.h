//
//  SCNetworkRequest.h
//  SecureChat
//
//  Created by William Woody on 3/2/16.
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

@class SCNetworkResponse;

/*
 *	Internal class used by SCNetwork to track requests
 */

@interface SCNetworkRequest : NSObject

@property (copy) NSString *requestUri;
@property (strong) NSDictionary *params;
@property (assign) BOOL skipErrors;
@property (assign) BOOL backgroundFlag;
@property (weak) id<NSObject> caller;
@property (assign) NSTimeInterval enqueueTime;
@property (copy) void (^callback)(SCNetworkResponse *response);

@property (strong) NSURLSessionDataTask *task;

@property (strong) SCNetworkResponse *lastError;
@property (assign) BOOL waitFlag;

@end
