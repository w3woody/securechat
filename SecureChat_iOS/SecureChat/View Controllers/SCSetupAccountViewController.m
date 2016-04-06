//
//  SCSetupAccountViewController.m
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

#import "SCSetupAccountViewController.h"
#import "SCKeychain.h"
#import "SCMessageQueue.h"
#import "SCRSAManager.h"

@interface SCSetupAccountViewController ()

@end

@implementation SCSetupAccountViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)newAccount:(id)sender
{
	UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Are you sure?", @"title") message:NSLocalizedString(@"This will erase the old RSA key, reset the username on this device, and cause all stored messages to be erased.",@"message") preferredStyle:UIAlertControllerStyleAlert];

	UIAlertAction *action = [UIAlertAction actionWithTitle:NSLocalizedString(@"New Device", @"verb") style:UIAlertActionStyleDestructive handler:^(UIAlertAction * action) {

		/*
		 *	Stop the network interaction between the message queue and the
		 *	back end.
		 */

		[[SCMessageQueue shared] stopQueue];

		/*
		 *	We arrived here after the login screen, so notify the callers the
		 *	network requests have failed. Any residiual calls into the message
		 *	queue will terminate with failure here.
		 */

		if (self.callback) self.callback(NO);

		/*
		 *	We clear the secure store and open the setup onboarding pathway
		 */

		[[SCMessageQueue shared] clearQueue];
		[[SCRSAManager shared] clear];

		UIViewController *parent = self.presentingViewController;
		[self.navigationController dismissViewControllerAnimated:YES completion:^{
			UIStoryboard *onboarding = [UIStoryboard storyboardWithName:@"Onboarding" bundle:nil];
			UIViewController *root = onboarding.instantiateInitialViewController;
			[parent presentViewController:root animated:YES completion:nil];
		}];
	}];
	[alert addAction:action];

	action = [UIAlertAction actionWithTitle:NSLocalizedString(@"Cancel", @"verb") style:UIAlertActionStyleCancel handler:^(UIAlertAction * action) {
	}];
	[alert addAction:action];
	[self presentViewController:alert animated:YES completion:nil];
}

@end
