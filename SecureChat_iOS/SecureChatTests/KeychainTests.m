//
//  KeychainTests.m
//  SecureChat
//
//  Created by William Woody on 2/26/16.
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
#import "SCKeychain.h"
#import "SCSecuredData.h"

@interface KeychainTests : XCTestCase

@end

@implementation KeychainTests

- (void)setUp {
    [super setUp];
    // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown {
    // Put teardown code here. This method is called after the invocation of each test method in the class.
    [super tearDown];
}

- (void)testKeychain
{
	SCClearSecureData();

	XCTAssert(NO == SCHasSecureData());

	NSString *str = @"123";
	NSData *data = [str dataUsingEncoding:NSUTF8StringEncoding];

	SCSaveSecureData(data);

	XCTAssert(YES == SCHasSecureData());

	NSData *test = SCGetSecureData();
	XCTAssert([test isEqualToData:data]);

	SCClearSecureData();

	XCTAssert(NO == SCHasSecureData());
}

- (void)testSecuredData
{
	SCSecuredData *data = [[SCSecuredData alloc] init];
	data.uuid = @"1";
	data.publicKey = @"1234";
	data.privateKey = @"12345";

	NSData *enc = [data serializeData];

	XCTAssert([enc length] % 8 == 0);	// make sure 64-bit size

	SCSecuredData *ret = [SCSecuredData deserializeData:enc];
	XCTAssert([ret.uuid isEqualToString:@"1"]);
	XCTAssert([ret.publicKey isEqualToString:@"1234"]);
	XCTAssert([ret.privateKey isEqualToString:@"12345"]);
}

@end
