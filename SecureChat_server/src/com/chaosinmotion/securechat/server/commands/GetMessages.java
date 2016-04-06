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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.json.JSONObject;
import com.chaosinmotion.securechat.server.database.Database;
import com.chaosinmotion.securechat.server.json.MessageReturnResult;
import com.chaosinmotion.securechat.server.json.ReturnResult;
import com.chaosinmotion.securechat.shared.Errors;

public class GetMessages
{
	public static ReturnResult processRequest(Login.UserInfo userinfo,
			JSONObject requestParams) throws ClassNotFoundException, SQLException, IOException
	{
		String deviceid = requestParams.optString("deviceid");
		MessageReturnResult mrr = new MessageReturnResult();

		/*
		 * Save message to the database.
		 */
		
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			/*
			 * Get the device ID for this device. Verify it belongs to the
			 * user specified
			 */
			c = Database.get();
			ps = c.prepareStatement(
					"SELECT deviceid " +
					"FROM Devices " +
					"WHERE deviceuuid = ? AND userid = ?"); 
			ps.setString(1, deviceid);
			ps.setInt(2, userinfo.getUserID());
			rs = ps.executeQuery();
			
			int deviceID = 0;
			if (rs.next()) {
				deviceID = rs.getInt(1);
			}
			
			rs.close();
			ps.close();
			if (deviceID == 0) {
				return new ReturnResult(Errors.ERROR_UNKNOWNDEVICE,"Unknown device");
			}
			
			/*
			 * Run query to get messages
			 */
			
			ps = c.prepareStatement("SELECT Messages.messageid, "
					+ "    Messages.senderid, "
					+ "    Users.username, "
					+ "    Messages.toflag, "
					+ "    Messages.received, "
					+ "    Messages.message "
					+ "FROM Messages, Users "
					+ "WHERE Messages.deviceid = ? "
					+ "  AND Messages.senderid = Users.userid");
			ps.setInt(1, deviceID);
			
			rs = ps.executeQuery();
			while (rs.next()) {
				int messageID = rs.getInt(1);
				int senderID = rs.getInt(2);
				String senderName = rs.getString(3);
				boolean toflag = rs.getBoolean(4);
				Timestamp received = rs.getTimestamp(5);
				byte[] message = rs.getBytes(6);
				
				mrr.addMessage(messageID, senderID, senderName, toflag, received, message);
			}
				
			/*
			 * Return messages
			 */
			return mrr;
		}
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}

}
