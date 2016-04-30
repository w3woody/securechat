//
//  SCMessageObject.m
//  SecureChat
//
//  Created by William Woody on 4/26/16.
//  Copyright © 2016 William Edward Woody. All rights reserved.
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


#import "SCMessageObject.h"
#import "UIImage+SCResizeImage.h"

@interface SCMessageObject ()
@property (strong) UIImage *image;
@property (copy) NSString *message;
@end

@implementation SCMessageObject

- (id)initWithData:(NSData *)data
{
	if (nil != (self = [super init])) {
		NSUInteger len = [data length];
		uint8_t *bytes = (uint8_t *)[data bytes];

		self.image = nil;
		self.message = nil;

		if ((len > 2) && (bytes[0] == 0x01)) {
			/*
			 *	Escape
			 */

			NSData *d = [NSData dataWithBytesNoCopy:bytes+2 length:len-2 freeWhenDone:NO];
			if (bytes[1] == 0x00) {
				// 0x01 0x00: JPEG
				self.image = [UIImage imageWithData:d];

			} else if (bytes[1] == 0x01) {
				// 0x01 0x01: Message
				self.message = [[NSString alloc] initWithBytes:bytes length:len encoding:NSUTF8StringEncoding];

			}
		} else {
			self.message = [[NSString alloc] initWithBytes:bytes length:len encoding:NSUTF8StringEncoding];
		}
	}
	return self;
}

// Create a message to be sent
- (id)initWithString:(NSString *)msg
{
	if (nil != (self = [super init])) {
		self.message = msg;
	}
	return self;
}

// Create a message to be sent
- (id)initWithImage:(UIImage *)image
{
	if (nil != (self = [super init])) {
		self.image = image;
	}
	return self;
}


// Encode this message as a data blob
- (NSData *)dataFromMessage
{
	NSData *data;
	uint8_t b[2];

	if (self.message) {
		data = [self.message dataUsingEncoding:NSUTF8StringEncoding];
		if (data.length < 1) return data;

		[data getBytes:b range:NSMakeRange(0, 1)];
		if (b[0] != 1) return data;

		b[0] = 1;
		b[1] = 1;
	} else if (self.image) {
		data = UIImageJPEGRepresentation(self.image, 0.75);

		b[0] = 1;
		b[1] = 0;
	} else {
		return nil;
	}

	NSMutableData *ret = [[NSMutableData alloc] initWithCapacity:data.length + 2];

	[ret appendBytes:b length:2];
	[ret appendData:data];

	return ret;
}

// Summary view content
- (NSString *)summaryMessageText
{
	if (self.message) {
		return self.message;
	} else if (self.image) {
		return NSLocalizedString(@"(photo)", @"Photo filler");
	} else {
		return nil;
	}
}

// Bubble rendering management
- (CGSize)sizeForWidth:(CGFloat)width
{
	if (self.message) {
		NSDictionary *d = @{ NSFontAttributeName: [UIFont systemFontOfSize: UIFont.labelFontSize] };
		CGRect r = [self.message boundingRectWithSize:CGSizeMake(width, 9999) options:NSStringDrawingUsesLineFragmentOrigin | NSStringDrawingTruncatesLastVisibleLine attributes:d context:nil];
		return r.size;
	} else if (self.image) {
		return [self.image sizeForWidth:width];
	} else {
		// TODO: Size?
		return CGSizeMake(44, 22);
	}
}

- (void)drawWithRect:(CGRect)rect withTextColor:(NSString *)txtColor
{
	if (self.message) {
		NSDictionary *d = @{ NSFontAttributeName: [UIFont systemFontOfSize: UIFont.labelFontSize],
							 NSForegroundColorAttributeName: txtColor };

		[self.message drawInRect:rect withAttributes: d];
	} else if (self.image) {
		UIImage *i = [self.image resizeToSize:rect.size];
		CGSize size = i.size;

		rect.origin.x += (rect.size.width - size.width)/2;
		rect.origin.y += (rect.size.height - size.height)/2;
		rect.size = size;

		[i drawInRect:rect];
	}
}

@end
