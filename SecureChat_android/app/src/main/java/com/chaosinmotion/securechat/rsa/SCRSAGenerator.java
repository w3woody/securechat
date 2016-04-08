/*
 * Copyright (c) 2016. William Edward Woody
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>
 *
 */

package com.chaosinmotion.securechat.rsa;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Correlates to the SCRSAEncryption class on iOS. This contains the code
 * used to generate our own RSA key. We don't rely on any built-in security
 * classes on the assumption the built-in classes have been hacked to
 * provide weak keys.
 *
 * We do leverage BigInteger, because the Java implementation of BigInteger
 * is fantastic.
 *
 * Created by woody on 4/8/16.
 */
public class SCRSAGenerator
{
	/**
	 * Generates a key pair. If successful returns a 2 item array
	 * of keys; the first in the array is the public key, the second
	 * the private key
	 * @param nbits
	 * @return
	 */
	public static SCRSAKey[] generateKeyPair(int nbits)
	{
		SecureRandom random = new SecureRandom();

		BigInteger one = BigInteger.ONE;
		BigInteger e;
		BigInteger d;
		BigInteger n;

		boolean success;

		/*
		 *	Note: in the highly unlikely the two values p and q are not properly
		 *	prime, then our encoding and decoding process will fail. So once we
		 *	have generated probable public and private keys, we run through a
		 *	few values to make sure things work correctly
		 */

		do {
			// Generate primes
			BigInteger p = BigInteger.probablePrime(nbits/2,random);
			BigInteger q = BigInteger.probablePrime(nbits/2,random);

			n = p.multiply(q);

			// phi(n) = (p-1) * (q-1)
			BigInteger phiN = p.subtract(one).multiply(q.subtract(one));

			// Find e with gcd(e, phiN) == 1
			do {
				e = new BigInteger(nbits,random);
			} while ((!e.gcd(phiN).equals(one)) || (e.compareTo(phiN) >= 0));

			// Find d
			d = e.modInverse(phiN);

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
			for (i = 0; i < 5; ++i) {
				BigInteger test = new BigInteger(nbits-2,random);
				BigInteger enc = test.modPow(d, n);
				BigInteger dec = enc.modPow(e, n);
				if (!test.equals(dec)) break;
			}
			success = i >= 5;
		} while (!success);

		// Copy the resulting keys e,n and d,n
		SCRSAKey[] retval = new SCRSAKey[2];
		retval[0] = new SCRSAKey(nbits,e,n);
		retval[1] = new SCRSAKey(nbits,d,n);
		return retval;
	}
}
