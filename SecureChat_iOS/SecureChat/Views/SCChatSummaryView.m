//
//  SCChatSummaryView.m
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

#import "SCChatSummaryView.h"

@interface SCChatSummaryView ()
@property (assign) NSInteger count;
@end

@implementation SCChatSummaryView

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
	self.backgroundColor = [UIColor whiteColor];
	self.contentMode = UIViewContentModeRedraw;
}

- (void)setDeviceCount:(NSInteger)devCt
{
	self.count = devCt;
	[self setNeedsDisplay];
}

// From PaintCode
- (void)drawSummaryWithFrame: (CGRect)frame textLabel: (NSString*)textLabel
{
    //// General Declarations
    CGContextRef context = UIGraphicsGetCurrentContext();

    //// Color Declarations
    UIColor* senderColor = [UIColor colorWithRed: 0.891 green: 0.891 blue: 0.891 alpha: 1];

    //// Rectangle Drawing
    UIBezierPath* rectanglePath = [UIBezierPath bezierPathWithRect: CGRectMake(CGRectGetMinX(frame), CGRectGetMinY(frame) + CGRectGetHeight(frame) - 1, CGRectGetWidth(frame), 1)];
    [senderColor setFill];
    [rectanglePath fill];


    //// Text Drawing
    CGRect textRect = CGRectMake(CGRectGetMinX(frame), CGRectGetMinY(frame), CGRectGetWidth(frame), CGRectGetHeight(frame) - 1);
    NSMutableParagraphStyle* textStyle = NSMutableParagraphStyle.defaultParagraphStyle.mutableCopy;
    textStyle.alignment = NSTextAlignmentCenter;

    NSDictionary* textFontAttributes = @{NSFontAttributeName: [UIFont systemFontOfSize: UIFont.systemFontSize], NSForegroundColorAttributeName: UIColor.grayColor, NSParagraphStyleAttributeName: textStyle};

    CGFloat textTextHeight = [textLabel boundingRectWithSize: CGSizeMake(textRect.size.width, INFINITY)  options: NSStringDrawingUsesLineFragmentOrigin attributes: textFontAttributes context: nil].size.height;
    CGContextSaveGState(context);
    CGContextClipToRect(context, textRect);
    [textLabel drawInRect: CGRectMake(CGRectGetMinX(textRect), CGRectGetMinY(textRect) + (CGRectGetHeight(textRect) - textTextHeight) / 2, CGRectGetWidth(textRect), textTextHeight) withAttributes: textFontAttributes];
    CGContextRestoreGState(context);
}

- (void)drawRect:(CGRect)rect
{
	NSString *str;
	if (self.isSelf) {
		if (self.count == 1) {
			str = NSLocalizedString(@"You have %d device.", @"single");
		} else {
			str = NSLocalizedString(@"You have %d devices.", @"single");
		}
	} else {
		if (self.count == 1) {
			str = NSLocalizedString(@"User has %d device.", @"single");
		} else {
			str = NSLocalizedString(@"User has %d devices.", @"single");
		}
	}

	str = [NSString stringWithFormat:str, (int)self.count];
	[self drawSummaryWithFrame:self.bounds textLabel:str];
}

@end
