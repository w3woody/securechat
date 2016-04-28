//
//  BlockEncodeTest.m
//  SecureChat
//
//  Created by William Woody on 4/28/16.
//  Copyright © 2016 William Edward Woody. All rights reserved.
//

#import <XCTest/XCTest.h>
#include <string.h>
#include "SCChecksum.h"
#include "SCSecureHash.h"
#include "SCRSAEncryption.h"
#import "SCRSAEncoder.h"
#import "SCRSAManager.h"
#include <Security/Security.h>

@interface BlockEncodeTest : XCTestCase

@end

@implementation BlockEncodeTest

- (void)setUp
{
    [super setUp];
}

- (void)tearDown
{
    // Put teardown code here. This method is called after the invocation of each test method in the class.
    [super tearDown];
}

- (void)testExample
{
	SCRSAPadding padding(1024);

	if (![[SCRSAManager shared] setPasscode:@"1234"]) {
		[[SCRSAManager shared] clear];

		[[SCRSAManager shared] setPasscode:@"1234"];
		[[SCRSAManager shared] generateRSAKeyWithSize:1024];
	} else if (![[SCRSAManager shared] hasRSAKey]) {
		[[SCRSAManager shared] generateRSAKeyWithSize:1024];
	}

	uint8_t msg[112];		// should be padding length
	XCTAssert(sizeof(msg) == padding.GetMessageSize());

	SecRandomCopyBytes(kSecRandomDefault, sizeof(msg), msg);

	// Padding encode/decode test
	uint8_t ebuf[128];
	padding.Encode(msg,ebuf);
	uint8_t mbuf[112];
	XCTAssert(padding.Decode(ebuf,mbuf));
	XCTAssert(!memcmp(msg,mbuf,112));

	NSString *pubKey = [[SCRSAManager shared] publicKey];
	SCRSAEncoder *enc = [[SCRSAEncoder alloc] initWithEncoderKey:pubKey];

	NSData *data = [NSData dataWithBytesNoCopy:msg length:112 freeWhenDone:NO];
	NSData *edata = [enc encodeData:data];
	NSData *ddata = [[SCRSAManager shared] decodeData:edata];

	XCTAssert([data isEqualToData:ddata]);
}

@end
