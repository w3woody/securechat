//
//  SCNetwork.m
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

#import "SCNetwork.h"
#import "SCNetworkRequest.h"
#import "SCNetworkResponse.h"
#import "SCNetworkCredentials.h"
#import "SCNetworkCookies.h"


@interface SCNetwork ()
// network session
@property (strong) NSURLSession *session;
// Cookie storage
@property (strong) SCNetworkCookies *cookies;
// current list of requests that are in progress
@property (strong) NSMutableArray<SCNetworkRequest *> *callQueue;
// current list of requests waiting to retry
@property (strong) NSMutableArray<SCNetworkRequest *> *retryQueue;
// Server prefix
@property (copy) NSString *server;
// In login process
@property (assign) BOOL inLogin;
// Network error
@property (assign) BOOL networkError;

@end

@implementation SCNetwork

/************************************************************************/
/*																		*/
/*	Startup/Shutdown													*/
/*																		*/
/************************************************************************/

+ (SCNetwork *)shared
{
	static SCNetwork *instance;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		instance = [[SCNetwork alloc] init];
	});
	return instance;
}

- (id)init
{
	if (nil != (self = [super init])) {
		NSURLSessionConfiguration *config = [NSURLSessionConfiguration ephemeralSessionConfiguration];
		self.cookies = [[SCNetworkCookies alloc] init];
		self.session = [NSURLSession sessionWithConfiguration:config];
		self.callQueue = [[NSMutableArray alloc] init];
		self.retryQueue = [[NSMutableArray alloc] init];
	}
	return self;
}


/************************************************************************/
/*																		*/
/*	Login Requests														*/
/*																		*/
/************************************************************************/

/*
 *	Perform the network request to actually perform the login process
 */

- (void)doLogin:(SCNetworkCredentials *)creds withCallback:(void (^)(SCLoginError err))callback
{
	void (^copyCallback)(SCLoginError success) = [callback copy];

	[self request:@"login/token" withParameters:nil caller:self response:^(SCNetworkResponse *response) {
		if (response.success) {
			NSString *token = response.data[@"token"];

			NSDictionary *d = @{ @"username": creds.username,
								 @"password": [creds hashPasswordWithToken:token] };

			[self request:@"login/login" withParameters:d caller:self response:^(SCNetworkResponse *response) {
				if (response.success) {
					copyCallback(LOGIN_SUCCESS);
				} else if (response.error == 2) {
					copyCallback(LOGIN_FAILURE);
				} else {
					// server error; also display error
					[self showError:response];
					copyCallback(LOGIN_SERVERERROR);
				}
			}];
		} else {
			// server error; also display error
			[self showError:response];
			copyCallback(LOGIN_SERVERERROR);
		}
	}];
}

/*
 *	We failed to login. Cancel
 */

- (void)failedLogin
{
	self.inLogin = NO;

	for (SCNetworkRequest *req in self.retryQueue) {
		req.callback(req.lastError);
	}
	[self.retryQueue removeAllObjects];
}

/*
 *	Succeeded logging in; retry all the requests that need to be retried
 */

- (void)successfulLogin
{
	self.inLogin = NO;

	NSArray *tmp = [self.retryQueue copy];
	[self.retryQueue removeAllObjects];

	for (SCNetworkRequest *req in tmp) {
		[self sendRequest:req];
	}
}

/*
 *	Login request
 */

- (void)loginRequest
{
	if (self.inLogin) return;
	self.inLogin = YES;

	/*
	 *	Step 1: perform the login request if we have credentials
	 */

	SCNetworkCredentials *creds = [self.delegate credentials];
	if (creds) {
		[self doLogin:creds withCallback:^(SCLoginError success) {
			if (success == LOGIN_SUCCESS) {
				[self successfulLogin];
			} else if (success == LOGIN_FAILURE) {
				[self runLoginDialog];
			} else {
				[self failedLogin];
			}
		}];
	} else {
		[self runLoginDialog];
	}
}

/*
 *	Run the login dialog. The login dialog will handle the login process
 *	itself and return credentials, or fail and return nil for the credentials
 */

- (void)runLoginDialog
{
	[self.delegate requestLoginDialog:^(BOOL success) {
		if (success) {
			/*
			 *	We've successfully logged in. Assume the dialog has done
			 *	the heavy lifting of saving the new credentials
			 */

			[self successfulLogin];

		} else {
			/*
			 *	User canceled the login operation. Fail by canceling all
			 *	of the requests, and send cleared credentials
			 */

			[self failedLogin];
		}
	}];
}

