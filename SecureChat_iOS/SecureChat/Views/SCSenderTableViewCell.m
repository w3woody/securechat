//
//  SCSenderTableViewCell.m
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

#import "SCSenderTableViewCell.h"
#import "SCMessageSender.h"
#import "SCRSAManager.h"
#import "SCRSAEncoder.h"
#import "SCDecryptCache.h"

@interface SCSenderTableViewCell ()
@property (strong) SCMessageSender *messageSender;
@property (weak, nonatomic) IBOutlet UILabel *senderName;
@property (weak, nonatomic) IBOutlet UILabel *lastMessage;
@end

@implementation SCSenderTableViewCell

- (void)awakeFromNib {
    // Initialization code
}

- (void)setSelected:(BOOL)selected animated:(BOOL)animated {
    [super setSelected:selected animated:animated];

    // Configure the view for the selected state
}

- (void)setSender:(SCMessageSender *)sender
{
	self.messageSender = sender;
	self.senderName.text = sender.senderName;
	self.lastMessage.text = @"";

	NSString *str = [[SCDecryptCache shared] decrypt:sender.lastMessage atIndex:sender.messageID withCallback:^(NSInteger ident, NSString *msg) {
		if (ident == sender.messageID) {
			self.lastMessage.text = msg;
		}
	}];
	
	if (str) {
		self.lastMessage.text = str;
	} else {
		self.lastMessage.text = NSLocalizedString(@"(decrypting)", @"decrypting");
	}
}

@end
