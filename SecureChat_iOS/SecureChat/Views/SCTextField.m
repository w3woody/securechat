//
//  SCTextField.m
//  SecureChat
//
//  Created by William Woody on 2/28/16.
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

#import "SCTextField.h"

@implementation SCTextField

- (id)initWithCoder:(NSCoder *)aDecoder
{
	if (nil != (self = [super initWithCoder:aDecoder])) {
		[self internalInit];
	}
	return self;
}

- (id)initWithFrame:(CGRect)frame
{
	if (nil != (self = [super initWithFrame:frame])) {
		[self internalInit];
	}
	return self;
}

- (void)internalInit
{
	[self.layer setCornerRadius:4.0];
	[self.layer setBorderWidth:1];
	[self.layer setBorderColor:[UIColor lightGrayColor].CGColor];
}

- (CGRect)textRectForBounds:(CGRect)bounds
{
	return CGRectInset(bounds, 10, 0);		// left, right border
}

- (CGRect)placeholderRectForBounds:(CGRect)bounds
{
	return CGRectInset(bounds, 10, 0);		// left, right border
}

- (CGRect)editingRectForBounds:(CGRect)bounds
{
	return CGRectInset(bounds, 10, 0);		// left, right border
}

@end
