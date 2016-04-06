//
//  ManagerTests.m
//  SecureChat
//
//  Created by William Woody on 2/27/16.
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
#import "SCRSAManager.h"
#import "SCRSAEncoder.h"
#import "SCKeychain.h"

@interface ManagerTests : XCTestCase

@end

@implementation ManagerTests

- (void)setUp {
    [super setUp];
    // Put setup code here. This method is called before the invocation of each test method in the class.

	SCClearSecureData();		// We're going to test the keychain stuff.
}

- (void)tearDown {
    // Put teardown code here. This method is called after the invocation of each test method in the class.
    [super tearDown];
}

/*
 *	This test should exercise all code paths for SCRSAManager and for
 *	SCRSAEncoder
 */

- (void)testRSAManager
{
	// Fails if there is something in our secure store, but that
	// should have been cleared in the setup.
	XCTAssert([[SCRSAManager shared] setPasscode:@"1234"]);

	XCTestExpectation *e = [self expectationWithDescription:@"Test RSA Generator"];

	[[SCRSAManager shared] generateRSAKeyWithSize:1024 callback:^(BOOL success) {
		XCTAssert(success);
		[e fulfill];
	}];
	[self waitForExpectationsWithTimeout:3600 handler:nil];

	[[SCRSAManager shared] encodeSecureData];

	// At this point the RSA manager should be set up. Validate

	/*
	 *	Test password checking
	 */

	XCTAssert(![[SCRSAManager shared] setPasscode:@"1244"]);
	XCTAssert([[SCRSAManager shared] setPasscode:@"1234"]);

	/*
	 *	Create an encoder and encode a block of data
	 */

	NSData *data = [@"Hello world." dataUsingEncoding:NSUTF8StringEncoding];
	SCRSAEncoder *encoder = [[SCRSAEncoder alloc] initWithEncoderKey:[[SCRSAManager shared] publicKey]];

	NSData *encData = [encoder encodeData:data];

	/*
	 *	Now decode and verify we got the same thing
	 */

	NSData *decData = [[SCRSAManager shared] decodeData:encData];

	XCTAssert([data isEqualToData:decData]);
}

@end
