//
//  UIImage+SCResizeImage.m
//  SecureChat
//
//  Created by William Woody on 4/28/16.
//  Copyright © 2016 William Edward Woody. All rights reserved.
//

#import "UIImage+SCResizeImage.h"

@implementation UIImage (SCResizeImage)

- (UIImage *)resizeToFit:(CGFloat)maxDimension
{
	CGSize size = self.size;

	if (size.width > maxDimension) {
		size.height *= maxDimension / size.width;
		size.width = maxDimension;
	}
	if (size.height > maxDimension) {
		size.width *= maxDimension / size.height;
		size.height = maxDimension;
	}

	UIGraphicsBeginImageContext(size);
	[self drawInRect:CGRectMake(0, 0, size.width, size.height)];
	UIImage *ret = UIGraphicsGetImageFromCurrentImageContext();
	UIGraphicsEndImageContext();

	return ret;
}

- (UIImage *)resizeToSize:(CGSize)maxDimension
{
	CGSize size = self.size;

	if (size.width > maxDimension.width) {
		size.height *= maxDimension.width / size.width;
		size.width = maxDimension.width;
	}
	if (size.height > maxDimension.height) {
		size.width *= maxDimension.height / size.height;
		size.height = maxDimension.height;
	}

	UIGraphicsBeginImageContext(size);
	[self drawInRect:CGRectMake(0, 0, size.width, size.height)];
	UIImage *ret = UIGraphicsGetImageFromCurrentImageContext();
	UIGraphicsEndImageContext();

	return ret;
}

- (CGSize)sizeInSize:(CGSize)maxDimension
{
	CGSize size = self.size;

	if (size.width > maxDimension.width) {
		size.height *= maxDimension.width / size.width;
		size.width = maxDimension.width;
	}
	if (size.height > maxDimension.height) {
		size.width *= maxDimension.height / size.height;
		size.height = maxDimension.height;
	}

	return size;
}

- (CGSize)sizeForWidth:(CGFloat)width
{
	CGSize size = self.size;

	if (size.width < width) return size;
	if ((size.width < 1) || (size.height < 1)) return CGSizeMake(1, 1);
	return CGSizeMake(width,size.height * width / size.width);
}

@end
