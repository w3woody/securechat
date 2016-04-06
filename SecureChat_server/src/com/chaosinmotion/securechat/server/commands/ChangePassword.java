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
import com.chaosinmotion.securechat.server.utils.Hash;
import com.chaosinmotion.securechat.shared.Constants;

public class ChangePassword
{

	public static boolean processRequest(Login.UserInfo userinfo, JSONObject requestParams,
			String token) throws ClassNotFoundException, SQLException, IOException
	{
		String oldpassword = requestParams.optString("oldpassword");
		String newpassword = requestParams.optString("newpassword");
		
		/*
		 * Validate the old password against the token we received
		 */

		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			c = Database.get();
			ps = c.prepareStatement("SELECT password "
					+ "FROM Users "
					+ "WHERE userid = ?");
			ps.setInt(1, userinfo.getUserID());
			rs = ps.executeQuery();
			
			if (rs.next()) {
				/*
				 * If the result is found, hash the entry in the way it would
				 * be hashed by the front end, and compare to see if the
				 * hash codes match. (This requires that the hashed password
				 * stored in the back-end has a consistent capitalization.
				 * We arbitrarily pick lower-case for our SHA-256 hex string.
				 */
				String spassword = rs.getString(1);
				
				/*
				 * Encrypt password with token and salt
				 */
				
				spassword = spassword + Constants.SALT + token;
				spassword = Hash.sha256(spassword);
				
				/*
				 * Compare; if matches, then return the user info record
				 * so we can store away. While the SHA256 process returns
				 * consistent case, we compare ignoring case anyway, just
				 * because. :-)
				 */
				
				if (!spassword.equalsIgnoreCase(oldpassword)) {
					/* Wrong password */
					return false;
				}
			}
			
			/*
			 * Update password stored with the updated value passed in.
			 */
			
			rs.close();
			ps.close();
			
			ps = c.prepareStatement(
					"UPDATE Users " +
					"SET password = ? " +
					"WHERE userid = ?");
			ps.setString(1, newpassword);
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
