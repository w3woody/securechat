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
import org.json.JSONObject;
import com.chaosinmotion.securechat.server.database.Database;

/**
 * Process a forgotten password. This requires that we return the same token
 * that was generated during the forgotpassword request.
 * @author woody
 *
 */
public class UpdateForgottenPassword
{

	public static boolean processRequest(Login.UserInfo userinfo, JSONObject requestParams) throws ClassNotFoundException, SQLException, IOException
	{
		String newPassword = requestParams.getString("password");
		String requestToken = requestParams.getString("token");
		
		/*
		 * Determine if the token matches for this user record. We are in the
		 * unique situation of having a logged in user, but he doesn't know
		 * his password. We also ignore any requests with an expired
		 * token.
		 */
		
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			/*
			 * Delete old requests
			 */
			c = Database.get();
			ps = c.prepareStatement(
					"DELETE FROM forgotpassword WHERE expires < LOCALTIMESTAMP");
			ps.execute();
			
			ps.close();
			ps = null;
			
			/*
			 * Verify the token we passed back was correct
			 */
			ps = c.prepareStatement(
					"SELECT token "
					+ "FROM forgotpassword "
					+ "WHERE userid = ? "
					+ "AND token = ?");
			ps.setInt(1, userinfo.getUserID());
			ps.setString(2, requestToken);
			rs = ps.executeQuery();
			if (!rs.next()) return false;		// token does not exist or expired.
			
			rs.close();
			rs = null;
			ps.close();
			ps = null;

			/*
			 * Step 2: Modify the password.
			 */
			
			ps = c.prepareStatement("UPDATE Users SET password = ? WHERE userid = ?");
			ps.setString(1, newPassword);
			ps.setInt(2, userinfo.getUserID());
			ps.execute();
			
			return true;
		}
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}

}
