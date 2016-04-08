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

package com.chaosinmotion.securechat;

import com.chaosinmotion.securechat.rsa.SCRSAGenerator;
import com.chaosinmotion.securechat.rsa.SCRSAKey;

import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;

import static org.junit.Assert.assertTrue;

/**
 * Created by woody on 4/8/16.
 */
public class SecureChatUnitTest
{

	@Test
	public void testRSAGenerator()
	{
		SecureRandom random = new SecureRandom();
		SCRSAKey pub;
		SCRSAKey priv;

		// Hard core test: generate a 4096 public/private key, then verify that
		// we can encrypt and decrypt.
		SCRSAGenerator.Pair pair = SCRSAGenerator.generateKeyPair(1024);
		pub = pair.getPublicKey();
		priv = pair.getPrivateKey();

		BigInteger tmp = new BigInteger(1022,random);	// slightly smaller
		BigInteger enc = tmp.modPow(pub.getExponent(), pub.getModulus());
		BigInteger dec = enc.modPow(priv.getExponent(), priv.getModulus());

		assertTrue(tmp.equals(dec));
	}
}
