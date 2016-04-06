//
//  SCOnboardCreateUsernameViewController.m
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

#import "SCOnboardCreateUsernameViewController.h"
#import "SCTextField.h"
#import "SCRSAManager.h"
#import "SCNetwork.h"
#import "SCNetworkCredentials.h"
#import "SCNetworkResponse.h"
#import "SCPasswordComplexity.h"
#import "SCMessageQueue.h"

@interface SCOnboardCreateUsernameViewController ()
@property (weak, nonatomic) IBOutlet SCTextField *usernameField;
@property (weak, nonatomic) IBOutlet SCTextField *passwordField;

@end

@implementation SCOnboardCreateUsernameViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)dealloc
{
	[[SCNetwork shared] cancelRequestsWithCaller:self];
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/
- (IBAction)doNext:(id)sender
{
	/*
	 *	Password complexity test
	 */

	NSString *password = self.passwordField.text;
	if (!SCPasswordComplexityTest(password)) {
		UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Password weak", @"Error Title") message:@"Your password must have at least 8 characters, with a capital and lowercase letter, number and punctuation." preferredStyle:UIAlertControllerStyleAlert];
		UIAlertAction *defaultAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"Button") style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
		}];
		[alert addAction:defaultAction];
		[self presentViewController:alert animated:YES completion:nil];
		return;
	}

	/*
	 *	At this point we have a public/private key, a device ID, and
	 *	a server. We also uave a proposed username/password pair. So
	 *	we create a new account
	 */

	SCNetworkCredentials *creds = [[SCNetworkCredentials alloc] init];
	creds.username = self.usernameField.text;
	[creds setPasswordFromClearText:self.passwordField.text];

	NSDictionary *req = @{ @"username": creds.username,
						   @"password": creds.password,
						   @"deviceid": [[SCRSAManager shared] deviceUUID],
						   @"pubkey": [[SCRSAManager shared] publicKey] };

	[[SCNetwork shared] request:@"login/createaccount"
			withParameters:req
			caller:self
			response:^(SCNetworkResponse *response) {

		/*
		 *	Errors are handled by the network delegate, so we only care 
		 *	about success
		 */

		if (response.success) {
			/*
			 *	Success. Save the username and password, and save the
			 *	whole thing to the back end.
			 */

			[[SCRSAManager shared] setUsername:creds.username passwordHash:creds.password];
			[[SCRSAManager shared] encodeSecureData];

			/*
			 *	We have what we need to start the queue
			 */

			[[SCMessageQueue shared] startQueue];

			/*
			 *	Done; go to the next page
			 */

			[self performSegueWithIdentifier:@"NextPage" sender:self];
		}
	}];
}

@end