/************************************************************************/
/*																		*/
/*	Request																*/
/*																		*/
/************************************************************************/

/*
 *	Iterate through all queued calls forcing them to be canceled
 */

- (void)cancelRequestsWithCaller:(id<NSObject>)caller
{
	NSMutableArray<SCNetworkRequest *> *remove = [[NSMutableArray alloc] init];
	for (SCNetworkRequest *req in self.callQueue) {
		if (req.caller == caller) {
			if (req.task) {
				[req.task cancel];
			}
			[remove addObject:req];

			if ([self.delegate respondsToSelector:@selector(stopWaitSpinner)] && req.waitFlag) {
				req.waitFlag = NO;
				[self.delegate stopWaitSpinner];
			}
		}
	}
	[self.callQueue removeObjectsInArray:remove];

	remove = [[NSMutableArray alloc] init];
	for (SCNetworkRequest *req in self.retryQueue) {
		if (req.caller == caller) {
			if (req.task) {
				[req.task cancel];
			}
			[remove addObject:req];

			if ([self.delegate respondsToSelector:@selector(stopWaitSpinner)] && req.waitFlag) {
				req.waitFlag = NO;
				[self.delegate stopWaitSpinner];
			}
		}
	}
	[self.retryQueue removeObjectsInArray:remove];
}

/*
 *	Set the server prefix
 */

- (void)setServerPrefix:(NSString *)prefix
{
	if ([prefix length] > 0) {
		if ([prefix characterAtIndex:prefix.length - 1] == '/') {
			prefix = [prefix substringToIndex:prefix.length - 1];
		}

		NSRange r = [prefix rangeOfString:@"://"];
		if (r.location == NSNotFound) {
			prefix = [NSString stringWithFormat:@"https://%@",prefix];
		}

		self.server = [NSString stringWithFormat:@"%@/ss/api/1",prefix];
	}
}

/*
 *	Convert network request to a request URL
 */

- (NSURLRequest *)requestWith:(SCNetworkRequest *)req
{
	NSString *path = [NSString stringWithFormat:@"%@/%@",self.server,req.requestUri];
	NSURL *url = [NSURL URLWithString:path];
	NSMutableURLRequest *ret = [[NSMutableURLRequest alloc] initWithURL:url];

	[ret setHTTPMethod:@"POST"];
	if (req.params) {
		NSError *error;
		NSData *data = [NSJSONSerialization dataWithJSONObject:req.params options:NSJSONWritingPrettyPrinted error:&error];
		[ret setHTTPBody:data];
	}

	NSString *cookie = [self.cookies sendCookieValue];
	if (cookie) {
		[ret setValue:cookie forHTTPHeaderField:@"Cookie"];
	}

	return ret;
}

/*
 *	Internal process request. This enqueues the request and parses the
 *	response.
 */

- (void)sendRequest:(SCNetworkRequest *)request
{
	[self.callQueue addObject:request];

	/*
	 *	Enqueue the request
	 */

	if (!request.backgroundFlag) {
		/* Wait spinner only if we are not a background request */
		if ([self.delegate respondsToSelector:@selector(startWaitSpinner)]) {
			request.waitFlag = YES;
			[self.delegate startWaitSpinner];
		}
	}

	NSURLRequest *urlRequest = [self requestWith:request];
	NSURLSessionDataTask *task = [self.session dataTaskWithRequest:urlRequest completionHandler:^(NSData *data, NSURLResponse *response, NSError * error) {

		/*
		 *	Get the response code from the server and parse the response if
		 *	one was returned
		 */

		NSHTTPURLResponse *urlResponse = (NSHTTPURLResponse *)response;
		NSInteger serverCode = urlResponse.statusCode;

		[self.cookies processReceivedHeader:urlResponse.allHeaderFields];

		NSDictionary *d;
		if (data == nil) {
			d = nil;
		} else {
			d = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
		}
		request.task = nil;

		/*
		 *	Parse the standard response.
		 */

		SCNetworkResponse *respData = [[SCNetworkResponse alloc] init];
		respData.serverCode = serverCode;
		respData.success = [d[@"success"] boolValue];
		respData.error = [d[@"error"] integerValue];
		respData.errorMessage = (NSString *)(d[@"message"]);
		respData.exceptionStack = (NSArray *)(d[@"exception"]);
		respData.data = (NSDictionary *)(d[@"data"]);

		dispatch_async(dispatch_get_main_queue(), ^{
			if ([self.delegate respondsToSelector:@selector(stopWaitSpinner)] && request.waitFlag) {
				request.waitFlag = NO;
				[self.delegate stopWaitSpinner];
			}

			[self.callQueue removeObject:request];
			[self handleResponse:respData withRequest:request];
		});

	}];
	request.task = task;
	[task resume];
}

