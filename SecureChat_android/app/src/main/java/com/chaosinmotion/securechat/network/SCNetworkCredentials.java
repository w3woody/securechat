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

package com.chaosinmotion.securechat.network;

import java.security.MessageDigest;

/**
 * Created by woody on 4/9/16.
 */
public class SCNetworkCredentials
{
	private String username;
	private String password;

	private static String sha256(String str)
	{
		try {
			MessageDigest d = MessageDigest.getInstance("SHA-256");
			StringBuffer buf = new StringBuffer();
			for (byte b: d.digest(str.getBytes("UTF-8"))) {
				buf.append(String.format("%02x", 0xFF & b));
			}
			return buf.toString();
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";  // never happens
		}
	}

	public SCNetworkCredentials()
	{
		username = null;
		password = null;
	}

	public SCNetworkCredentials(String u)
	{
		username = u;
		password = null;
	}

	/**
	 * Initialize from hashed password value
	 * @param u username
	 * @param pwd hashed password
	 */
	public SCNetworkCredentials(String u, String pwd)
	{
		username = u;
		password = pwd;
	}

	public String getUsername()
	{
		return username;
	}

	/**
	 * Returns the hashed password
	 * @return hashed passowrd
	 */
	public String getPassword()
	{
		return password;
	}

	/**
	 * Given a cleartext password, sets the internal password to the hashed
	 * value.
	 * @param text Clear text password
	 */
	public void setPasswordFromClearText(String text)
	{
		String p = text + "PwdSalt134";
		password = sha256(p);
	}

	/**
	 * Given a token returned from the back end server, generates the
	 * appropriate hash to send in response for a login
	 * @param token Token from server
	 * @return Hash for login credentials
	 */
	public String hashPasswordWithToken(String token)
	{
		String p = password + "PEnSalt194" + token;
		return sha256(p);
	}
}
