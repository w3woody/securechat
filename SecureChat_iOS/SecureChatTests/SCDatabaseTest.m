//
//  SCDatabaseTest.m
//  SecureChat
//
//  Created by William Woody on 3/14/16.
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

#import <XCTest/XCTest.h>
#import "SCMessageDatabase.h"
#import "SCMessageSender.h"
#import "SCMessage.h"

@interface SCDatabaseTest : XCTestCase

@end

@implementation SCDatabaseTest

- (void)setUp {
    [super setUp];
    // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown {
    // Put teardown code here. This method is called after the invocation of each test method in the class.
    [super tearDown];
}

- (void)testFileIO
{
	SCMessageDatabase *db = [[SCMessageDatabase alloc] init];
	[SCMessageDatabase removeDatabase];

	NSString *path = [SCMessageDatabase databaseFileLocation];
	XCTAssert([db openDatabase]);
	XCTAssert([[NSFileManager defaultManager] fileExistsAtPath:path]);
	db = nil;
	[SCMessageDatabase removeDatabase];
	XCTAssert(![[NSFileManager defaultManager] fileExistsAtPath:path]);
}

- (void)testWriteMessage
{
	SCMessageDatabase *db = [[SCMessageDatabase alloc] init];
	[SCMessageDatabase removeDatabase];

	XCTAssert([db openDatabase]);

	/*
	 *	Write two messages, and veriy our summary and message stores work
	 */

	NSData *data = [@"Hi" dataUsingEncoding:NSUTF8StringEncoding];
	XCTAssert([db insertMessageFromSenderID:1 name:@"sender" received:0 withMessageID:2 timestamp:nil message:data]);

	XCTAssert([db insertMessageFromSenderID:1 name:@"sender" received:0 withMessageID:3 timestamp:nil message:data]);

	NSArray<SCMessageSender *> *sender = [db senders];
	XCTAssert([sender count] == 1);
	XCTAssert([sender[0].senderName isEqualToString:@"sender"]);
	XCTAssert(sender[0].senderID == 1);
	XCTAssert(sender[0].messageID == 3);
	XCTAssert(sender[0].receiveFlag == NO);

	NSArray<SCMessage *> *messages = [db messagesInRange:NSMakeRange(0, 10) fromSender:1];
	XCTAssert(messages.count == 2);
	XCTAssert(messages[0].messageID == 2);
	XCTAssert(messages[0].receiveFlag == NO);
	XCTAssert(messages[1].messageID == 3);
	XCTAssert(messages[1].receiveFlag == NO);

	/*
	 *	Verify caching works
	 */
	
	XCTAssert([db insertMessageFromSenderID:1 name:@"sender" received:1 withMessageID:4 timestamp:nil message:data]);

	sender = [db senders];
	XCTAssert([sender count] == 1);
	XCTAssert([sender[0].senderName isEqualToString:@"sender"]);
	XCTAssert(sender[0].senderID == 1);
	XCTAssert(sender[0].messageID == 4);
	XCTAssert(sender[0].receiveFlag == YES);
}


@end
