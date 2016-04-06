//
//  SCKeyboardCommonViewController.m
//  SecureChat
//
//  Created by William Woody on 2/29/16.
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

#import "SCKeyboardCommonViewController.h"

/*
 *	Common interface for onboarding view controllers that use a keyboard.
 *	This assumes the onboarding screen has a scroll area which is resized
 *	as the keyboard is made visible on the screen
 */

@interface SCKeyboardCommonViewController ()
@property (weak, nonatomic) IBOutlet NSLayoutConstraint *heightConstraint;
@property (weak, nonatomic) IBOutlet NSLayoutConstraint *bottomBorderConstraint;
@property (weak, nonatomic) IBOutlet UIScrollView *scroll;
@end

@implementation SCKeyboardCommonViewController

- (void)viewDidLoad
{
	[super viewDidLoad];

	// Do any additional setup after loading the view.
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(keyboardShowHide:) name:UIKeyboardWillShowNotification object:nil];
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(keyboardShowHide:) name:UIKeyboardWillHideNotification object:nil];
}

- (void)dealloc
{
	[[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)viewDidLayoutSubviews
{
	[super viewDidLayoutSubviews];

	CGRect r = self.scroll.bounds;

	CGFloat height = r.origin.y + r.size.height;
	height += self.bottomBorderConstraint.constant;
	self.heightConstraint.constant = height;
}

- (void)keyboardShowHide:(NSNotification *)n
{
	CGRect krect;

	/* Extract the size of the keyboard when the animation stops */
	krect = [n.userInfo[UIKeyboardFrameEndUserInfoKey] CGRectValue];

	/* Convert that to the rectangle in our primary view. Note the raw
	 * keyboard size from above is in the window's frame, which could be
	 * turned on its side.
	 */
	krect = [self.view convertRect:krect fromView:nil];

	/* Get the animation duration, and animation curve */
	NSTimeInterval duration = [[n.userInfo objectForKey:UIKeyboardAnimationDurationUserInfoKey] doubleValue];
	UIViewAnimationCurve curve = [[n.userInfo objectForKey:UIKeyboardAnimationCurveUserInfoKey] intValue];

	/* Calculate the bottom border height */
	CGFloat height = [n.name isEqualToString:UIKeyboardWillHideNotification] ? 0 : krect.size.height;
	self.bottomBorderConstraint.constant = height;

	/* Kick off the animation. What you do with the keyboard size is up to you */
	[UIView animateWithDuration:0
			delay:duration
			options:UIViewAnimationOptionBeginFromCurrentState | curve
			animations:^{
				/* Move things around */
				[self.view layoutIfNeeded];
			}
			completion:^(BOOL finished) {
				/* Finish up here */
			}];
}

@end
