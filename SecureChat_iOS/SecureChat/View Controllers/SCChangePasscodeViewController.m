//
//  SCChangePasscodeViewController.m
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

#import "SCChangePasscodeViewController.h"
#import "SCTextField.h"
#import "SCRSAManager.h"

@interface SCChangePasscodeViewController ()
@property (weak, nonatomic) IBOutlet SCTextField *oldPasscode;
@property (weak, nonatomic) IBOutlet SCTextField *updatePasscode;
@property (weak, nonatomic) IBOutlet SCTextField *retypedPasscode;

@end

@implementation SCChangePasscodeViewController

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
	NSString *passcode = self.updatePasscode.text;
	if (passcode.length < 4) {
		UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Passcode too short", @"Error Title") message:@"The passcode you selected is too short. It needs to be at least 4 digits." preferredStyle:UIAlertControllerStyleAlert];
		UIAlertAction *defaultAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"Button") style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
		}];
		[alert addAction:defaultAction];
		[self presentViewController:alert animated:YES completion:nil];
		return;
	}

	NSString *retypedPasscode = self.retypedPasscode.text;
	if (![passcode isEqualToString:retypedPasscode]) {
		UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Passcode doesn't match", @"Error Title") message:@"The new passcode must match the retyped passcode." preferredStyle:UIAlertControllerStyleAlert];
		UIAlertAction *defaultAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"Button") style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
		}];
		[alert addAction:defaultAction];
		[self presentViewController:alert animated:YES completion:nil];
		return;
	}

	/*
	 *	Update the passcode.
	 */

	if (![[SCRSAManager shared] updatePasscode:passcode withOldPasscode:self.oldPasscode.text]) {
		UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Incorrect passcode", @"Error Title") message:@"The old passcode was incorrect." preferredStyle:UIAlertControllerStyleAlert];
		UIAlertAction *defaultAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"Button") style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
		}];
		[alert addAction:defaultAction];
		[self presentViewController:alert animated:YES completion:nil];
		return;
	}

	/*
	 *	Next page
	 */

	[self performSegueWithIdentifier:@"NextPage" sender:self];
}

@end
