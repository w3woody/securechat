//
//  SCRSAEncoder.mm
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

#import "SCRSAEncoder.h"
#include "SCRSAEncryption.h"
#include <stdlib.h>

/************************************************************************/
/*																		*/
/*	Internals															*/
/*																		*/
/************************************************************************/

@interface SCRSAEncoder ()
@property (assign) SCRSAKey *publicRSAKey;
@end


/************************************************************************/
/*																		*/
/*	RSA Encryption Wrapper												*/
/*																		*/
/************************************************************************/

@implementation SCRSAEncoder

- (id)initWithEncoderKey:(NSString *)publicKey;
{
	if (nil != (self = [super init])) {
		const char *str = [publicKey cStringUsingEncoding:NSUTF8StringEncoding];
		self.publicRSAKey = new SCRSAKey(str);
	}
	return self;
}

- (void)dealloc
{
	delete self.publicRSAKey;
}

- (NSData *)encodeData:(NSData *)data
{
	/*
	 *	Perform encryption process. Ths runs the message through the
	 *	padding class, then through the RSA class
	 *
	 *	Note that we encode the complete message with a header
	 *	which gives the size of the data block in bytes. The
	 *	header is encoded in big endian format.
	 */

	NSMutableData *ret = [[NSMutableData alloc] init];

	SCRSAPadding padding(self.publicRSAKey->Size());
	size_t msgSize = padding.GetMessageSize();
	size_t encSize = padding.GetEncodeSize() / sizeof(BIWORD);	// in 32-bit words

	uint8_t *msgBuffer = (uint8_t *)malloc(msgSize);
	BIWORD *encBuffer = (BIWORD *)malloc(padding.GetEncodeSize());

	uint8_t *dataPtr = (uint8_t *)[data bytes];
	uint32_t len = (uint32_t)data.length;
	uint32_t msgPos = 0;		// pos in msgBuffer
	uint32_t dataPos = 0;		// pos in data as we unravel

	/*
	 *	Prepend 4 bytes for len. Assumes message size is bigger than
	 *	4 bytes
	 */

	msgBuffer[msgPos++] = (uint8_t)(len >> 24);
	msgBuffer[msgPos++] = (uint8_t)(len >> 16);
	msgBuffer[msgPos++] = (uint8_t)(len >> 8);
	msgBuffer[msgPos++] = (uint8_t)(len);

	/*
	 *	Now run the rest of the data through
	 */

	while (dataPos < len) {
		msgBuffer[msgPos++] = dataPtr[dataPos++];
		if (msgPos >= padding.GetMessageSize()) {
			msgPos = 0;

			/*
			 *	Encode the buffer and encrypt it
			 */

			padding.Encode(msgBuffer, (uint8_t *)encBuffer);

			// Spin the bits in each word from network order.
			// (I.E.: we have a stream of bytes but we want them
			// flipped to deal with the host order for long words)
			for (uint32_t i = 0; i < encSize; ++i) {
#ifdef BIUSE32BIT
				encBuffer[i] = ntohl(encBuffer[i]);
#else
				encBuffer[i] = ntohs(encBuffer[i]);
#endif
			}

			// Spin the bytes. That's because our padding puts the MSB
			// at word 0, and our SCBigInteger implementation wants the
			// MSB at word n-1
			for (size_t i = 0, j = encSize-1; i < j; ++i, --j) {
				BIWORD tmp = encBuffer[i];
				encBuffer[i] = encBuffer[j];
				encBuffer[j] = tmp;
			}

			// Now convert to an integer, and transform via RSA
			SCBigInteger bi(encBuffer,(size_t)encSize);
			SCBigInteger ei = self.publicRSAKey->Transform(bi);
			memcpy(encBuffer,ei.GetData(),encSize * sizeof(BIWORD));

			// Spin the bytes
			for (size_t i = 0, j = encSize-1; i < j; ++i, --j) {
				BIWORD tmp = encBuffer[i];
				encBuffer[i] = encBuffer[j];
				encBuffer[j] = tmp;
			}

			// Spin the bits back to network order and write the data
			for (uint32_t i = 0; i < encSize; ++i) {
#ifdef BIUSE32BIT
				encBuffer[i] = htonl(encBuffer[i]);
#else
				encBuffer[i] = htons(encBuffer[i]);
#endif
			}
			[ret appendBytes:encBuffer length:encSize * sizeof(BIWORD)];
		}
	}

	if (msgPos > 0) {
		// zero tail of buffer
		memset(msgBuffer + msgPos, 0, msgSize - msgPos);

		/*
		 *	Encode the buffer and encrypt it
		 */

		padding.Encode(msgBuffer, (uint8_t *)encBuffer);

		// Spin the bits in each word from network order.
		// (I.E.: we have a stream of bytes but we want them
		// flipped to deal with the host order for long words)
		for (uint32_t i = 0; i < encSize; ++i) {
#ifdef BIUSE32BIT
			encBuffer[i] = ntohl(encBuffer[i]);
#else
			encBuffer[i] = ntohs(encBuffer[i]);
#endif
		}

		// Spin the bytes. That's because our padding puts the MSB
		// at word 0, and our SCBigInteger implementation wants the
		// MSB at word n-1
		for (size_t i = 0, j = encSize-1; i < j; ++i, --j) {
			BIWORD tmp = encBuffer[i];
			encBuffer[i] = encBuffer[j];
			encBuffer[j] = tmp;
		}

		// Now convert to an integer, and transform via RSA

		SCBigInteger bi(encBuffer,(size_t)encSize);
		SCBigInteger ei = self.publicRSAKey->Transform(bi);
		memcpy(encBuffer,ei.GetData(),encSize * sizeof(BIWORD));

		// Spin the bytes
		for (size_t i = 0, j = encSize-1; i < j; ++i, --j) {
			BIWORD tmp = encBuffer[i];
			encBuffer[i] = encBuffer[j];
			encBuffer[j] = tmp;
		}

		// Spin the bits back to network order and write the data
		for (uint32_t i = 0; i < encSize; ++i) {
#ifdef BIUSE32BIT
			encBuffer[i] = htonl(encBuffer[i]);
#else
			encBuffer[i] = htons(encBuffer[i]);
#endif
		}
		[ret appendBytes:encBuffer length:encSize * sizeof(BIWORD)];
	}

	/*
	 *	Completed encryption process. Free scratch buffers and return
	 *	our built data
	 */

	free(msgBuffer);
	free(encBuffer);

	return ret;
}


@end
