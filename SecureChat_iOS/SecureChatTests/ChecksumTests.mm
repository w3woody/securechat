//
//  ChecksumTests.m
//  SecureChat
//
//  Created by William Woody on 2/24/16.
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
#include "SCChecksum.h"
#include "SCSecureHash.h"
#include "SCRSAEncryption.h"

@interface ChecksumTests : XCTestCase

@end

@implementation ChecksumTests

- (void)setUp {
    [super setUp];
    // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown {
    // Put teardown code here. This method is called after the invocation of each test method in the class.
    [super tearDown];
}

- (void)testCRC8Vectors
{
	// CRC32 check for test vectors from RFC 3720
	uint8_t bytes[32];

	memset(bytes,0,sizeof(bytes));
	XCTAssert(0x00 == SCCalcCRC8(0,bytes,sizeof(bytes)));

	for (int i = 0; i < 32; ++i) bytes[i] = i;
	XCTAssert(0x06 == SCCalcCRC8(0,bytes,sizeof(bytes)));

	for (int i = 0; i < 32; ++i) bytes[i] = (31-i);
	XCTAssert(0xf6 == SCCalcCRC8(0,bytes,sizeof(bytes)));

	for (int i = 0; i < 32; ++i) bytes[i] = 0xFF;
	XCTAssert(0x09 == SCCalcCRC8(0,bytes,sizeof(bytes)));
}

static uint8_t ByteFromChar(char c)
{
	if (('0' <= c) && (c <= '9')) return c - '0';
	if (('a' <= c) && (c <= 'f')) return c - 'a' + 10;
	if (('A' <= c) && (c <= 'F')) return c - 'A' + 10;
	return 0;
}

static uint8_t ByteFromString(const char *c)
{
	return (ByteFromChar(*c) << 4) | ByteFromChar(c[1]);
}

- (void)testSHA256String
{
	NSString *tmp = @"6aae34c6b9610bfa7db3a43494172267d1bcd375d66f50146ca97b5254486a1fPEnSalt1946d96efd2-2aa9-486a-98b5-308ded9a3d91";

	const char *utf = [tmp UTF8String];

	SCSHA256Context ctx;
	ctx.Start();
	ctx.Update(strlen(utf), (const uint8_t *)utf);

	uint8_t output[32];
	ctx.Finish(output);

	// Convert to string
	char buffer[80];
	for (int i = 0; i < 32; ++i) {
		sprintf(buffer + i*2,"%02x",output[i]);
	}
	NSString *res = [NSString stringWithUTF8String:buffer];

	XCTAssert([res isEqualToString:@"efe2c2129b00d93b37d3be981b0898554c91068c173cda47d6a0660de6df221a"]);

}

