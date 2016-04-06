//
//  SCMainViewController.m
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

#import "SCMainViewController.h"
#import "SCRSAManager.h"
#import "SCKeychain.h"
#import "SCMessageQueue.h"
#import "SCSenderTableViewCell.h"
#import "SCChatViewController.h"
#import "SCDeviceCache.h"
#import "SCMessageSender.h"
#import "SCChatSummaryView.h"
#import "SCDevice.h"
#import "SCForgotIntroviewController.h"

@interface SCMainViewController ()
@property (weak, nonatomic) IBOutlet UITableView *tableView;
@property (weak, nonatomic) IBOutlet UIBarButtonItem *editButton;
@property (weak, nonatomic) IBOutlet UIBarButtonItem *writeButton;
@property (weak, nonatomic) IBOutlet SCChatSummaryView *deviceCount;
@property (weak, nonatomic) IBOutlet NSLayoutConstraint *adminPanelHeight;

@property (assign) BOOL receiveFlag;
@property (assign) NSTimer *timer;

@property (copy) NSString *senderName;
@property (assign) NSInteger senderID;

@property (copy) NSString *adminPanelToken;	// token
@end

@implementation SCMainViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view.

	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(refreshContents:) name:NOTIFICATION_NEWMESSAGE object:nil];
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(refreshContents:) name:NOTIFICATION_STARTQUEUE object:nil];
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(handleAdminMessage:) name:NOTIFICATION_ADMINMESSAGE object:nil];

	// Hide admin panel
	self.adminPanelHeight.constant = 0;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)viewDidAppear:(BOOL)animated
{
	[super viewDidAppear:animated];

	/*
	 *	Every time we appear, verify we have an RSA key. We cannot get to
	 *	this screen if we don't have a secure store, so we basically wait
	 *	until we have an RSA key loaded into memory
	 *
	 *	If the select password screen is properly passed, this test will
	 *	fail, so it's safe to test repeatedly.
	 *
	 *	Note if we don't have secure data at all, this is because we're
	 *	about to perform onboarding.
	 */

	if (SCHasSecureData()) {
		if (![[SCRSAManager shared] hasRSAKey]) {
			[self performSegueWithIdentifier:@"LoginOverlay" sender:self];
		}
	}

	/*
	 *	Update the device lists
	 */

	NSString *str = [[SCRSAManager shared] username];
	if (str == nil) {
		// Don't know, so set the value
		[self.deviceCount setDeviceCount:1];
	} else {
		[[SCDeviceCache shared] devicesFor:str withCallback:^(NSInteger userID, NSArray<SCDevice *> *array) {
			[self.deviceCount setDeviceCount:array.count];

			/*
			 *	While I'm here, determine that the public key announced by
			 *	the back end matches the public key I have on file, so to
			 *	make sure I haven't been hacked. If I have, alert the
			 *	user.
			 */

			BOOL valid = NO;
			NSString *deviceID = [[SCRSAManager shared] deviceUUID];
			NSString *publicKey = [[SCRSAManager shared] publicKey];
			for (SCDevice *dev in array) {
				if ([dev.deviceid isEqualToString:deviceID]) {
					if ([dev.pubkeytext isEqualToString:publicKey]) {
						valid = YES;
					}
				}
			}

			if (valid == NO) {
				[self warnUserSecurityBreach];
			}
		}];
	}

	/*
	 *	Ask a refresh
	 */

	[self.tableView reloadData];
}

/*
 *	This device was not found in the published list of devices. Complain.
 */

- (void)warnUserSecurityBreach
{
	NSString *title = NSLocalizedString(@"Security Error", @"Title");
	NSString *msg = NSLocalizedString(@"This device's public credentials could not be found on the remote server. This indicates a security breach or a problem with this device. We recommend you stop using SecureChat now and investigate the problem.", @"Message");
	UIAlertController *a = [UIAlertController alertControllerWithTitle:title message:msg preferredStyle:UIAlertControllerStyleAlert];

	UIAlertAction *action = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"OK") style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) {
	}];
	[a addAction:action];

	[self presentViewController:a animated:YES completion:nil];
}

#pragma mark - Admin Messages

- (void)handleAdminMessage:(NSNotification *)n
{
	// The notification JSON is in our user dictionary. Determine how to
	// process.

	NSDictionary *d = n.userInfo;
	NSString *cmd = d[@"cmd"];
	if ([cmd isEqualToString:@"forgotpassword"]) {
		NSString *token = d[@"token"];

		if (self.adminPanelToken == nil) {
			self.adminPanelHeight.constant = 45;
			[UIView animateWithDuration:0.33 animations:^{
				[self.view layoutIfNeeded];
			} completion:^(BOOL finished) {
			}];
		}
		self.adminPanelToken = token;
	}
}

- (IBAction)doForgotPassword:(id)sender
{
	UIStoryboard *s = [UIStoryboard storyboardWithName:@"ForgotPassword" bundle:nil];
	UINavigationController *vc = [s instantiateInitialViewController];
	SCForgotIntroViewController *rvc = vc.viewControllers[0];
	rvc.token = self.adminPanelToken;
	[self presentViewController:vc animated:YES completion:^{
		self.adminPanelHeight.constant = 45;
		[self.view layoutIfNeeded];
	}];

}

