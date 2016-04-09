//
//  SCOnboardGenerateRSAViewController.m
//  SecureChat
//
//  Created by William Woody on 2/25/16.
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

#import "SCOnboardGenerateRSAViewController.h"
#import "SCRSAManager.h"

@interface SCOnboardGenerateRSAViewController ()
@property (weak, nonatomic) IBOutlet UISegmentedControl *rsaKeyPicker;
@property (weak, nonatomic) IBOutlet UIButton *generateKeyButton;
@property (weak, nonatomic) IBOutlet UIActivityIndicatorView *generateKeySpinner;
@end

@implementation SCOnboardGenerateRSAViewController

- (void)viewDidLoad
{
    [super viewDidLoad];

	[self.generateKeySpinner stopAnimating];
	self.generateKeySpinner.hidden = YES;

	if ([self isRSAKeyGenerated]) {
		[self.generateKeyButton setTitle:NSLocalizedString(@"Regenerate Key", @"Verb") forState:UIControlStateNormal];
	}
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)doGenerateKey:(id)sender
{
	[self generateKeyWithCallback:^{
		UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"RSA Key Generated", @"Success Title") message:@"Your RSA Key has been generated." preferredStyle:UIAlertControllerStyleAlert];
		UIAlertAction *defaultAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"Button") style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
		}];
		[alert addAction:defaultAction];
		[self presentViewController:alert animated:YES completion:nil];
	}];
}

- (IBAction)doNext:(id)sender
{
	if (![self isRSAKeyGenerated]) {
		[self generateKeyWithCallback:^{
			[self performSegueWithIdentifier:@"NextPage" sender:self];
		}];
	} else {
		[self performSegueWithIdentifier:@"NextPage" sender:self];
	}
}

#pragma mark - RSA Key Generation

- (BOOL)isRSAKeyGenerated
{
	return [SCRSAManager shared].publicKey != nil;
}

/*
 *	Core of the generation; this warns the user as needed then generates
 *	the key
 */

- (void)generateKeyWithCallback:(void (^)(void))callback
{
	void (^copyCallback)(void) = [callback copy];
	uint32_t size;
	NSInteger index = self.rsaKeyPicker.selectedSegmentIndex;

	switch (index) {
		default:
		case 0:	size = 1024;	break;
		case 1:	size = 2048;	break;
		case 2:	size = 4096;	break;
	}

	if ([self isRSAKeyGenerated]) {
		UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Regenerate Key?", @"Question") message:NSLocalizedString(@"Are you sure you wish to regenerate your RSA key? Doing so will make it impossible to read stored messages, and should only be done if you suspect your private key on this device has been compromised.",@"Explanation") preferredStyle:UIAlertControllerStyleAlert];

		UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"Cancel", @"Cancel") style:UIAlertActionStyleCancel handler:^(UIAlertAction *action) {
		}];

		[alert addAction:cancelAction];

		UIAlertAction *regenAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"Regenerate", @"verb") style:UIAlertActionStyleDestructive handler:^(UIAlertAction * _Nonnull action) {
			[self generateRSAKey:size withCompletion:copyCallback];
		}];

		[alert addAction:regenAction];

		[self presentViewController:alert animated:YES completion:nil];
	} else {

		if (size > 1024) {
			/*
			 *	Long key warning.
			 */

			UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Generate Key?", @"Question") message:NSLocalizedString(@"The larger the RSA key, the longer this will take. A long RSA key can take over a minute to generate, and cannot be canceled. This operation only needs to be done once. Continue?",@"Explanation") preferredStyle:UIAlertControllerStyleAlert];

			UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"Cancel", @"Cancel") style:UIAlertActionStyleCancel handler:^(UIAlertAction *action) {
			}];

			[alert addAction:cancelAction];

			UIAlertAction *regenAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"Generate", @"verb") style:UIAlertActionStyleDestructive handler:^(UIAlertAction * _Nonnull action) {
				[self generateRSAKey:size withCompletion:copyCallback];
			}];

			[alert addAction:regenAction];

			[self presentViewController:alert animated:YES completion:nil];
		} else {
			/*
			 *	Short key warning
			 */

			[self generateRSAKey:size withCompletion:copyCallback];
		}
	}
}

/*
 *	Start generating RSA key. Asynchronous operation
 */

- (void)generateRSAKey:(uint32_t)keyLen withCompletion:(void (^)(void))completion
{
	void (^callbackCopy)(void) = [completion copy];

	[self.generateKeyButton setHidden:YES];
	[self.generateKeySpinner setHidden:NO];
	[self.generateKeySpinner startAnimating];

	// This is a long operation. Kick off in the background
	dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
		BOOL success = [[SCRSAManager shared] generateRSAKeyWithSize:keyLen];

		dispatch_async(dispatch_get_main_queue(), ^{
			[self.generateKeyButton setHidden:NO];
			[self.generateKeySpinner setHidden:YES];
			[self.generateKeySpinner stopAnimating];

			if (success) {
				[self.generateKeyButton setTitle:NSLocalizedString(@"Regenerate Key", @"Verb") forState:UIControlStateNormal];
				if (callbackCopy) callbackCopy();

			} else {
				UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"RSA Generator Error", @"Error Title") message:@"An unexpected problem occured while generating your RSA key." preferredStyle:UIAlertControllerStyleAlert];
				UIAlertAction *defaultAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"Button") style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
				}];
				[alert addAction:defaultAction];
				[self presentViewController:alert animated:YES completion:nil];
			}
		});
	});
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
