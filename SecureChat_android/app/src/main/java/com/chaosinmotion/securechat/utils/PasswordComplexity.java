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

package com.chaosinmotion.securechat.utils;

/**
 * Simple utility for assuring sufficiently complex passwords.
 * Created by woody on 4/10/16.
 */
public class PasswordComplexity
{
	public static boolean complexityTest(String str)
	{
		if (str.length() < 8) return false;

		boolean hasCap = false;
		boolean hasLow = false;
		boolean hasNum = false;
		boolean hasPct = false;

		int i,len = str.length();
		for (i = 0; i < len; ++i) {
			char ch = str.charAt(i);

			if ((ch >= '0') && (ch <= '9')) hasNum = true;
			if ((ch >= 'A') && (ch <= 'Z')) hasCap = true;
			if ((ch >= 'a') && (ch <= 'z')) hasLow = true;

			if ((ch > ' ') && (ch < '0')) hasPct = true;	// ASCII assumed
			if ((ch > '9') && (ch < 'A')) hasPct = true;	// ASCII assumed
			if (ch == '~') hasPct = true;
		}

		return hasCap && hasLow && hasNum && hasPct;
	}
}
