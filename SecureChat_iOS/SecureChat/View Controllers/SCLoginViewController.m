//
//  SCLoginViewController.m
//  SecureChat
//
//  Created by William Woody on 3/8/16.
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

#import "SCLoginViewController.h"
#import "SCTextField.h"
#import "SCNetwork.h"
#import "SCNetworkResponse.h"
#import "SCNetworkCredentials.h"
#import "SCRSAManager.h"
#import "SCSetupAccountViewController.h"

@interface SCLoginViewController ()
@property (weak, nonatomic) IBOutlet SCTextField *usernameField;
@property (weak, nonatomic) IBOutlet SCTextField *passwordField;

@end

@implementation SCLoginViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)doLogin:(id)sender
{
	SCNetworkCredentials *creds = [[SCNetworkCredentials alloc] init];
	creds.username = self.usernameField.text;
	[creds setPasswordFromClearText:self.passwordField.text];

	[[SCNetwork shared] doLogin:creds withCallback:^(SCLoginError err) {
		if (err == LOGIN_SUCCESS) {
			/*
			 *	Save the credentials and dismiss
			 */

			[[SCRSAManager shared] setUsername:creds.username passwordHash:creds.password];
			[[SCRSAManager shared] encodeSecureData];

			[self.navigationController dismissViewControllerAnimated:YES completion:^{
				self.callback(YES);
			}];
		} else if (err == LOGIN_FAILURE) {
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
		}
	}];
}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
	if ([segue.identifier isEqualToString:@"NewAccountSegue"]) {
		SCSetupAccountViewController *c = segue.destinationViewController;
		c.callback = self.callback;
	}
}

@end
