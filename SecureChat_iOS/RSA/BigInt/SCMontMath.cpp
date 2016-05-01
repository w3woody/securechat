//
//  SCMontMath.cpp
//  SecureChat
//
//  Created by William Woody on 2/23/16.
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
#import "SCBigInteger.h"

/************************************************************************/
/*																		*/
/*	Montgomery exponentiation support									*/
/*																		*/
/************************************************************************/

/*	SCMontMath::MontInit
 *
 *		Internal init of intermediate values
 */

void SCMontMath::MontInit()
{
	if (mrinit) return;
	mrinit = true;

	/*
	 *	All this assumes m is odd, which is true for RSA keys. This
	 *	calculates the value - (m-1) % b using newton iteration
	 *
	 *	Note: I'm not sure how this works. But I know BIWORD is either
	 *	uint16_t or uint32_t, and 2**16 evenly divides 2**32.
	 */

	uint32_t v = m.dataArray[0];
	uint32_t t = v;
	t *= 2 - v*t;
	t *= 2 - v*t;
	t *= 2 - v*t;
	t *= 2 - v*t;
	minv = (BIWORD)(-t);		// All mod 32, so we don't care unsigned

	/*
	 *	Now calculate rmod (R % mod) and r2mod ((R*R) % mod). These are
	 *	done once, so we don't sweat the inefficiency
	 */

	SCBigInteger r;
	r.Realloc(m.dataSize + 1);
	memset(r.dataArray,0,m.dataSize * sizeof(BIWORD));
	r.dataArray[m.dataSize] = 1;
	r.dataSize = m.dataSize + 1;

	rmod = r % m;
	r2mod = (r * r) % m;
}

/*	SCMontMath::MontMult
 *
 *		Algorithm 14.36, assumes everything already initialized. Note that
 *	we're making use of internal structures in SCBigInteger.
 */

SCBigInteger SCMontMath::MontMult(const SCBigInteger &x, const SCBigInteger &y)
{
	/* 1 */
	SCBigInteger a = 0;

	/* 2 */
	for (size_t i = 0; i < m.dataSize; i++) {
		/* 2.1: calculate ui */
		BIWORD ui = 0;
		BIWORD xi = 0;

		/*
		 *	Note that b = 2**32, so all math we perform in uint32_t will be
		 *	implicitly modulus b.
		 */

		if (a.dataSize) {
			ui = a.dataArray[0];		// a0, if exists
		}
		if (i < x.dataSize) {
			// xi non-zero
			xi = x.dataArray[i];
			ui += xi * y.dataArray[0];
		}
		ui *= minv;

		/* 2.2: A <- (A + xi * y + ui * m) / b */
		a.MulAdd(y,xi);			/* A += xi * y */
		a.MulAdd(m,ui);			/* A += ui * m */
		a.RightShiftWord();
	}

	/* 3 */
	if (a >= m) {
		a = a - m;
	}

	/* 4 */
	return a;
}

/*	SCMontMath::ExpMod
 *
 *		Performs v.ModPow(e,m), but using Montgomery exponeniation,
 *	algorithm 14.94, for efficiency
 */

SCBigInteger SCMontMath::ExpMod(const SCBigInteger &val, const SCBigInteger &e)
{
	/*
	 *	Internal initialize if needed
	 */

	MontInit();

	SCBigInteger v = val;
	if (v >= m) {
		// % is expensive, only do if needed
		v = v % m;
	}

	/*
	 *	Now initialize and run our iteration
	 */

	// 1
	SCBigInteger a = rmod;
	SCBigInteger x = MontMult(v, r2mod);

	// 2
//	size_t nbits = e.GetBitLength();
//	for (int32_t i = (int32_t)nbits; i >= 0; --i) { // Need signed number; int32_t should be big enough
//		// 2.1
//		a = MontMult(a, a);
//
//		// 2.2
//		if (e.BitTest(i)) {
//			a = MontMult(a, x);
//		}
//	}

	size_t nbits = e.GetBitLength();
	for (size_t i = 0; i < nbits; ++i) { // Need signed number; int32_t should be big enough
		if (e.BitTest(i)) {
			a = MontMult(a,x);
		}
		x = MontMult(x,x);
	}
	return MontMult(a, SCBigInteger(1));
}

