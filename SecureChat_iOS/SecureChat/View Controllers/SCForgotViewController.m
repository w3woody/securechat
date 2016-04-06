//
//  SCForgotViewController.m
//  SecureChat
//
//  Created by William Woody on 3/23/16.
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

#import "SCForgotViewController.h"
#import "SCTextField.h"
#import "SCPasswordComplexity.h"
#import "SCNetwork.h"
#import "SCNetworkCredentials.h"
#import "SCNetworkResponse.h"
#import "SCRSAManager.h"

@interface SCForgotViewController ()
@property (weak, nonatomic) IBOutlet SCTextField *passwordField;
@property (weak, nonatomic) IBOutlet SCTextField *retypePasswordField;
@end

@implementation SCForgotViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

- (IBAction)doCancel:(id)sender
{
	[self.navigationController dismissViewControllerAnimated:YES completion:nil];
}

- (IBAction)doNext:(id)sender
{
	NSString *password = self.passwordField.text;

	if (!SCPasswordComplexityTest(password)) {
		UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Password weak", @"Error Title") message:@"Your password must have at least 8 characters, with a capital and lowercase letter, number and punctuation." preferredStyle:UIAlertControllerStyleAlert];
		UIAlertAction *defaultAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"Button") style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
		}];
		[alert addAction:defaultAction];
		[self presentViewController:alert animated:YES completion:nil];
		return;
	}

	NSString *retypedPassword = self.retypePasswordField.text;
	if (![password isEqualToString:retypedPassword]) {
		UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Password doesn't match", @"Error Title") message:@"The new password must match the retyped password." preferredStyle:UIAlertControllerStyleAlert];
		UIAlertAction *defaultAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"Button") style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
		}];
		[alert addAction:defaultAction];
		[self presentViewController:alert animated:YES completion:nil];
		return;
	}

	/*
	 *	Update the passcode
	 */

	SCNetworkCredentials *creds = [[SCNetworkCredentials alloc] init];
	[creds setPasswordFromClearText:password];
	NSString *newPassword = [creds password];

	NSDictionary *d = @{ @"token": self.token,
						 @"password": newPassword };

	[[SCNetwork shared] request:@"login/updateforgotpassword" withParameters:d caller:self response:^(SCNetworkResponse *response) {
		if (response.success) {
			/*
			 *	Successful response; update in storage
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

@end
