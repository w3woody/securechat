//
//  SCDateUtils.m
//  SecureChat
//
//  Created by William Woody on 3/13/16.
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

#import "SCDateUtils.h"

/*
 *	Parse server date string. Date format string is always of form
 *
 *		yyyy-MM-dd'T'HH:mm:ss
 *
 *	and represents UTC time.
 */

NSDate *SCParseServerDate(NSString *str)
{
	static NSDateFormatter *formatter;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		formatter = [[NSDateFormatter alloc] init];
		[formatter setDateFormat:@"yyyy-MM-dd'T'HH:mm:ss"];
		[formatter setTimeZone:[NSTimeZone timeZoneForSecondsFromGMT:0]];
	});
	return [formatter dateFromString:str];
}

/*
 *	Format a date/time string for presentation. This gives a date/time
 *	formatted for short time presentation
 */

NSString *SCFormatDisplayTime(NSDate *date)
{
	static NSDateFormatter *formatter;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		formatter = [[NSDateFormatter alloc] init];
		[formatter setDateStyle:NSDateFormatterShortStyle];
		[formatter setTimeStyle:NSDateFormatterShortStyle];
	});
	return [formatter stringFromDate:date];
}
