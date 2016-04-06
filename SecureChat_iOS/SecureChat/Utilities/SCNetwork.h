//
//  SCNetwork.h
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
@class SCNetworkCredentials;

/************************************************************************/
/*																		*/
/*	Classes																*/
/*																		*/
/************************************************************************/

typedef enum {
	LOGIN_SUCCESS = 0,
	LOGIN_FAILURE = 1,
	LOGIN_SERVERERROR = 2
} SCLoginError;

/*	SCNetworkDelegate
 *
 *		Delegate which gives us the chance to handle things through a
 *	single interface
 */

@protocol SCNetworkDelegate <NSObject>

/*
 *	Wait spinner; called when we're inside a network request
 */

@optional
- (void)startWaitSpinner;
- (void)stopWaitSpinner;

- (void)showServerError:(SCNetworkResponse *)response;

@required
- (SCNetworkCredentials *)credentials;
- (void)requestLoginDialog:(void (^)(BOOL success))callback;

@end

/*	SCNetwork
 *
 *		Class wrappers code for handling network requests
 */

@interface SCNetwork : NSObject

@property (weak) id<SCNetworkDelegate> delegate;

+ (SCNetwork *)shared;

- (void)setServerPrefix:(NSString *)prefix;

- (void)doLogin:(SCNetworkCredentials *)creds
		withCallback:(void (^)(SCLoginError err))callback;

- (void)request:(NSString *)requestUri
		withParameters:(NSDictionary *)params
		caller:(id<NSObject>)caller
		response:(void (^)(SCNetworkResponse *response))callback;

- (void)request:(NSString *)requestUri
		withParameters:(NSDictionary *)params
		skipErrorHandler:(BOOL)skipErrors
		caller:(id<NSObject>)caller
		response:(void (^)(SCNetworkResponse *response))callback;

- (void)request:(NSString *)requestUri
		withParameters:(NSDictionary *)params
		backgroundRequest:(BOOL)backgroundFlag
		caller:(id<NSObject>)caller
		response:(void (^)(SCNetworkResponse *response))callback;

- (void)request:(NSString *)requestUri
		withParameters:(NSDictionary *)params
		backgroundRequest:(BOOL)backgroundFlag
		skipErrorHandler:(BOOL)skipErrors
		caller:(id<NSObject>)caller
		response:(void (^)(SCNetworkResponse *response))callback;

- (void)cancelRequestsWithCaller:(id<NSObject>)caller;

@end
