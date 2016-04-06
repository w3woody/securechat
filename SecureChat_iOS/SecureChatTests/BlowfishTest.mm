//
//  BlowfishTest.m
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
#include "SCBlowfish.h"

@interface BlowfishTest : XCTestCase

@end

@implementation BlowfishTest

- (void)setUp {
    [super setUp];
    // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown {
    // Put teardown code here. This method is called after the invocation of each test method in the class.
    [super tearDown];
}


static const char *TestVectors[] = {
	/* Key bytes       clear bytes        cipher bytes */
	"0000000000000000","0000000000000000","4EF997456198DD78",
	"FFFFFFFFFFFFFFFF","FFFFFFFFFFFFFFFF","51866FD5B85ECB8A",
	"3000000000000000","1000000000000001","7D856F9A613063F2",
	"1111111111111111","1111111111111111","2466DD878B963C9D",
	"0123456789ABCDEF","1111111111111111","61F9C3802281B096",

	"1111111111111111","0123456789ABCDEF","7D0CC630AFDA1EC7",
	"0000000000000000","0000000000000000","4EF997456198DD78",
	"FEDCBA9876543210","0123456789ABCDEF","0ACEAB0FC6A0A28D",
	"7CA110454A1A6E57","01A1D6D039776742","59C68245EB05282B",
	"0131D9619DC1376E","5CD54CA83DEF57DA","B1B8CC0B250F09A0",

	"07A1133E4A0B2686","0248D43806F67172","1730E5778BEA1DA4",
	"3849674C2602319E","51454B582DDF440A","A25E7856CF2651EB",
	"04B915BA43FEB5B6","42FD443059577FA2","353882B109CE8F1A",
	"0113B970FD34F2CE","059B5E0851CF143A","48F4D0884C379918",
	"0170F175468FB5E6","0756D8E0774761D2","432193B78951FC98",

	"43297FAD38E373FE","762514B829BF486A","13F04154D69D1AE5",
	"07A7137045DA2A16","3BDD119049372802","2EEDDA93FFD39C79",
	"04689104C2FD3B2F","26955F6835AF609A","D887E0393C2DA6E3",
	"37D06BB516CB7546","164D5E404F275232","5F99D04F5B163969",
	"1F08260D1AC2465E","6B056E18759F5CCA","4A057A3B24D3977B",

	"584023641ABA6176","004BD6EF09176062","452031C1E4FADA8E",
	"025816164629B007","480D39006EE762F2","7555AE39F59B87BD",
	"49793EBC79B3258F","437540C8698F3CFA","53C55F9CB49FC019",
	"4FB05E1515AB73A7","072D43A077075292","7A8E7BFA937E89A3",
	"49E95D6D4CA229BF","02FE55778117F12A","CF9C5D7A4986ADB5",

	"018310DC409B26D6","1D9D5C5018F728C2","D1ABB290658BC778",
	"1C587F1C13924FEF","305532286D6F295A","55CB3774D13EF201",
	"0101010101010101","0123456789ABCDEF","FA34EC4847B268B2",
	"1F1F1F1F0E0E0E0E","0123456789ABCDEF","A790795108EA3CAE",
	"E0FEE0FEF1FEF1FE","0123456789ABCDEF","C39E072D9FAC631D",

	"0000000000000000","FFFFFFFFFFFFFFFF","014933E0CDAFF6E4",
	"FFFFFFFFFFFFFFFF","0000000000000000","F21E9A77B71C49BC",
	"0123456789ABCDEF","0000000000000000","245946885754369A",
	"FEDCBA9876543210","FFFFFFFFFFFFFFFF","6B5C5A9C5D9E0A5A",
};

static int FromHex(char c)
{
	if (('0' <= c) && (c <= '9')) return c - '0';
	if (('A' <= c) && (c <= 'F')) return c - 'A' + 10;
	if (('a' <= c) && (c <= 'f')) return c - 'a' + 10;
	return 0;
}

static void UnrollKey(const char *text, uint8_t k[8])
{
	for (int i = 0; i < 8; ++i) {
		k[i] = (FromHex(text[i*2]) << 4) | FromHex(text[i*2+1]);
	}
}

static void Unroll(const char *text, uint32_t x[2])
{
	uint32_t tmp;

	tmp = 0;
	for (int i = 0; i < 8; ++i) {
		tmp = (tmp << 4) | FromHex(text[i]);
	}
	x[0] = tmp;

	tmp = 0;
	for (int i = 8; i < 16; ++i) {
		tmp = (tmp << 4) | FromHex(text[i]);
	}
	x[1] = tmp;
}

- (void)testBlowfish
{
	for (int i = 0; i < 34; ++i) {
		uint8_t key[8];
		uint32_t clear[2];
		uint32_t enc[2];
		uint32_t scratch[2];

		UnrollKey(TestVectors[i*3],key);
		Unroll(TestVectors[i*3+1],clear);
		Unroll(TestVectors[i*3+2],enc);

		SCBlowfish bl(sizeof(key),key);

		scratch[0] = clear[0];
		scratch[1] = clear[1];
		bl.EncryptBlock(scratch);
		XCTAssert(scratch[0] == enc[0]);
		XCTAssert(scratch[1] == enc[1]);

		bl.DecryptBlock(scratch);
		XCTAssert(scratch[0] == clear[0]);
		XCTAssert(scratch[1] == clear[1]);
	}
}


@end
