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

import com.chaosinmotion.securechat.rsa.SCChecksum;
import com.chaosinmotion.securechat.rsa.SCRSAPadding;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by woody on 4/7/16.
 */
public class ChecksumUnitTest
{
	@Test
	public void testCRC8Vectors()
	{
		byte[] bytes = new byte[32];

		for (int i = 0; i < 32; ++i) bytes[i] = 0;
		assertTrue(0x00 == SCChecksum.get().calcCRC8((byte)0,bytes));

		for (int i = 0; i < 32; ++i) bytes[i] = (byte)i;
		assertTrue(0x06 == SCChecksum.get().calcCRC8((byte)0,bytes));

		for (int i = 0; i < 32; ++i) bytes[i] = (byte)(31-i);
		assertTrue(0xf6 == (0x0FF & SCChecksum.get().calcCRC8((byte)0,bytes)));

		for (int i = 0; i < 32; ++i) bytes[i] = (byte)0xFF;
		assertTrue(0x09 == SCChecksum.get().calcCRC8((byte)0,bytes));
	}

	@Test
	public void testPadding()
	{
		SCRSAPadding padding = new SCRSAPadding(256);			// 256 bits == 32 bytes, r = 3 bytes

		assertTrue(padding.getEncodeSize() == 32);
		assertTrue(padding.getMessageSize() == 28);

		byte[] enc = new byte[32];
		byte[] msg = new byte[28];
		byte[] out = new byte[28];

		Arrays.fill(enc, (byte)0);
		padding.encode(msg, enc);
		padding.decode(enc, out);
		assertTrue(Arrays.equals(out, msg));

		for (int i = 0; i < 28; ++i) msg[i] = (byte)i;
		padding.encode(msg, enc);
		padding.decode(enc, out);
		assertTrue(Arrays.equals(out, msg));

		// Large buffer
		SCRSAPadding padding2 = new SCRSAPadding(4096);		// 256 bits == 32 bytes, r = 3 bytes

		assertTrue(padding2.getEncodeSize() == 512);
		assertTrue(padding2.getMessageSize() == 448);

		byte[] enc2 = new byte[512];
		byte[] msg2 = new byte[448];
		byte[] out2 = new byte[448];

		Arrays.fill(msg2, (byte)0);
		padding2.encode(msg2, enc2);
		padding2.decode(enc2, out2);
		assertTrue(Arrays.equals(out2, msg2));

		for (int i = 0; i < 448; ++i) msg2[i] = (byte)i;
		padding2.encode(msg2, enc2);
		padding2.decode(enc2, out2);
		assertTrue(Arrays.equals(out2, msg2));
	}
}
