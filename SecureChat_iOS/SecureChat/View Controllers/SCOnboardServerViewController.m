//
//  SCOnboardServerViewController.m
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

#import "SCOnboardServerViewController.h"
#import "SCTextField.h"
#import "SCRSAManager.h"
#import "SCNetwork.h"
#import "SCNetworkResponse.h"

@interface SCOnboardServerViewController ()
@property (weak, nonatomic) IBOutlet SCTextField *serverTextField;
@end

@implementation SCOnboardServerViewController

- (void)viewDidLoad
{
	[super viewDidLoad];
}

- (void)dealloc
{
	[[SCNetwork shared] cancelRequestsWithCaller:self];
}

- (IBAction)doNext:(id)sender
{
	NSString *server = self.serverTextField.text;

	// Make sure something was entered. At some point we can ping the
	// server to make sure its working.
	if (server.length < 3) {
		UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Please enter URL", @"Error Title") message:@"Please enter the URL of a server that is hosting the SecureChat server URL." preferredStyle:UIAlertControllerStyleAlert];
		UIAlertAction *defaultAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"Button") style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
		}];
		[alert addAction:defaultAction];
		[self presentViewController:alert animated:YES completion:nil];
		return;
	}

	/*
	 *	Save the server URL and verify the server actually exists at the
	 *	URL given.
	 */

	[[SCNetwork shared] setServerPrefix:server];
	[[SCNetwork shared] request:@"login/status"
			withParameters:nil
			skipErrorHandler:YES
			caller:self
			response:^(SCNetworkResponse *response) {

		/*
		 *	How can this fail? Network error?
		 */
		
		if (response.success) {
			[[SCRSAManager shared] setServerUrl:server];
			[self performSegueWithIdentifier:@"NextPage" sender:self];

		} else {
			/*
			 *	Error
			 */

			UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Server Error", @"error") message:NSLocalizedString(@"The server you provided is unavailable or incorrect", @"Error") preferredStyle:UIAlertControllerStyleAlert];
			UIAlertAction *action = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"OK") style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
			}];
			[alert addAction:action];
			[self presentViewController:alert animated:YES completion:nil];
		}
	}];
}

@end
