//
//  SCChatViewController.m
//  SecureChat
//
//  Created by William Woody on 3/19/16.
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

#import "SCChatViewController.h"
#import "SCMessageSender.h"
#import "SCMessageQueue.h"
#import "SCMessage.h"
#import "SCChatTableViewCell.h"
#import "SCMessageQueue.h"
#import "SCWait.h"
#import "SCDeviceCache.h"
#import "SCChatSummaryView.h"
#import "SCDecryptCache.h"
#import "SCMessageObject.h"

@interface SCChatViewController ()
@property (weak, nonatomic) IBOutlet UITableView *tableView;
@property (weak, nonatomic) IBOutlet UIButton *sendButton;
@property (weak, nonatomic) IBOutlet UITextView *textView;
@property (weak, nonatomic) IBOutlet SCChatSummaryView *deviceCount;
@property (weak, nonatomic) IBOutlet UILabel *chatPrompt;

@property (weak, nonatomic) IBOutlet NSLayoutConstraint *editHeight;
@property (weak, nonatomic) IBOutlet NSLayoutConstraint *bottomBorder;

@property (strong) NSMutableDictionary<NSNumber *, NSString *> *decode;
@end

@implementation SCChatViewController

- (void)viewDidLoad
{
    [super viewDidLoad];

	self.navigationItem.title = self.senderName;

	// Do any additional setup after loading the view.
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(keyboardShowHide:) name:UIKeyboardWillShowNotification object:nil];
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(keyboardShowHide:) name:UIKeyboardWillHideNotification object:nil];

	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(refreshContents:) name:NOTIFICATION_NEWMESSAGE object:nil];

	/*
	 *	Cached heights
	 */

	self.decode = [[NSMutableDictionary alloc] init];

	/*
	 *	Determine device count 
	 */

	[[SCDeviceCache shared] devicesFor:self.senderName withCallback:^(NSInteger userID, NSArray<SCDevice *> *array) {
		[self.deviceCount setDeviceCount:array.count];
		if (userID == 0) {
			self.editHeight.constant = 0;
		}
	}];

	/*
	 *	Clear
	 */

	self.textView.text = @"";
	self.editHeight.constant = 45;
	self.chatPrompt.hidden = NO;

	/*
	 *	Scroll to bottom
	 */

	NSInteger len = [[SCMessageQueue shared] messagesForSender:self.senderID];
	if (len > 0) {
		NSIndexPath *path = [NSIndexPath indexPathForRow:len-1 inSection:0];
		[self.tableView scrollToRowAtIndexPath:path atScrollPosition:UITableViewScrollPositionBottom animated:YES];
	}
}

