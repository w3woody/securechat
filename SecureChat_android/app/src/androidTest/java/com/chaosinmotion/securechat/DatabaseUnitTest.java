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

import com.chaosinmotion.securechat.messages.SCMessageDatabase;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by woody on 4/9/16.
 */
public class DatabaseUnitTest extends AndroidTestCase
{
	public void testFileIO()
	{
		SCMessageDatabase db = new SCMessageDatabase();
		SCMessageDatabase.removeDatabase(getContext());

		File path = SCMessageDatabase.databaseFileLocation(getContext());
		assertTrue(db.openDatabase(getContext()));
		assertTrue(path.exists());
		db.closeDatabase();
		SCMessageDatabase.removeDatabase(getContext());
		assertTrue(!path.exists());
	}

	public void testWriteMessage() throws UnsupportedEncodingException
	{
		SCMessageDatabase db = new SCMessageDatabase();
		SCMessageDatabase.removeDatabase(getContext());

		assertTrue(db.openDatabase(getContext()));

		/*
		 *	Write two messages, and veriy our summary and message stores work
		 */

		byte[] data = "Hi".getBytes("UTF-8");
		assertTrue(db.insertMessage(1,"sender",false,2,null,data));
		assertTrue(db.insertMessage(1,"sender",false,3,null,data));

		List<SCMessageDatabase.Sender> sender = db.getSenders();
		assertTrue(sender.size() == 1);
		SCMessageDatabase.Sender s = sender.get(0);
		assertTrue(s.getSenderName().equals("sender"));
		assertTrue(s.getSenderID() == 1);
		assertTrue(s.getMessageID() == 3);
		assertTrue(s.isReceived() == false);

		List<SCMessageDatabase.Message> messages;
		messages = db.messages(1,0,10);
		assertTrue(messages.size() == 2);

		SCMessageDatabase.Message m = messages.get(0);
		assertTrue(m.getMessageID() == 2);
		assertTrue(m.isReceiveFlag() == false);
		m = messages.get(1);
		assertTrue(m.getMessageID() == 3);
		assertTrue(m.isReceiveFlag() == false);

		/*
		 *	Verify caching works
		 */

		assertTrue(db.insertMessage(1,"sender",true,4,null,data));
		sender = db.getSenders();
		assertTrue(sender.size() == 1);
		s = sender.get(0);
		assertTrue(s.getSenderName().equals("sender"));
		assertTrue(s.getSenderID() == 1);
		assertTrue(s.getMessageID() == 4);
		assertTrue(s.isReceived() == true);
	}

}
