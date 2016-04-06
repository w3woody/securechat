//
//  SCWait.m
//  SecureChat
//
//  Created by William Woody on 3/7/16.
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

#import "SCWait.h"
#import "AppDelegate.h"

@interface SCWait ()
{
	NSInteger count;
	NSTimer *timer;
	BOOL visible;
}

@property (strong, nonatomic) IBOutlet UIView *view;
@property (strong, nonatomic) IBOutlet UIActivityIndicatorView *spinner;
@end

@implementation SCWait

+ (SCWait *)shared
{
	static SCWait *wait;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		wait = [[SCWait alloc] init];
	});
	return wait;
}

- (void)showWait
{
	if (visible) return;
	visible = YES;

	AppDelegate *del = (AppDelegate *)[[UIApplication sharedApplication] delegate];
	UIWindow *w = del.window;

	if (self.view == nil) {
		self.view = [[UIView alloc] initWithFrame:w.bounds];
		self.view.backgroundColor = [UIColor colorWithWhite:0 alpha:0.75];

		self.spinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhiteLarge];
		[self.view addSubview:self.spinner];

		CGSize size = self.spinner.frame.size;
		CGRect r = self.view.bounds;
		r.origin.x += floorf((r.size.width - size.width)/2);
		r.origin.y += floorf((r.size.height - size.height)/2);
		r.size = size;

		[self.spinner setFrame:r];
		[self.spinner startAnimating];

		UIGestureRecognizer *gesture = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(doCancel:)];
		[self.view addGestureRecognizer:gesture];
	}

	[w addSubview:self.view];
	[self.view setFrame:w.bounds];
	[self.view setAlpha:0];

	[UIView animateWithDuration:0.25 animations:^{
		[self.view setAlpha:1];
	} completion:nil];
}

- (void)hideWait
{
	if (!visible) return;
	visible = NO;

	if (self.view) {
		[UIView animateWithDuration:0.25 animations:^{
			[self.view setAlpha:0];
		} completion:^(BOOL flag) {
			[self.view removeFromSuperview];
			self.view = nil;
			self.spinner = nil;
		}];
	}
}

- (void)internalShow
{
	if (timer) timer = nil;
	[self showWait];
}

- (void)wait
{
	if (count++ == 0) {
		timer = [NSTimer scheduledTimerWithTimeInterval:0.25 target:self selector:@selector(internalShow) userInfo:nil repeats:NO];
	}
}

- (void)stopWait
{
	if (--count == 0) {
		if (timer) [timer invalidate];
		[self hideWait];
	}
}


- (IBAction)doCancel:(id)sender
{
	if (timer) [timer invalidate];
	[self hideWait];
}

@end