- (void)dealloc
{
	[[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Notifications

- (void)refreshContents:(NSNotification *)n
{
	NSDictionary *d = n.userInfo;
	NSNumber *userid = d[@"userid"];
	if (userid.integerValue == self.senderID) {
		/*
		 *	Got a new message. Refresh and scroll to bottom
		 */

		[self.tableView reloadData];

		NSInteger len = [[SCMessageQueue shared] messagesForSender:self.senderID];
		if (len > 0) {
			NSIndexPath *path = [NSIndexPath indexPathForRow:len-1 inSection:0];
			[self.tableView scrollToRowAtIndexPath:path atScrollPosition:UITableViewScrollPositionBottom animated:YES];
		}
	}
}

#pragma mark - Keyboard

- (void)keyboardShowHide:(NSNotification *)n
{
	CGRect krect;

	/* Extract the size of the keyboard when the animation stops */
	krect = [n.userInfo[UIKeyboardFrameEndUserInfoKey] CGRectValue];

	/* Convert that to the rectangle in our primary view. Note the raw
	 * keyboard size from above is in the window's frame, which could be
	 * turned on its side.
	 */
	krect = [self.view convertRect:krect fromView:nil];

	/* Get the animation duration, and animation curve */
	NSTimeInterval duration = [[n.userInfo objectForKey:UIKeyboardAnimationDurationUserInfoKey] doubleValue];
	UIViewAnimationCurve curve = [[n.userInfo objectForKey:UIKeyboardAnimationCurveUserInfoKey] intValue];

	/* Calculate the bottom border height */
	CGFloat height = [n.name isEqualToString:UIKeyboardWillHideNotification] ? 0 : krect.size.height;
	self.bottomBorder.constant = height;

	/* Kick off the animation. What you do with the keyboard size is up to you */
	[UIView animateWithDuration:0
			delay:duration
			options:UIViewAnimationOptionBeginFromCurrentState | curve
			animations:^{
				/* Move things around */
				[self.view layoutIfNeeded];
			}
			completion:^(BOOL finished) {
				/* Finish up here */
			}];
}

#pragma mark - Text Entry

- (IBAction)dismissKeyboard:(id)sender
{
	[self.textView resignFirstResponder];
}

- (void)textViewDidBeginEditing:(UITextView *)textView
{
	self.chatPrompt.hidden = YES;
}

- (void)textViewDidEndEditing:(UITextView *)textView
{
	if ([textView.text length] > 0) {
		self.chatPrompt.hidden = YES;
	} else {
		self.chatPrompt.hidden = NO;
	}
}

- (void)textViewDidChange:(UITextView *)textView
{
	CGSize size = [textView sizeThatFits:CGSizeMake(textView.bounds.size.width, 9999)];

	CGFloat height = ceil(size.height) + 5;
	if (height < 45) height = 45;
	if (height > 132) height = 132;
	self.editHeight.constant = height;

	self.chatPrompt.hidden = YES;
}

- (IBAction)doSubmit:(id)sender
{
	NSString *cleartext = self.textView.text;
	if ([cleartext length] == 0) return;

	self.textView.text = @"";
	self.editHeight.constant = 45;
	self.chatPrompt.hidden = NO;

	SCMessageObject *msg = [[SCMessageObject alloc] initWithString:cleartext];

	// We can do better than flashing the wait spinner. Ideally we need to
	// have each bubble contain a spinner when in progress
	[[SCWait shared] wait];

	[[SCMessageQueue shared] sendMessage:msg toSender:self.senderName completion:^(BOOL success) {
		[[SCWait shared] stopWait];
		if (success) {
			// TODO -- send success?
		} else {
			// TODO -- send failure?
		}
	}];
}

#pragma mark - Table View

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
	return [[SCMessageQueue shared] messagesForSender:self.senderID];
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
	/*
	 *	Get the decrypted message for height calculation
	 */

	NSInteger ix = indexPath.row;
	NSRange range = NSMakeRange(ix & ~15, 16);
	NSArray<SCMessage *> *marray = [[SCMessageQueue shared] messagesInRange:range fromSender:self.senderID];
	SCMessage *message = marray[ix & 15];

	SCMessageObject *msg = [[SCDecryptCache shared] decrypt:message.message atIndex:message.messageID withCallback:nil];
	if (msg == nil) return 60;

	CGSize size = [SCBubbleView sizeWithMessage:msg width:self.tableView.bounds.size.width - 80];
	return ceil(size.height + 31);		// text spacing + top spacing
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
	/*
	 *	Get a range for the index by blocks of 16
	 */

	NSInteger ix = indexPath.row;
	NSRange range = NSMakeRange(ix & ~15, 16);
	NSArray<SCMessage *> *marray = [[SCMessageQueue shared] messagesInRange:range fromSender:self.senderID];

	SCMessage *message = marray[ix & 15];
	NSString *clabel = !message.receiveFlag ? @"ReceiveCell" : @"SendCell";

	/*
	 *	Chat setup
	 */

	SCChatTableViewCell *cell = (SCChatTableViewCell *)[tableView dequeueReusableCellWithIdentifier:clabel forIndexPath:indexPath];

	SCMessageObject *msg = [[SCDecryptCache shared] decrypt:message.message atIndex:message.messageID withCallback:^(NSInteger ident, SCMessageObject *msg) {
		// row loaded, so reload
		[self.tableView reloadRowsAtIndexPaths:@[ indexPath ] withRowAnimation:UITableViewRowAnimationFade];
	}];
	if (msg == nil) {
		// TODO: Rethink the decrypt placeholder
		msg = [[SCMessageObject alloc] initWithString:NSLocalizedString(@"(decrypt)", @"message")];
	}
	[cell setMessage:msg atTime:message.timestamp];

	return cell;
}

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath
{
	return YES;
}

- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(nonnull NSIndexPath *)indexPath
{
	if (editingStyle == UITableViewCellEditingStyleDelete) {
		/* Delete this message */
		NSInteger ix = indexPath.row;
		NSRange range = NSMakeRange(ix & ~15, 16);
		NSArray<SCMessage *> *marray = [[SCMessageQueue shared] messagesInRange:range fromSender:self.senderID];
		SCMessage *message = marray[ix & 15];

		if ([[SCMessageQueue shared] deleteMessageForIdent:message.messageID]) {
			[tableView deleteRowsAtIndexPaths:@[ indexPath ] withRowAnimation:UITableViewRowAnimationAutomatic];
		}
	}
}

@end
