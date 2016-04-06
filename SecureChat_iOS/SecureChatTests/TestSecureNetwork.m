//
//  TestSecureNetwork.m
//  SecureChat
//
//  Created by William Woody on 4/5/16.
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

/*
 *	This test case requires that a test server be running on 127.0.0.1:12345.
 *	The source code for that can be found in the SecureChat Server test units
 */

@interface TestSecureNetwork : XCTestCase

@end

@implementation TestSecureNetwork

- (void)setUp {
    [super setUp];
    // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown {
    // Put teardown code here. This method is called after the invocation of each test method in the class.
    [super tearDown];
}

- (void)testNetwork
{
	NSString *host = @"127.0.0.1";
	NSInteger port = 12345;
	CFReadStreamRef readStream;
	CFWriteStreamRef writeStream;
	uint8_t buffer[256];

	setenv("CFNETWORK_DIAGNOSTICS","3",1);

	CFStreamCreatePairWithSocketToHost(kCFAllocatorDefault, (__bridge CFStringRef)host, (uint32_t)port, &readStream, &writeStream);

	NSInputStream *inStream = (__bridge_transfer NSInputStream *)readStream;
	NSOutputStream *outStream = (__bridge_transfer NSOutputStream *)writeStream;

	NSDictionary *d = @{ (NSString *)kCFStreamSSLValidatesCertificateChain: (id)kCFBooleanFalse };
	[inStream setProperty:NSStreamSocketSecurityLevelNegotiatedSSL forKey:NSStreamSocketSecurityLevelKey];
	[inStream setProperty:d forKey:(id)kCFStreamPropertySSLSettings];

	[inStream open];
	[outStream open];

	[outStream write:(uint8_t *)"a" maxLength:1];
	NSInteger i = [inStream read:buffer maxLength:sizeof(buffer)];
	XCTAssert(i == 1);
	XCTAssert(buffer[0] = 'a');
	[outStream write:(uint8_t *)"c" maxLength:1];

	[inStream close];
	[outStream close];
}

@end
