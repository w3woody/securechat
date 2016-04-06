//
//  SCOnboardPasswordViewController.m
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

#import "SCOnboardPasswordViewController.h"
#import "SCRSAManager.h"
#import "SCKeychain.h"

@interface SCOnboardPasswordViewController ()
@property (weak, nonatomic) IBOutlet UITextField *password;
@end

@implementation SCOnboardPasswordViewController

- (void)viewDidLoad
{
	[super viewDidLoad];
}

- (IBAction)doNext:(id)sender
{
	NSString *pwd = self.password.text;
	if (pwd.length < 4) {
		UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Passcode too short", @"Error Title") message:@"The passcode you selected is too short. It needs to be at least 4 digits." preferredStyle:UIAlertControllerStyleAlert];
		UIAlertAction *defaultAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"Button") style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
		}];
		[alert addAction:defaultAction];
		[self presentViewController:alert animated:YES completion:nil];
		return;
	}

	/*
	 *	Force clear and save passcode
	 */

	[[SCRSAManager shared] clear];
	[[SCRSAManager shared] setPasscode:pwd];

	/*
	 *	Next page
	 */

	[self performSegueWithIdentifier:@"NextPage" sender:self];
}

@end
