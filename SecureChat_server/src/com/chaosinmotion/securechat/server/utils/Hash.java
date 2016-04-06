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

package com.chaosinmotion.securechat.server.utils;

import java.security.MessageDigest;

/**
 * Utility class performs a SHA-256 hash of the provided string. The string
 * returned is the SHA-256 hash as a lower-case hex string. This needs to be
 * consistent between the back end and the front end for logging in to work.
 */
public class Hash
{
    private static String byteArrayToHex(byte[] bstr)
    {
        StringBuffer buf = new StringBuffer();
        for (byte b: bstr) {
            buf.append(String.format("%02x", 0xFF & b));
        }
        return buf.toString();
    }
    
    public static String sha256(String str)
    {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            return byteArrayToHex(d.digest(str.getBytes("UTF-8")));
            
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";  // never happens
        }
    }
}
