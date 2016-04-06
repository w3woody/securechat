//
//  SCNetworkResponse.m
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

#import "SCNetworkResponse.h"

@implementation SCNetworkResponse

/*
 *	NOTE: The design of our server protocol is that we return 200 for all
 *	requests that went through successfully. If the command failed, we
 *	continue to return a 200 (success), but then return a JSON object
 *	which contains the error result. 
 *
 *	Our design presumes that we are executing a command, and if the command
 *	ran successfully, we return 200--even if the command itself had a problem.
 *	The HTTP result only has to do with if the request went through; the
 *	JSON payload then gives the actual result of the command.
 *
 *	Compare and contrast to many REST protocols where the HTTP result is
 *	also used to indicate if there was a problem with the command result.
 *	So if the user is not authenticated we send a 200, but with a JSON
 *	payload that indicates the user is not authenticated. The 200 means
 *	"command received"; the payload indicates the result. (Other protocols
 *	may return a 403: forbidden, or a 401: unauthorized. I'm not a fan of
 *	this because the status codes may be interpreted by a proxy in ways
 *	which are not desirable.)
 *
 *	Combined with the fact that all my requests are sent via a POST, and
 *	we've effectively told proxy servers to ignore what we're doing.
 */

- (BOOL)isServerError
{
	return (self.serverCode != 200);
}

- (BOOL)isAuthenticationError
{
	return (!self.success && (self.error == 4));
}


@end
