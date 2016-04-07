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

import java.security.SecureRandom;

/**
 * SCUUIDGenerator provides a mechanism for generating a type 4 (random) UUID
 * for internal use. We defer to creating our own UUIDs rather than using
 * system generated UUIDs because they may contain device identifying
 * information.
 *
 * We generate it ourselves rather than relying on the java.util.UUID class,
 * out of a show of paranoid precaution.
 *
 * The format of the UUID for Version 4 is described in the Wikipedia article
 * here:
 *
 * https://en.wikipedia.org/wiki/Universally_unique_identifier#Version_4_.28random.29
 *
 * Created by woody on 4/7/16.
 */
public class SCUUIDGenerator
{
	public static String generateUUID()
	{
		// TODO: Should we have a single shared SecureRandom class, as all this
		// initialization could be expensive.
		SecureRandom r = new SecureRandom();
		byte[] buffer = new byte[16];

		r.nextBytes(buffer);

		buffer[6] = (byte)((buffer[6] & 0x0F) | 0x40);		// set version = 4
		buffer[8] = (byte)((buffer[8] & 0x3F) | 0x80);		// Slam top to bits to 10b

		/*
		 *	Format in standard format
		 */

		StringBuilder str = new StringBuilder();
		for (int i = 0; i < 16; ++i) {
			if ((i == 4) || (i == 6) || (i == 8) || (i == 10)) {
				str.append('-');
			}

			str.append(String.format("%02x",0x00FF & buffer[i]));
		}

		return str.toString();
	}
}
