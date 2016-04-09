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

import android.test.AndroidTestCase;

import com.chaosinmotion.securechat.rsa.SCRSAEncoder;
import com.chaosinmotion.securechat.rsa.SCRSAManager;

import java.util.Arrays;

/**
 * Created by woody on 4/9/16.
 */
public class ManagerUnitTest extends AndroidTestCase
{
	public void testRSAManager()
	{
		try {
			// clear for test
			SCRSAManager.shared().clear(getContext());

			assertTrue(SCRSAManager.shared().setPasscode("1234", getContext()));
			assertTrue(SCRSAManager.shared().generateRSAKeyWithSize(1024));
			SCRSAManager.shared().encodeSecureData(getContext());

			// Now test password checking
			assertTrue(!SCRSAManager.shared().setPasscode("1244", getContext()));
			assertTrue(SCRSAManager.shared().setPasscode("1234", getContext()));

			// Create an encoder and encode a block of data
			byte[] data = "Hello World.".getBytes("UTF-8");
			SCRSAEncoder encoder = new SCRSAEncoder(SCRSAManager.shared().getPublicKey());
			byte[] enc = encoder.encodeData(data);

			byte[] dec = SCRSAManager.shared().decodeData(enc);
			assertTrue(Arrays.equals(data, dec));
		}
		catch (Exception ex) {
			ex.printStackTrace();
			fail();
		}
	}
}
