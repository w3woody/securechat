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

package com.chaosinmotion.securechat.rsa;

/**
 * Checksum
 */
public class SCChecksum
{
	private static final byte POLYNOMIAL = 0x07;
	private static byte[] crc8Table;
	
	// Static initializer
	static {
		crc8Table = new byte[256];
		
		for (int i = 0; i < 256; ++i) {
			byte rem = (byte)i;
			for (int j = 0; j < 8; ++j) {
				if (0 != (rem & 0x80)) {
					rem <<= 1;
					rem ^= POLYNOMIAL;
				} else {
					rem <<= 1;
				}
			}
			crc8Table[i] = rem;
		}
	}
	
	/**
	 * CRC8 checksum
	 * @param crc
	 * @param buf
	 * @return
	 */
	public static byte calcCRC8(byte crc, byte[] buf, int off, int len)
	{
		for (int i = 0; i < len; ++i) {
			byte b = buf[off + i];
			crc = crc8Table[0xFF & (crc ^ b)];
		}
		return crc;
	}

	public static byte calcCRC8(byte crc, byte[] buf)
	{
		for (byte b: buf) {
			crc = crc8Table[0xFF & (crc ^ b)];
		}
		return crc;
	}
}
