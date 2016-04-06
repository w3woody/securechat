//
//  SCChangePasswordViewController.m
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

#import "SCChangePasswordViewController.h"
#import "SCTextField.h"
#import "SCPasswordComplexity.h"
#import "SCNetwork.h"
#import "SCNetworkCredentials.h"
#import "SCNetworkResponse.h"
#import "SCRSAManager.h"

@interface SCChangePasswordViewController ()
@property (weak, nonatomic) IBOutlet SCTextField *oldPasswordField;
@property (weak, nonatomic) IBOutlet SCTextField *changedPasswordField;
@property (weak, nonatomic) IBOutlet SCTextField *retypedPasswordField;
@end

@implementation SCChangePasswordViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)doNext:(id)sender
{
	/*
	 *	Validation checks
	 */
	
	NSString *password = self.changedPasswordField.text;
	if (!SCPasswordComplexityTest(password)) {
		UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Password weak", @"Error Title") message:@"Your password must have at least 8 characters, with a capital and lowercase letter, number and punctuation." preferredStyle:UIAlertControllerStyleAlert];
		UIAlertAction *defaultAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"Button") style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
		}];
		[alert addAction:defaultAction];
		[self presentViewController:alert animated:YES completion:nil];
		return;
	}

	NSString *retypedPassword = self.retypedPasswordField.text;
	if (![password isEqualToString:retypedPassword]) {
		UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Password doesn't match", @"Error Title") message:@"The new password must match the retyped password." preferredStyle:UIAlertControllerStyleAlert];
		UIAlertAction *defaultAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"Button") style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
		}];
		[alert addAction:defaultAction];
		[self presentViewController:alert animated:YES completion:nil];
		return;
	}

	/*
	 *	Update the passcode. Get the token
	 */

	[[SCNetwork shared] request:@"login/token" withParameters:nil caller:self response:^(SCNetworkResponse *response) {
		if (response.success) {
			NSString *token = response.data[@"token"];

			/*
			 *	Encrypt old password with token
			 */

			SCNetworkCredentials *creds = [[SCNetworkCredentials alloc] init];
			[creds setPasswordFromClearText:self.oldPasswordField.text];
			NSString *oldPassword = [creds hashPasswordWithToken:token];
			[creds setPasswordFromClearText:password];
			NSString *newPassword = creds.password;

			NSDictionary *req = @{ @"oldpassword": oldPassword,
								   @"newpassword": newPassword };

			[[SCNetwork shared] request:@"login/changepassword" withParameters:req caller:self response:^(SCNetworkResponse *response) {
				if (response.success) {
					/*
					 *	Successful response
					 */

					NSString *username = [[SCRSAManager shared] username];
					[[SCRSAManager shared] setUsername:username passwordHash:creds.password];
					[[SCRSAManager shared] encodeSecureData];

					/*
					 *	Done; go to the next page
					 */
					[self performSegueWithIdentifier:@"NextPage" sender:self];
				}
			}];
		}
	}];
}

@end
