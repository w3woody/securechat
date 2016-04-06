//
//  SCSecureHash.cpp
//  SecureChat
//
//  Created by William Woody on 2/24/16.
//  Copyright © 2016 by William Edward Woody.
//
//	Adopted from https://en.wikipedia.org/wiki/SHA-2
//  and
//	http://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.180-4.pdf
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

#include <string.h>
#include "SCSecureHash.h"

/************************************************************************/
/*																		*/
/*	Construction/Destruction											*/
/*																		*/
/************************************************************************/

/*	SCSHA256Context::SCSHA256Context
 *
 *		Construction
 */

SCSHA256Context::SCSHA256Context()
{
	Zero();
	Start();
}

/*	SCSHA256Context::~SCSHA256Context
 *
 *		Destruction. Zero state
 */

SCSHA256Context::~SCSHA256Context()
{
	Zero();
}

/*	SCSHA256Context::Zero
 *
 *		Zero the contents
 */

void SCSHA256Context::Zero()
{
	total = 0;
	index = 0;
	memset(state,0,sizeof(state));
	memset(buffer,0,sizeof(buffer));
}

/************************************************************************/
/*																		*/
/*	Internal															*/
/*																		*/
/************************************************************************/

/*
 *	K
 */

static const uint32_t KVal[] = {
	0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
	0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
	0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
	0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
	0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
	0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
	0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
	0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
	0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
	0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
	0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
	0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
	0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
	0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
	0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
	0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
};

/*
 *	Rotate/shfit primitives
 */

static inline uint32_t SHR(uint32_t x, int n)
{
	return x >> n;
}

static inline uint32_t RTR(uint32_t x, int n)
{
	return SHR(x,n) | (uint32_t)(x << (32 - n));
}

/*
 *	4.1.2: SHA_256 functions
 */

static inline uint32_t sum0(uint32_t x)
{
	return RTR(x, 2) ^ RTR(x, 13) ^ RTR(x, 22);
}

static inline uint32_t sum1(uint32_t x)
{
	return RTR(x, 6) ^ RTR(x, 11) ^ RTR(x, 25);
}

static inline uint32_t sigma0(uint32_t x)
{
	return RTR(x, 7) ^ RTR(x, 18) ^ SHR(x, 3);
}

static inline uint32_t sigma1(uint32_t x)
{
	return RTR(x, 17) ^ RTR(x, 19) ^ SHR(x, 10);
}

static inline uint32_t ch(uint32_t x, uint32_t y, uint32_t z)
{
	return (x & y) ^ (~x & z);
}

static inline uint32_t maj(uint32_t x, uint32_t y, uint32_t z)
{
	return (x & y) ^ (x & z) ^ (y & z);
}


/************************************************************************/
/*																		*/
/*	SHA256 Algorithm													*/
/*																		*/
/************************************************************************/

/*	SHSHA256Context::Start
 *
 *		Zero and start.
 */

void SCSHA256Context::Start()
{
	memset(buffer,0,sizeof(buffer));

    total = 0;			// total bytes passed in, for append length
	index = 0;

	// start state vector for SHA-256
	state[0] = 0x6A09E667;
	state[1] = 0xBB67AE85;
	state[2] = 0x3C6EF372;
	state[3] = 0xA54FF53A;
	state[4] = 0x510E527F;
	state[5] = 0x9B05688C;
	state[6] = 0x1F83D9AB;
	state[7] = 0x5BE0CD19;
}

/*	SCSHA256Context::ProcessBuffer
 *
 *		Given the buffer contents (in the buffer object), run the inner
 *	loop of the SHA-256 calculation
 */

void SCSHA256Context::ProcessBuffer()
{
	/*
	 *	Step 1: unroll buffer into array, and initialize w
	 *
	 *	(Algorithm 6.2.2)
	 */

	uint32_t w[64];
	for (int i = 0; i < 16; ++i) {
		uint32_t tmp = buffer[i*4];
		tmp = (tmp << 8) | buffer[i*4+1];
		tmp = (tmp << 8) | buffer[i*4+2];
		tmp = (tmp << 8) | buffer[i*4+3];
		w[i] = tmp;
	}
	for (int i = 16; i < 64; ++i) {
		w[i] = sigma1(w[i-2]) + w[i-7] + sigma0(w[i-15]) + w[i-16];
	}

	// step 2
	uint32_t a = state[0];
	uint32_t b = state[1];
	uint32_t c = state[2];
	uint32_t d = state[3];
	uint32_t e = state[4];
	uint32_t f = state[5];
	uint32_t g = state[6];
	uint32_t h = state[7];

	for (int i = 0; i < 64; ++i) {
		uint32_t t1 = h + sum1(e) + ch(e,f,g) + KVal[i] + w[i];
		uint32_t t2 = sum0(a) + maj(a, b, c);
		h = g;
		g = f;
		f = e;
		e = d + t1;
		d = c;
		c = b;
		b = a;
		a = t1 + t2;
	}

	state[0] += a;
	state[1] += b;
	state[2] += c;
	state[3] += d;
	state[4] += e;
	state[5] += f;
	state[6] += g;
	state[7] += h;
}

/*	SCSHA256Context::Update
 *
 *		Update--append message
 */

void SCSHA256Context::Update(size_t len, const uint8_t *data)
{
	size_t wlen;

	total += len;

	/*
	 *	Pull the chunks across, encoding each type
	 */

	while (len > 0) {
		wlen = len;
		if (wlen > 64 - index) wlen = 64 - index;
		memmove(buffer + index, data, wlen);
		index += wlen;
		
		if (index >= 64) {
			ProcessBuffer();
			index = 0;
		}

		len -= wlen;
		data += wlen;
	}
}

/*	SCSHA256Context::Finish
 *
 *		Finish processing, return result.
 */

void SCSHA256Context::Finish(uint8_t output[32])
{
	uint8_t tail = 0x80;

	/*
	 *	Step 1: append 0x80 to string
	 */

	uint64_t curLen = total;
	Update(1, &tail);

	/*
	 *	Step 2: append size in bits
	 */

	curLen *= 8;		// 8 bits per byte
	if (index > 56) {	// If not fits, zero and add next block
		memset(buffer + index, 0, 64 - index);
		ProcessBuffer();
		index = 0;
	}
	memset(buffer + index, 0, 56 - index);
	for (int i = 0; i < 8; ++i) {
		buffer[63-i] = (uint8_t)curLen;
		curLen >>= 8;
	}
	ProcessBuffer();

	/*
	 *	Convert result state into output
	 */

	for (int i = 0; i < 8; ++i) {
		uint32_t tmp = state[i];
		output[i*4+3] = (uint8_t)tmp;
		tmp >>= 8;
		output[i*4+2] = (uint8_t)tmp;
		tmp >>= 8;
		output[i*4+1] = (uint8_t)tmp;
		tmp >>= 8;
		output[i*4] = (uint8_t)tmp;
	}
}

