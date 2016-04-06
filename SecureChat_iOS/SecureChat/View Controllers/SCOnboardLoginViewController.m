//
//  SCOnboardLoginViewController.m
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

#import "SCOnboardLoginViewController.h"
#import "SCTextField.h"
#import "SCOnboardForgotPasswordViewController.h"
#import "SCRSAManager.h"
#import "SCNetwork.h"
#import "SCNetworkCredentials.h"
#import "SCNetworkResponse.h"
#import "SCMessageQueue.h"

@interface SCOnboardLoginViewController ()
@property (weak, nonatomic) IBOutlet SCTextField *usernameField;
@property (weak, nonatomic) IBOutlet SCTextField *passwordField;

@end

@implementation SCOnboardLoginViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void)dealloc
{
	[[SCNetwork shared] cancelRequestsWithCaller:self];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)doForgotPassword:(id)sender
{
	[self performSegueWithIdentifier:@"ForgotPasswordPage" sender:self];
}

- (IBAction)doNext:(id)sender
{
	SCNetworkCredentials *creds = [[SCNetworkCredentials alloc] init];
	creds.username = self.usernameField.text;
	[creds setPasswordFromClearText:self.passwordField.text];

	[[SCNetwork shared] doLogin:creds withCallback:^(SCLoginError resp) {
		if (resp == LOGIN_SUCCESS) {
			/*
			 *	Login success.
			 */

			[[SCRSAManager shared] setUsername:creds.username passwordHash:creds.password];
			[[SCRSAManager shared] encodeSecureData];

			/*
			 *	Now send the add device request.
			 */

			NSDictionary *d = @{ @"deviceid": [[SCRSAManager shared] deviceUUID],
								 @"pubkey": [[SCRSAManager shared] publicKey] };
			[[SCNetwork shared] request:@"device/adddevice"
					withParameters:d
					caller:self
					response:^(SCNetworkResponse *response) {
				/*
				 *	Success: move on
				 */

				if (response.success) {

					/*
					 *	We have what we need to start the queue
					 */

					[[SCMessageQueue shared] startQueue];

					[self performSegueWithIdentifier:@"NextPage" sender:self];
				}
			}];

		} else if (resp == LOGIN_FAILURE) {
			/*
			 *	Login failure
			 */

			NSString *title = NSLocalizedString(@"Login Error", @"title");
			NSString *message = NSLocalizedString(@"The username or password provided are incorrect", @"message");

			UIAlertController *alert = [UIAlertController alertControllerWithTitle:title message:message preferredStyle:UIAlertControllerStyleAlert];
			UIAlertAction *action = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"OK") style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) {
			}];
			[alert addAction:action];
			[self presentViewController:alert animated:YES completion:nil];
		} else {
			/*
			 *	Back end server error. This will be handled elsewhere.
			 */

		}
	}];

}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
	if ([segue.identifier isEqualToString:@"ForgotPasswordPage"]) {
		SCOnboardForgotPasswordViewController *vc = segue.destinationViewController;
		vc.username = self.usernameField.text;
	}
}

@end