- (void)testSHA256
{
	const char *testVectors[] = {
		/* SHA-256 test vectors */
		"", "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855",
		"8E", "949F94D858EF6AD1333164D796A0D777FD82F9155ECE7D6FAD68C0B992F0E7AF",
		"61", "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb",
		"5D44880EFE95A5720919FA06495A31AB", "018DB1FFF00FDB75DD886B7881B3FB12CB210B7D0C33F7B2A7B56D047980BA56",
		"8DD26F25AA71F1B1F490F7A42DF4FBB8EE0F0BCDF344E3D6D9BA8AF239492B85", "2209F4508F7501DE5890BAE34922056598F1B7132EE255937CF6F8F976C374C0",
		"8D982C113BC8AD0ED23242BC00B724C1923A62B8F20B83C540F4DE6843B5989794F69240FBA2F72D5172D0EA3091A317", "CF66AA1557FE58474E82148EEB6C0373AC28017406297C6E30C289DE6EB1112B",
		"99DCC037765659A6E21D36210B64A52B5478C24A7770EDD268D75FBB1B4DF90353E1EEA5D9056DFB7B93B85EC20FB957A9AF3CADED861C0635597BED", "6B7525002499E4C52EF81FDC828C11ED9FE2EE1E80CC0F8C6C627900496A31F4",
		"2F630EF1AEABD28EC35C79C1C01006B82CAEC0C9096E12A15E8A429429DA9C69CE0341EAB8F4C3E3AF992471D29150BCC98F78AF1FD3A9A86D3462C19E1AD374", "1881AD9EE06293B77D2F9AFD1F971F7CB2FA318070CFEC1E37DFFC6AD5F42EF5",
		"C1FB6DB1DE67076DBC9CE8B73F5CD32027609EC686C572F1AA0A04DE0D9EE580CFAE91F49E41DCA6C600B9994466B108E0A491252432885FAD20D7AFCAF59E54C8", "5E4F5AC70FD832EDB6AE5451596F1C88F964F9A7DD55310641BE6ECB9EC17E1F",
		"AA4382BE30C522188C8E3B003DEFEB50491BF249F0B9A5582A04F34403E58ADAEE234B8070F8F80B94138D7B3B1CE967BEAE7058B1ECE557AD6395C340416D9BC637B60A1376BC363B73BB9A967068FA7464A2955064158B68A0E2868231A0710DD1C397FEB2CDCEAF756B511E9450733C28C304B8A3152B", "C42935B15F4D40DD63C4823DF0090DEC12CB0373AFC17B8766D9F877E63F0050",
		"FB9E8396D13D500855F5EA822D84E6B2A9A590F07AC5E8CB84E99849500C15AD3C75BB6F63A93D3B0E984865EDF37CA1FE0D5510B071CC0AE619452C4D20207311B9AEBCB9EEB509A7C5849DFE227587B74039BCD78A70D50AA8621FC650386034D212C21F25B020A10F1A041C7319DE09E65DE98FAD31935C8861AF0F986E8A048D67382170624BA2FE4A80D32B1B5F3D4AF7E2CAA9A142EFC874D3E2C4EAFC55D4A96D9927D24BE1E6ED90BD99A7E6780507C76B246CB4D599797B97F2ACC1EE1CA51AE81CECDD20A031FC3B477A44CAE12CA5580AE6C0A9360D836CAF54F1A30DB439C9D0F30EBC12F455D4B5FCD0", "AFB589CC58C64D23A1EF735556A3DAC30A086FBF446EEEF55E0C08E3051F1D85",
		"1C2ACE388E03923A936C93B435C23FAA9E15FB0868FBE7A054E54FC39E1BA6BD5EAC3966E67C484129132F6E8E302581784EB2FF3EC49DBFB2F67902996927E75F84627D5F3EE7564DDDA6A503E3B83B7A9C5FC3B81860051E6E35B6E287F9351B861AD91D58ABE6934E2DD4A84703E37E2046D86DD2A6029772CCFFADFF35F427D7571FEB9BA46A3903E6F64B27A4D6C745DB891E4977F327E95101E160C6C2DD49C682F0497665DBB03E8467D650A5FFFF0701935B2117AF843D11A3B7DF7430B11D1E9BB0C225BE2B68415C64CCAB19431BED952B76FBF5348FBB82EA6F27FBFF33D2A2E1C990481C3BF02B0A8E1ABDA367C93D7340535FF069B35DABCBA1C3F825D95ECD7E0254AEA4DD705A6056E50CE5628ECD7325C6DF20CEC1AADF86E2AB152D392BE1F5256AECF3C5421E2D79C319F5659A118138ACAAD32DCCA23D2C6B5D15F8FD9FFA5F004424ED34A45ED60BF66994A400BF79CC26BE38053C443B0E3858714C10094ED4ABD722A617B0B6076B73110DF89AA70C7AF55D7D31DB0FA07602059C13363D42C6D0332CC5FD9E0B593E31D4061AD3E96833EF7AA52F7015CB225F89314F482153A733F8C0C3C77F713CB63C8C34097F19D528A3677115C48655479639932334D18B71E599BBDE0FCCA970059FCF762658180F0CC626641B1CAA0A4863EC08AAE9E3D1A3CE4B746CE9F5700143255FE64C3AADA8", "6587A388C3E5F53E850CAE1DC40EBA632EF9C016695D98204B02679A896B9E71",
	};

	uint8_t data[1024];
	size_t len;
	uint8_t hash[32];
	size_t clen;
	uint8_t comp[32];

	for (int i = 0; i < 11; ++i) {
		const char *invector = testVectors[i*2];
		const char *outvector = testVectors[i*2+1];

		const char *ptr;
		len = 0;
		for (ptr = invector; *ptr; ptr += 2) {
			XCTAssert(len < 1024);					// verify vectors fit
			uint8_t byte = ByteFromString(ptr);
			data[len++] = byte;
		}

		clen = 0;
		for (ptr = outvector; *ptr; ptr += 2) {
			XCTAssert(clen < 32);					// verify vectors correct
			uint8_t byte = ByteFromString(ptr);
			comp[clen++] = byte;
		}
		XCTAssert(clen == 32);

		SCSHA256Context sha256;
		sha256.Start();
		sha256.Update(len, data);
		sha256.Finish(hash);

		XCTAssert(memcmp(comp, hash, 32) == 0);
	}
}

- (void)testPadding
{
	SCRSAPadding padding(256);			// 256 bits == 32 bytes, r = 3 bytes

	XCTAssert(padding.GetEncodeSize() == 32);
	XCTAssert(padding.GetMessageSize() == 28);

	uint8_t enc[32];
	uint8_t msg[28];
	uint8_t out[28];

	memset(msg, 0, sizeof(msg));
	padding.Encode(msg, enc);
	padding.Decode(enc, out);
	XCTAssert(0 == memcmp(out, msg, sizeof(msg)));

	for (int i = 0; i < 28; ++i) msg[i] = i;
	padding.Encode(msg, enc);
	padding.Decode(enc, out);
	XCTAssert(0 == memcmp(out, msg, sizeof(msg)));

	// Large buffer
	SCRSAPadding padding2(4096);		// 256 bits == 32 bytes, r = 3 bytes

	XCTAssert(padding2.GetEncodeSize() == 512);
	XCTAssert(padding2.GetMessageSize() == 448);

	uint8_t enc2[512];
	uint8_t msg2[448];
	uint8_t out2[448];

	memset(msg2, 0, sizeof(msg2));
	padding2.Encode(msg2, enc2);
	padding2.Decode(enc2, out2);
	XCTAssert(0 == memcmp(out2, msg2, sizeof(msg2)));

	for (int i = 0; i < 448; ++i) msg2[i] = i;
	padding2.Encode(msg2, enc2);
	padding2.Decode(enc2, out2);
	XCTAssert(0 == memcmp(out2, msg2, sizeof(msg2)));
}


//- (void)testPerformanceExample {
//    // This is an example of a performance test case.
//    [self measureBlock:^{
//        // Put the code you want to measure the time of here.
//    }];
//}

@end