/*
 *	Handle errors
 */

- (void)showError:(SCNetworkResponse *)response
{
	// If this is a network error, present this only once.
	if ([response isServerError]) {
		if (self.networkError) return;
		self.networkError = NO;
	}

	if ([self.delegate respondsToSelector:@selector(showServerError:)]) {
		[self.delegate showServerError:response];
	}
}

/*
 *	Handle the request
 */

- (void)handleResponse:(SCNetworkResponse *)response withRequest:(SCNetworkRequest *)request
{
//#if DEBUG
//	NSLog(@"------");
//	NSLog(@"Request: %@",request.requestUri);
//	NSLog(@"         %@",request.params);
//	NSLog(@"Response: %d: %@",(int)response.serverCode,response.errorMessage);
//	NSLog(@"         %@",response.data);
//#endif

	/*
	 *	Test authentication; if not authenticated try logging in.
	 */

	if ([response isAuthenticationError] && self.delegate) {
		/*
		 *	If authentication error, queue for retry and try to login
		 */

		[self.retryQueue addObject:request];
		request.lastError = response;
		[self loginRequest];
		return;						// We're not done yet.
	}

	/*
	 *	Process error notifications
	 */
	
	if (!request.skipErrors) {
		/*
		 *	Handle errors via delegate. If we are skipping error processing
		 *	we simply pass the results up.
		 */

		if ([response isServerError]) {
			/*
			 *	If server error, display
			 */

			[self showError:response];

		} else if (!response.success) {
			/*
			 *	If there was a random error display the error, unless it was a
			 *	login error.
			 */

			if (response.error != 2) {
				[self showError:response];
			}
		}
	}

	/*
	 *	Reset our network error flag if a request succeeded. We do this
	 *	for our network error display.
	 */

	if (![response isServerError]) {
		self.networkError = NO;
	}

	request.callback(response);
}

/*
 *	Enqueue request
 */

- (void)request:(NSString *)requestUri
		withParameters:(NSDictionary *)params
		caller:(id<NSObject>)caller
		response:(void (^)(SCNetworkResponse *response))callback;
{
	[self request:requestUri withParameters:params backgroundRequest:NO skipErrorHandler:NO caller:caller response:callback];
}

- (void)request:(NSString *)requestUri
		withParameters:(NSDictionary *)params
		backgroundRequest:(BOOL)backgroundFlag
		caller:(id<NSObject>)caller
		response:(void (^)(SCNetworkResponse *response))callback;
{
	[self request:requestUri withParameters:params backgroundRequest:backgroundFlag skipErrorHandler:NO caller:caller response:callback];
}

- (void)request:(NSString *)requestUri
		withParameters:(NSDictionary *)params
		skipErrorHandler:(BOOL)skipErrors
		caller:(id<NSObject>)caller
		response:(void (^)(SCNetworkResponse *response))callback;
{
	[self request:requestUri withParameters:params backgroundRequest:NO skipErrorHandler:skipErrors caller:caller response:callback];
}

- (void)request:(NSString *)requestUri
		withParameters:(NSDictionary *)params
		backgroundRequest:(BOOL)backgroundFlag
		skipErrorHandler:(BOOL)skipErrors
		caller:(id<NSObject>)caller
		response:(void (^)(SCNetworkResponse *response))callback
{
	SCNetworkRequest *request = [[SCNetworkRequest alloc] init];

	request.requestUri = requestUri;
	request.params = params;
	request.backgroundFlag = backgroundFlag;
	request.skipErrors = skipErrors;
	request.caller = caller;
	request.enqueueTime = [NSDate timeIntervalSinceReferenceDate];
	request.callback = callback;

	[self sendRequest:request];
}

@end
