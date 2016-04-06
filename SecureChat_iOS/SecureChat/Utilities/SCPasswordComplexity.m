//
//  SCPasswordComplexity.m
//  SecureChat
//
//  Created by William Woody on 3/9/16.
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

#import "SCPasswordComplexity.h"

/**
 *	Return true if the password is complex enough
 */

BOOL SCPasswordComplexityTest(NSString *str)
{
	if ([str length] < 8) return NO;		// insufficient length

	BOOL hasCap = NO;
	BOOL hasLow = NO;
	BOOL hasNum = NO;
	BOOL hasPct = NO;

	NSUInteger i,len = [str length];
	for (i = 0; i < len; ++i) {
		unichar ch = [str characterAtIndex:i];

		if ((ch >= '0') && (ch <= '9')) hasNum = YES;
		if ((ch >= 'A') && (ch <= 'Z')) hasCap = YES;
		if ((ch >= 'a') && (ch <= 'z')) hasLow = YES;

		if ((ch > ' ') && (ch < '0')) hasPct = YES;	// ASCII assumed
		if ((ch > '9') && (ch < 'A')) hasPct = YES;	// ASCII assumed
		if (ch == '~') hasPct = YES;
	}

	return hasCap && hasLow && hasNum && hasPct;
}

