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

package com.chaosinmotion.securechat.server.commands;

import java.io.IOException;
import java.sql.SQLException;
import org.json.JSONArray;
import org.json.JSONObject;
import com.chaosinmotion.securechat.server.json.ReturnResult;
import com.chaosinmotion.securechat.server.json.SimpleReturnResult;
import com.chaosinmotion.securechat.server.messages.MessageQueue;
import com.chaosinmotion.securechat.server.utils.Base64;

/**
 * Send message.
 * @author woody
 *
 */
public class SendMessages
{
	/*
	 * Note: sentflag indicates the sender is the receiver of the message.
	 * (That is, this message is being sent to the the owner of deviceid from
	 * the specified user.)
	 */
	public static ReturnResult processRequest(Login.UserInfo userinfo,
			JSONObject requestParams) throws ClassNotFoundException, SQLException, IOException
	{
		int messageid = 0;
		
		JSONArray array = requestParams.optJSONArray("messages");
		int i,len = array.length();
		for (i = 0; i < len; ++i) {
			JSONObject mrecord = array.getJSONObject(i);
			String checksum = mrecord.optString("checksum");
			String message = mrecord.optString("message");
			String deviceid = mrecord.optString("deviceid");
			int destuser = mrecord.optInt("destuser");
			
			byte[] mdata = Base64.decode(message);
			
			/*
			 * If destuser is not provided, then this indicates that the
			 * current logged in user is sending a message *to* the device's
			 * owner. The 'toflag' is set, meaning the device is owned by
			 * the user in userinfo, the message is being sent to the
			 * destuser.
			 */
			if (destuser == 0) {
				MessageQueue.getInstance().enqueue(userinfo.getUserID(), deviceid, false, mdata, checksum);
			} else {
				messageid = MessageQueue.getInstance().enqueue(destuser, deviceid, true, mdata, checksum);
			}
		}
		return new SimpleReturnResult("messageid",messageid);
	}

}
