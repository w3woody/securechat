//
//  SCPasscodeViewController.m
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

#import "SCPasscodeViewController.h"
#import "SCTextField.h"
#import "SCRSAManager.h"
#import "SCKeychain.h"
#import "SCNetwork.h"
#import "SCMessageQueue.h"

@interface SCPasscodeViewController ()
@property (weak, nonatomic) IBOutlet SCTextField *passcodeField;

@end

@implementation SCPasscodeViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

/*
 *	Determine if the passcode works. We deliberately use a weak passcode
 *	validation system by using an 8-bit checksum. This means there is a
 *	small chance this will succeed with the wrong passcode. This is a
 *	deliberate design decision.
 */

- (IBAction)doLogin:(id)sender
{
	if ([[SCRSAManager shared] setPasscode:self.passcodeField.text]) {
		/*
		 *	At this point we have a valid passcode, which means we've just
		 *	loaded the contents. Set up networking for network requests
		 */

		NSString *server = [[SCRSAManager shared] server];
		[[SCNetwork shared] setServerPrefix:server];

		/*
		 *	We have a server and credentials, start up the queue
		 */

		[[SCMessageQueue shared] startQueue];

		/*
		 *	And dismiss
		 */

		[self dismissViewControllerAnimated:YES completion:nil];

	} else {
		UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Incorrect passcode", @"title") message:NSLocalizedString(@"The passcode you entered is incorrect.",@"message") preferredStyle:UIAlertControllerStyleAlert];
		UIAlertAction *action = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"verb") style:UIAlertActionStyleDefault handler:^(UIAlertAction * action) {
		}];
		[alert addAction:action];
		[self presentViewController:alert animated:YES completion:nil];
	}
}

- (IBAction)newDevice:(id)sender
{
	UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Are you sure?", @"title") message:NSLocalizedString(@"This will erase the old RSA key, reset the username on this device, and cause all stored messages to be erased.",@"message") preferredStyle:UIAlertControllerStyleAlert];

	UIAlertAction *action = [UIAlertAction actionWithTitle:NSLocalizedString(@"New Device", @"verb") style:UIAlertActionStyleDestructive handler:^(UIAlertAction * action) {
		/*
		 *	We clear the secure store and open the setup onboarding pathway
		 */

		[[SCMessageQueue shared] stopQueue];
		[[SCMessageQueue shared] clearQueue];
		[[SCRSAManager shared] clear];

		UIViewController *parent = self.presentingViewController;
		[self dismissViewControllerAnimated:YES completion:^{
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

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