- (IBAction)doCancelAdminPanel:(id)sender
{
	if (self.adminPanelToken) {
		self.adminPanelToken = nil;
		self.adminPanelHeight.constant = 0;
		[UIView animateWithDuration:0.33 animations:^{
			[self.view layoutIfNeeded];
		} completion:^(BOOL finished) {
		}];
	}
}

#pragma mark - Functions

- (IBAction)doEdit:(id)sender
{
	if (self.tableView.isEditing) {
		[self.tableView setEditing:NO animated:YES];
		[self.editButton setTitle:NSLocalizedString(@"Edit", @"Edit Button")];
	} else {
		[self.tableView setEditing:YES animated:YES];
		[self.editButton setTitle:NSLocalizedString(@"Done", @"Done Button")];
	}
}

- (void)selfChatError
{
	NSString *f = NSLocalizedString(@"You cannot chat with yourself.", @"message");
	UIAlertController *a = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Error", @"Title") message:f preferredStyle:UIAlertControllerStyleAlert];

	UIAlertAction *action = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK",@"OK") style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) {
	}];
	[a addAction:action];

	[self presentViewController:a animated:YES completion:nil];
}

- (IBAction)doWrite:(id)sender
{
	UIAlertController *a = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"New Chat", @"Title") message:NSLocalizedString(@"Please specify the user you wish to chat with", @"Message") preferredStyle:UIAlertControllerStyleAlert];

	[a addTextFieldWithConfigurationHandler:^(UITextField *textField) {
		[textField setPlaceholder:NSLocalizedString(@"Username", @"text label")];
	}];

	UIAlertAction *action = [UIAlertAction actionWithTitle:NSLocalizedString(@"Chat",@"Chat") style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) {
		/*
		 *	Chat
		 */

		NSString *chat = a.textFields[0].text;
		if ([chat isEqualToString:[[SCRSAManager shared] username]]) {
			[self selfChatError];
		} else {
			[[SCDeviceCache shared] devicesFor:chat withCallback:^(NSInteger userID, NSArray<SCDevice *> *array) {
				if (userID) {
					self.senderID = userID;
					self.senderName = chat;

					[self performSegueWithIdentifier:@"DetailPage" sender:self];
				}
			}];
		}
	}];
	[a addAction:action];

	action = [UIAlertAction actionWithTitle:NSLocalizedString(@"Cancel",@"Cancel") style:UIAlertActionStyleCancel handler:^(UIAlertAction *action) {
	}];
	[a addAction:action];

	[self presentViewController:a animated:YES completion:nil];
}


#pragma mark - Table View

- (void)detectReset:(NSTimer *)timer
{
	if (self.receiveFlag) {
		self.receiveFlag = NO;
	} else {
		[self.timer invalidate];
		self.timer = nil;
		[self.tableView reloadData];
	}
}

- (void)refreshContents:(NSNotification *)n
{
	/*
	 *	Works to make sure if we get hammered with a bunch of notifications
	 *	that we throttle this so we don't bang the reloadData code rapidly
	 */
	
	if (self.timer == nil) {
		self.timer = [NSTimer scheduledTimerWithTimeInterval:0.25 target:self selector:@selector(detectReset:) userInfo:nil repeats:YES];
	}
	self.receiveFlag = YES;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
	NSArray<SCMessageSender *> *a = [[SCMessageQueue shared] senders];
	return a.count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
	SCSenderTableViewCell *cell = (SCSenderTableViewCell *)[tableView dequeueReusableCellWithIdentifier:@"ChatCell" forIndexPath:indexPath];

	NSArray<SCMessageSender *> *a = [[SCMessageQueue shared] senders];
	SCMessageSender *s = a[indexPath.row];

	[cell setSender:s];

	return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
	NSArray<SCMessageSender *> *a = [[SCMessageQueue shared] senders];
	SCMessageSender *s = a[indexPath.row];
	self.senderName = s.senderName;
	self.senderID = s.senderID;

	[self performSegueWithIdentifier:@"DetailPage" sender:self];
}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
	if ([segue.identifier isEqualToString:@"DetailPage"]) {
		SCChatViewController *chat = segue.destinationViewController;

		chat.senderName = self.senderName;
		chat.senderID = self.senderID;
	}
}

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath
{
	return YES;
}

- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(nonnull NSIndexPath *)indexPath
{
	if (editingStyle == UITableViewCellEditingStyleDelete) {
		/* Delete this sender */
		NSArray<SCMessageSender *> *a = [[SCMessageQueue shared] senders];
		SCMessageSender *s = a[indexPath.row];
		if ([[SCMessageQueue shared] deleteSenderForIdent:s.senderID]) {
			[tableView deleteRowsAtIndexPaths:@[ indexPath ] withRowAnimation:UITableViewRowAnimationAutomatic];
		}
	}
}


@end
