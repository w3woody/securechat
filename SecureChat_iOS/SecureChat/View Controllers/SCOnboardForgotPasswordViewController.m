//
//  SCOnboardForgotPasswordViewController.m
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

#import "SCOnboardForgotPasswordViewController.h"
#import "SCTextField.h"
#import "SCNetwork.h"
#import "SCNetworkResponse.h"

@interface SCOnboardForgotPasswordViewController ()
@property (weak, nonatomic) IBOutlet SCTextField *usernameField;
@end

@implementation SCOnboardForgotPasswordViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view.

	self.usernameField.text = self.username;
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

- (IBAction)doResetPassword:(id)sender
{
	// Show this alert only when password reset request sent.

	NSDictionary *d = @{ @"username": self.usernameField.text };

	[[SCNetwork shared] request:@"login/forgotpassword"
			withParameters:d
			caller:self
			response:^(SCNetworkResponse *response) {

		/*
		 *	Success: move on
		 */

		if (response.success) {
			UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Reset Password", @"Title") message:NSLocalizedString(@"Your password has been reset. Please check on another device on your account for steps to reset your password.", @"Instructions") preferredStyle:UIAlertControllerStyleAlert];
			UIAlertAction *okay = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"OK") style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
				[self.navigationController popViewControllerAnimated:YES];
			}];
			[alert addAction:okay];
			[self presentViewController:alert animated:YES completion:nil];
		}
	}];
}

@end
