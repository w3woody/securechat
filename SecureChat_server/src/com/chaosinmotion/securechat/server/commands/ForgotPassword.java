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
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;
import com.chaosinmotion.securechat.server.database.Database;
import com.chaosinmotion.securechat.server.messages.MessageQueue;

public class ForgotPassword
{
	/**
	 * Process a forgot password request. This generates a token that the
	 * client is expected to return with the change password request.
	 * @param requestParams
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws JSONException 
	 * @throws NoSuchAlgorithmException 
	 */

	public static void processRequest(JSONObject requestParams) throws SQLException, ClassNotFoundException, IOException, NoSuchAlgorithmException, JSONException
	{
		String username = requestParams.optString("username");
		
		/*
		 * Step 1: Convert username to the userid for this
		 */
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		int userID = 0;
		String retryID = UUID.randomUUID().toString();

		try {
			c = Database.get();
			ps = c.prepareStatement(
					"SELECT userid " +
					"FROM Users " + 
					"WHERE username = ?");
			ps.setString(1, username);
			rs = ps.executeQuery();
			if (rs.next()) {
				userID = rs.getInt(1);
			}
			
			if (userID == 0) return;
			ps.close();
			rs.close();
			
			/*
			 * Step 2: Generate the retry token and insert into the forgot 
			 * database with an expiration date 1 hour from now.
			 */
			
			Timestamp ts = new Timestamp(System.currentTimeMillis() + 3600000);
			ps = c.prepareStatement(
					"INSERT INTO ForgotPassword " +
					"    ( userid, token, expires ) " +
					"VALUES " +
					"    ( ?, ?, ?)");
			ps.setInt(1, userID);
			ps.setString(2, retryID);
			ps.setTimestamp(3, ts);
			ps.execute();
		}
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
		
		/*
		 * Step 3: formulate a JSON string with the retry and send
		 * to the user. The format of the command we send is:
		 * 
		 * { "cmd": "forgotpassword", "token": token }
		 */
		
		JSONObject obj = new JSONObject();
		obj.put("cmd", "forgotpassword");
		obj.put("token", retryID);
		MessageQueue.getInstance().enqueueAdmin(userID, obj.toString(4));
	}

}
