//
//  SCRSAEncryption.cpp
//  SecureChat
//
//  Created by William Woody on 2/21/16.
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

#include "SCRSAEncryption.h"

/************************************************************************/
/*																		*/
/*	Key Generator														*/
/*																		*/
/************************************************************************/

/*	SCRSAKeyGeneratePair
 *
 *		Generate a public/private key pair using the RSA agorithm
 *	sketched in section 19.3 of "Advanced Cryptography".
 *
 *		We take advantage of the fact that intermediate results are
 *	zeroed out as values are passed around. Thus, at the end of this
 *	operation, all of the intermediate big integer values have been
 *	zeroed out during the calculation process--and the only chunk of
 *	memory which stores the actual public and private key are the
 *	parameters passed into the generator
 */

void SCRSAKeyGeneratePair(uint32_t nbits, SCRSAKey &pub, SCRSAKey &priv)
{
	SCBigInteger one = 1;
	SCBigInteger e;
	SCBigInteger d;
	SCBigInteger n;

	bool success;

	/*
	 *	Note: in the highly unlikely the two values p and q are not properly
	 *	prime, then our encoding and decoding process will fail. So once we
	 *	have generated probable public and private keys, we run through a
	 *	few values to make sure things work correctly
	 */

	do {
		// Generate primes
		SCBigInteger p = SCBigInteger::ProbablePrime(nbits/2);
		SCBigInteger q = SCBigInteger::ProbablePrime(nbits/2);

		n = p * q;

		// phi(n)
		SCBigInteger phiN = (p - one) * (q - one);

		// Find e with gcd(e, phiN) == 1
		do {
			e = SCBigInteger::Random(nbits);
		} while ((e.GCD(phiN) != one) || (e >= phiN));

		// Find d
		d = e.ModInverse(phiN);

		/*
		 *	So we randomly draw 5 values and run through through the
		 *	basic RSA math. If this fails, this implies that p and q
		 *	were not properly picked, so we repeat.
		 *
		 *	Normally this should never happen in practice. But it's always
		 *	good to make sure.
		 *
		 *	(This issue surfaced when my implementation of the Rabin-Miller
		 *	prime test algorithm was faulty, and was causing non-prime values
		 *	to be returned by ProbablePrime(). I don't understand the theory
		 *	well enough to understand why; all I know is if we cannot get
		 *	past the test below, something was screwed up. And because we
		 *	generate RSA keys so seldomly, it's okay if this takes a couple
		 *	of seconds to complete.)
		 */

		int i;
		SCMontMath mm = n;
		for (i = 0; i < 5; ++i) {
			SCBigInteger test = SCBigInteger::Random(nbits-2);
			SCBigInteger enc = mm.ExpMod(test, d); //test.ModPow(d, n);
			SCBigInteger dec = mm.ExpMod(enc, e); //enc.ModPow(e, n);
			if (test != dec) break;
		}
		success = i >= 5;
	} while (!success);

	// Copy the resulting keys e,n and d,n
	pub = SCRSAKey(nbits,e,n);
	priv = SCRSAKey(nbits,d,n);
}
