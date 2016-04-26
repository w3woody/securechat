//
//  SCBubbleView.m
//  SecureChat
//
//  Created by William Woody on 3/19/16.
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

#import "SCBubbleView.h"
#import "SCMessageObject.h"

@interface SCBubbleView ()
@end

@implementation SCBubbleView

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
	self.backgroundColor = [UIColor clearColor];
	self.contentMode = UIViewContentModeRedraw;
}

- (void)setMessage:(SCMessageObject *)msg
{
	self.msg = msg;
	[self setNeedsDisplay];
}

/*
 *	Generated in PaintCode
 */

- (void)drawBubbleWithFrame: (CGRect)frame leftFlag: (BOOL)leftFlag label: (NSString*)label
{
    //// Color Declarations
    UIColor* senderColor = [UIColor colorWithRed: 0.891 green: 0.891 blue: 0.891 alpha: 1];
    UIColor* receiverColor = [UIColor colorWithRed: 0.25 green: 0.375 blue: 0.5 alpha: 1];
    UIColor* senderTextColor = [UIColor colorWithRed: 0 green: 0 blue: 0 alpha: 1];
    UIColor* receiverTextColor = [UIColor colorWithRed: 1 green: 1 blue: 1 alpha: 1];

    //// Variable Declarations
    BOOL rightFlag = !leftFlag;
    UIColor* bkColor = leftFlag ? senderColor : receiverColor;
    UIColor* txtColor = leftFlag ? senderTextColor : receiverTextColor;

    //// Rectangle Drawing
    UIBezierPath* rectanglePath = [UIBezierPath bezierPathWithRoundedRect: CGRectMake(CGRectGetMinX(frame) + 4, CGRectGetMinY(frame), CGRectGetWidth(frame) - 8, CGRectGetHeight(frame)) cornerRadius: 12];
    [bkColor setFill];
    [rectanglePath fill];


    if (leftFlag)
    {
        //// Bezier Drawing
        UIBezierPath* bezierPath = [UIBezierPath bezierPath];
        [bezierPath moveToPoint: CGPointMake(CGRectGetMinX(frame) + 24, CGRectGetMaxY(frame))];
        [bezierPath addLineToPoint: CGPointMake(CGRectGetMinX(frame), CGRectGetMaxY(frame))];
        [bezierPath addLineToPoint: CGPointMake(CGRectGetMinX(frame) + 12, CGRectGetMaxY(frame) - 10)];
        [bezierPath addLineToPoint: CGPointMake(CGRectGetMinX(frame) + 24, CGRectGetMaxY(frame))];
        [bezierPath closePath];
        [bkColor setFill];
        [bezierPath fill];
    }


    if (rightFlag)
    {
        //// Bezier 2 Drawing
        UIBezierPath* bezier2Path = [UIBezierPath bezierPath];
        [bezier2Path moveToPoint: CGPointMake(CGRectGetMaxX(frame), CGRectGetMaxY(frame))];
        [bezier2Path addLineToPoint: CGPointMake(CGRectGetMaxX(frame) - 24, CGRectGetMaxY(frame))];
        [bezier2Path addLineToPoint: CGPointMake(CGRectGetMaxX(frame) - 12, CGRectGetMaxY(frame) - 10)];
        [bezier2Path addLineToPoint: CGPointMake(CGRectGetMaxX(frame), CGRectGetMaxY(frame))];
        [bezier2Path closePath];
        [bkColor setFill];
        [bezier2Path fill];
    }


    //// Text 2 Drawing
    CGRect text2Rect = CGRectMake(CGRectGetMinX(frame) + 11, CGRectGetMinY(frame) + 4, CGRectGetWidth(frame) - 22, CGRectGetHeight(frame) - 8);
    UIBezierPath* text2Path = [UIBezierPath bezierPathWithRect: text2Rect];
    [bkColor setFill];
    [text2Path fill];
    NSMutableParagraphStyle* text2Style = NSMutableParagraphStyle.defaultParagraphStyle.mutableCopy;
    text2Style.alignment = NSTextAlignmentLeft;

    NSDictionary* text2FontAttributes = @{NSFontAttributeName: [UIFont systemFontOfSize: UIFont.labelFontSize], NSForegroundColorAttributeName: txtColor, NSParagraphStyleAttributeName: text2Style};

    [label drawInRect: text2Rect withAttributes: text2FontAttributes];
}

+ (CGSize)sizeWithMessage:(SCMessageObject *)msg width:(CGFloat)width
{
	NSDictionary *d = @{ NSFontAttributeName: [UIFont systemFontOfSize: UIFont.labelFontSize] };

	// TODO: Handle different message types.
	NSString *text = [msg messageAsText];

	CGRect r = [text boundingRectWithSize:CGSizeMake(width - 22, 9999) options:NSStringDrawingUsesLineFragmentOrigin | NSStringDrawingTruncatesLastVisibleLine attributes:d context:nil];
	r.size.width += 22;
	if (r.size.width < 44) r.size.width = 44;
	return CGSizeMake(ceil(r.size.width+22), ceil(r.size.height+8));
}

- (void)drawRect:(CGRect)rect
{
	CGRect bounds = self.bounds;
	NSDictionary *d = @{ NSFontAttributeName: [UIFont systemFontOfSize: UIFont.labelFontSize] };

	// TODO: Handle different message types.
	NSString *text = [self.msg messageAsText];

	CGRect r = [text boundingRectWithSize:CGSizeMake(bounds.size.width - 22, 9999) options:NSStringDrawingUsesLineFragmentOrigin | NSStringDrawingTruncatesLastVisibleLine attributes:d context:nil];
	r.size.width += 22;
	r.size.height += 10;
	if (r.size.width < 44) r.size.width = 44;

	if (self.senderFlag) {
		bounds.origin.x += bounds.size.width - r.size.width;
	}
	bounds.size = r.size;

	[self drawBubbleWithFrame:bounds leftFlag:!self.senderFlag label:text];
}

@end
