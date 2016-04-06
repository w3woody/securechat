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
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import com.chaosinmotion.securechat.server.commands.Login.UserInfo;
import com.chaosinmotion.securechat.server.database.Database;

public class DropMessages
{
	private static class Message
	{
		int message;
		String checksum;
	}

	public static void processRequest(UserInfo userinfo,
			JSONObject requestParams) throws ClassNotFoundException, SQLException, IOException
	{
		ArrayList<Message> messages = new ArrayList<Message>();
		
		JSONArray a = requestParams.getJSONArray("messages");
		int i,len = a.length();
		for (i = 0; i < len; ++i) {
			JSONObject item = a.getJSONObject(i);
			Message msg = new Message();
			msg.message  = item.getInt("messageid");
			msg.checksum = item.getString("checksum");
			messages.add(msg);
		}
		
		/*
		 * Iterate through the messages, deleting each. We only delete a
		 * message if message belongs to the user and the checksum matches.
		 * This assumes it's our message and it was read with someone who
		 * can read the message.
		 * 
		 * (Thus, the weird query)
		 */
		
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			int count = 0;
			c = Database.get();
			ps = c.prepareStatement(
					"DELETE FROM Messages " +
					"WHERE messageid IN " +
					"    (SELECT Messages.messageid " + 
					"     FROM Messages, Devices " +
					"     WHERE Messages.messageid = ? " +
					"     AND Messages.checksum = ? " +
					"     AND Devices.deviceid = Messages.deviceid " + 
					"     AND Devices.userid = ?)");
			
			for (Message msg: messages) {
				/*
				 * Get the device ID for this device. Verify it belongs to the
				 * user specified
				 */
				
				ps.setInt(1, msg.message);
				ps.setString(2, msg.checksum);
				ps.setInt(3, userinfo.getUserID());
				ps.addBatch();
				++count;
				if (count > 10240) {
					ps.executeBatch();
				}
			}
			if (count > 0) {
				ps.executeBatch();
			}
		}
		catch(BatchUpdateException batch) {
			throw batch.getNextException();
		}
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
}
