//
//  SCRSAPadding.cpp
//  SecureChat
//
//  Created by William Woody on 2/25/16.
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

#include <stdio.h>
#include <string.h>
#include "SCRSAEncryption.h"
#include "SCChecksum.h"
#include "SCSecureHash.h"
#include <Security/Security.h>

/************************************************************************\

This implements a custom OAEP encoding scheme, based on the information
in https://en.wikipedia.org/wiki/Optimal_asymmetric_encryption_padding
and at other locations.

We make a couple of modifications from the existing OAEP scheme that is
documented here:

(1) Because we know that we will be encrypting using the RSA key scheme,
our padding is only 2 *bits*, not the 2 or more bytes that is described
in RFC 2437 and RFC 3447.

Our rational is twofold.

First, because we know we're using the RSA key scheme, the maximum number
of bytes that is guaranteed to be encoded is 2**(N-2), where N is the 
number of bits specified during creation of the RSA key. That's because
the two primes P and Q which make N are both selected to be >= 2**(N/2-1) but
less than 2**(N/2), and that implies P*Q >= 2**(N-2).

Second, it is the observation that the more zeros we insert into the
message, the less 'entropy' we're encoding. And the whole point of the
encryption process is to maximize entropy. So we create an encoding which
only sets the top 2 bits to 0, to fit within our 2**(N-2) limit.

(2) We pick the size of R in our encoding scheme to be N/8 - 10 bits in 
width, and we select our oracle G to simply repeat the bits in R. Our 
rational here is again, to maximize entropy by maximizing the randomness
in the system. By repeating bits we allow R to be rather large; in practice
for a RSA key of 1024 bits, we pick a random value of 118 bits in length.

The selection of N/8 is arbitrary, but lopping off 10 bits is to allow us
8 bits for a CRC32 checksum (below), and 2 bits of zeros to fit in the
RSA key size (discussion above).

(3) We also add a CRC32 checksum to the message, to make sure that the
decoding process yields a valid message.

(4) Our G and H functions are both SHA-256, with the bits either repeated
or lopped off to fit the word size required. By repeating bits we allow
the width of R to exceed the 256 bit limit set by SHA-256 hash key sizes.


Note that because we are not interested in fitting in any existing
standards but in using the underlying ideas behind those standards to
encode our message, the class below is not standard compliant.

\************************************************************************/


/************************************************************************/
/*																		*/
/*	Construction/Destruction											*/
/*																		*/
/************************************************************************/

/*	SCRSAPadding::SCRSAPadding
 *
 *		Construct encoding
 */

SCRSAPadding::SCRSAPadding(uint32_t n)
{
	encSize = n/8;
	msgSize = (7 * encSize) / 8;			// 1/8th size for R, checksum
}

/*	SCRSAPadding::Encode
 *
 *		Encode the message. This performs the OAEP operation as shown
 *	in the Wikipedia article with the modifications noted above
 */

bool SCRSAPadding::Encode(const uint8_t *msg, uint8_t *enc)
{
	SCSHA256Context ctx;
	size_t msgOffset = encSize - msgSize;
	uint8_t gMask[32];
	uint8_t hMask[32];
	size_t i,j;

	/*
	 *	Step 1: Move message to bottom bytes of enc, calculate 
	 *	checksum
	 */

	memmove(enc + msgOffset, msg, msgSize);

	/*
	 *	Step 2: Calculate a CRC checksum of the message
	 */

	enc[msgOffset-1] = SCCalcCRC8(0, msg, msgSize);

	/*
	 *	Step 3: Add random buffer
	 */

	SecRandomCopyBytes(kSecRandomDefault, msgOffset-1, enc);
	enc[0] &= 0x3F;				// zero out top two bits

	/*
	 *	Now calculate G(r)
	 */

	ctx.Start();
	ctx.Update(msgOffset-1, enc);
	ctx.Finish(gMask);

	/*
	 *	Calculate G ^ m. Note the CRC is taken as part of the message
	 */

	j = 0;
	for (i = msgOffset-1; i < encSize; ++i) {
		enc[i] ^= gMask[j++];
		if (j >= 32) j = 0;
	}

	/*
	 *	Calculate H(m'). Note the CRC is taken as part of the message
	 */

	ctx.Start();
	ctx.Update(msgSize+1, enc+msgOffset-1);
	ctx.Finish(hMask);

	/*
	 *	Calculate r ^ H. Force top two bits as zero
	 */

	j = 0;
	for (i = 0; i < msgOffset-1; ++i) {
		enc[i] ^= hMask[j++];
		if (j >= 32) j = 0;
	}
	enc[0] &= 0x3F;

	return true;
}

/*	SCRSAPadding::Decode
 *
 *		Decode the encoded packet
 */

bool SCRSAPadding::Decode(const uint8_t *enc, uint8_t *msg)
{
	SCSHA256Context ctx;
	size_t msgOffset = encSize - msgSize;
	uint8_t gMask[32];
	uint8_t hMask[32];
	uint8_t scratch[32];
	size_t i,j,k;
	uint8_t crc;

	/*
	 *	Calculate H(m'). Note the CRC is taken as part of the message
	 */

	ctx.Start();
	ctx.Update(msgSize+1, enc+msgOffset-1);
	ctx.Finish(hMask);

	/*
	 *	Calculate G(r). Note that we do this without using more
	 *	than a nominal scratch buffer in order to remain re-entrant
	 *	and without undue allocation of memory
	 */

	ctx.Start();
	j = 0;
	for (i = 0; i < msgOffset-1; ++i) {
		scratch[j] = hMask[j] ^ enc[i];
		++j;

		if (i == 0) scratch[i] &= 0x3F;			// top two bits force 0
		if (j >= 32) {
			ctx.Update(j, scratch);
			j = 0;
		}
	}
	if (j > 0) {
		ctx.Update(j, scratch);
	}
	ctx.Finish(gMask);

	/*
	 *	Calculate crc, message
	 */

	k = 0;
	j = 0;
	i = msgOffset - 1;
	crc = enc[i++] ^ gMask[j++];
	while (i < encSize) {
		msg[k++] = enc[i++] ^ gMask[j++];
		if (j >= 32) j = 0;
	}

	/*
	 *	Now validate the CRC checksum of the decoded message
	 */

	return SCCalcCRC8(0, msg, msgSize) == crc;
}

