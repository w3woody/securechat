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

package com.chaosinmotion.securechat.server.json;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import com.chaosinmotion.securechat.server.utils.Base64;

/**
 * Return result sent when the back end queries for messages
 * @author woody
 *
 */
public class MessageReturnResult extends ReturnResult
{
	public static class Message
	{
		public int messageID;
		public int senderID;
		public boolean toflag;
		public String senderName;
		public String received;
		public String message;
		
		JSONObject getJSON()
		{
			JSONObject obj = new JSONObject();
			
			obj.put("messageID", messageID);
			obj.put("senderID", senderID);
			obj.put("senderName", senderName);
			obj.put("received", received);
			obj.put("toflag", toflag);
			obj.put("message", message);
			
			return obj;
		}
	}
	
	private ArrayList<Message> messages;
	private static SimpleDateFormat format;
	
	static {
		format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	}
	
	public MessageReturnResult()
	{
		messages = new ArrayList<Message>();
	}
	
	public void addMessage(int messageID, int senderID, String senderName, 
			boolean toflag, Timestamp received, byte[] message)
	{
		String date;
		synchronized(format) {
			date = format.format(received);
		}
		
		Message m = new Message();
		m.messageID = messageID;
		m.senderID = senderID;
		m.senderName = senderName;
		m.toflag = toflag;
		m.received = date;
		m.message = Base64.encode(message);
		
		messages.add(m);
	}
	
	/**
	 * Convert the stored values into a JSON object unless the set is empty.
	 */
	public JSONObject returnData()
	{
		JSONArray array = new JSONArray();
		for (Message m: messages) {
			array.put(m.getJSON());
		}
		
		JSONObject obj = new JSONObject();
		obj.put("messages", array);
		return obj;
	}

	
}
