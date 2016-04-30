//
//  UIImage+SCResizeImage.h
//  SecureChat
//
//  Created by William Woody on 4/28/16.
//  Copyright © 2016 William Edward Woody. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface UIImage (SCResizeImage)

- (UIImage *)resizeToFit:(CGFloat)maxDimension;
- (UIImage *)resizeToSize:(CGSize)maxDimension;
- (CGSize)sizeInSize:(CGSize)maxDimension;
- (CGSize)sizeForWidth:(CGFloat)width;

@end
