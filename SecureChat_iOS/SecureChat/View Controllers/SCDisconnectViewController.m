//
//  SCDisconnectViewController.m
//  SecureChat
//
//  Created by William Woody on 3/10/16.
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

#import "SCDisconnectViewController.h"
#import "SCKeychain.h"
#import "SCMessageQueue.h"
#import "SCRSAManager.h"
#import "SCNetwork.h"
#import "SCNetworkResponse.h"

@interface SCDisconnectViewController ()

@end

@implementation SCDisconnectViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)doDisconnect:(id)sender
{
	UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Are you sure?", @"title") message:NSLocalizedString(@"This will erase the old RSA key, reset the username on this device, and cause all stored messages to be erased.",@"message") preferredStyle:UIAlertControllerStyleAlert];

	UIAlertAction *action = [UIAlertAction actionWithTitle:NSLocalizedString(@"Erase", @"verb") style:UIAlertActionStyleDestructive handler:^(UIAlertAction * action) {

		/*
		 *	Unlike in the login screen or in the passcode screen where we
		 *	have the opportunity to set up a new account but the old
		 *	credentials are unavailable, in this case we have the
		 *	credentials so we can wipe this out
		 */

		NSString *deviceid = [[SCRSAManager shared] deviceUUID];
		NSDictionary *d = @{ @"deviceid": deviceid };
		[[SCNetwork shared] request:@"device/removedevice" withParameters:d caller:self response:^(SCNetworkResponse *response) {
			if (response.success) {
				/*
				 *	Once the API succeeds, clear the secure store and run the
				 *	next page.
				 */

				[[SCMessageQueue shared] stopQueue];
				[[SCMessageQueue shared] clearQueue];
				[[SCRSAManager shared] clear];

				[self performSegueWithIdentifier:@"NextPage" sender:self];
			}
		}];
	}];
	[alert addAction:action];

	action = [UIAlertAction actionWithTitle:NSLocalizedString(@"Cancel", @"verb") style:UIAlertActionStyleCancel handler:^(UIAlertAction * action) {
	}];
	[alert addAction:action];
	[self presentViewController:alert animated:YES completion:nil];
}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
	/*
	 *	We cannot go back; our operation was destructive.
	 */

	if ([segue.identifier isEqualToString:@"NextPage"]) {
		UIViewController *c = segue.destinationViewController;
		[c.navigationItem setHidesBackButton:YES];
	}
}

@end
