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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Common utilities for parsing and formatting dates
 * Created by woody on 4/9/16.
 */
public class DateUtils
{
	private static final SimpleDateFormat serverFormatter;
	private static final DateFormat textDateFormat;

	static {
		serverFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		serverFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));

		textDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT);
		textDateFormat.setTimeZone(TimeZone.getDefault());
	}

	public static Date parseServerDate(String str)
	{
		try {
			return serverFormatter.parse(str);
		}
		catch (ParseException e) {
			return new Date();        // TODO: how do we handle this?
		}
	}

	public static String formatDisplayTime(Date date)
	{
		return textDateFormat.format(date);
	}
}

