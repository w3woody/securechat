//
//  SCSettingsViewController.m
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

#import "SCSettingsViewController.h"

@interface SCSettingsViewController ()

@end

@implementation SCSettingsViewController

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

- (IBAction)changePasscode:(id)sender
{
	UIStoryboard *storyboard = [UIStoryboard storyboardWithName:@"ChangePasscode" bundle:nil];
	UIViewController *vc = storyboard.instantiateInitialViewController;
	[self presentViewController:vc animated:YES completion:nil];
}

- (IBAction)disconnectFromAccount:(id)sender
{
	UIStoryboard *storyboard = [UIStoryboard storyboardWithName:@"Disconnect" bundle:nil];
	UIViewController *vc = storyboard.instantiateInitialViewController;
	[self presentViewController:vc animated:YES completion:nil];
}

- (IBAction)changePassword:(id)sender
{
	UIStoryboard *storyboard = [UIStoryboard storyboardWithName:@"ChangePassword" bundle:nil];
	UIViewController *vc = storyboard.instantiateInitialViewController;
	[self presentViewController:vc animated:YES completion:nil];
}

@end
