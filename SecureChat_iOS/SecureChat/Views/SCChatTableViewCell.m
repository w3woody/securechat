//
//  SCChatTableViewCell.m
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

#import "SCChatTableViewCell.h"
#import "SCMessage.h"
#import "SCDateUtils.h"
#import "SCDecryptCache.h"

@interface SCChatTableViewCell ()
@property (weak, nonatomic) IBOutlet SCBubbleView *chatBubble;
@property (weak, nonatomic) IBOutlet UILabel *timeLabel;
@end

@implementation SCChatTableViewCell

- (void)awakeFromNib {
    // Initialization code
}

- (void)setSelected:(BOOL)selected animated:(BOOL)animated {
    [super setSelected:selected animated:animated];

    // Configure the view for the selected state
}

- (void)setMessage:(SCMessageObject *)message atTime:(NSDate *)date
{
	self.chatBubble.message = message;
	self.timeLabel.text = SCFormatDisplayTime(date);
}

@end
