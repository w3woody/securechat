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

/**
 * Created by woody on 4/7/16.
 */
public class SCChecksum
{
	private static final byte POLYNOMIAL = 0x07;
	private static SCChecksum shared;
	private byte[] crc8Table;

	private SCChecksum()
	{
		crc8Table = new byte[256];

		for (int i = 0; i < 256; i++) {
			byte rem = (byte)i;  /* remainder from polynomial division */
			for (int j = 0; j < 8; j++) {
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
	 * Singleton constructor.
	 * @return
	 */
	public static synchronized SCChecksum get()
	{
		if (shared == null) {
			shared = new SCChecksum();
		}
		return shared;
	}

	/**
	 * Calculate the checksum of the byte, calculating the subset
	 * of the array provided.
	 * @param crc
	 * @param buf
	 * @param off
	 * @param len
	 * @return
	 */
	public byte calcCRC8(byte crc, byte[] buf, int off, int len)
	{
		for (int i = 0; i < len; ++i) {
			byte b = buf[off + i];
			crc = crc8Table[0xFF & (crc ^ b)];
		}
		return crc;
	}


	/**
	 * Calculate the CRC8 checksum. Note that this takes a starting CRC
	 * value; that can be used to chain checksum values across multiple
	 * buffers.
	 * @param crc
	 * @param buf
	 * @return
	 */
	public byte calcCRC8(byte crc, byte[] buf)
	{
		for (int i = 0; i < buf.length; ++i) {
			crc = crc8Table[0x0FF & (crc ^ buf[i])];
		}
		return crc;
	}
}
