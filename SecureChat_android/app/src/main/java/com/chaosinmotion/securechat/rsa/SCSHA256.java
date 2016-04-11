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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Common SHA-256 code.
 * Created by woody on 4/11/16.
 */
public class SCSHA256
{
	public static String sha256(byte[] data)
	{
		try {
			MessageDigest d = MessageDigest.getInstance("SHA-256");
			StringBuffer buf = new StringBuffer();
			for (byte b: d.digest(data)) {
				buf.append(String.format("%02x", 0xFF & b));
			}
			return buf.toString();
		}
		catch (NoSuchAlgorithmException e) {
			return "";  // never happens
		}
	}

	public static String sha256String(String str)
	{
		try {
			return sha256(str.getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException e) {
			return "";      // should never happen
		}
	}
}
