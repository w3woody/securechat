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

/**
 * Login wraps all of the behavior necessary to handle a login request.
 */
public class Login
{
	/**
	 * Represents a logged in user. This class stores all the state information
	 * we need when a user is logged in, and is stored as part of the session
	 */
	public static class UserInfo
	{
		private int userid;
		
		public UserInfo(int uid)
		{
			userid = uid;
		}
		
		public int getUserID()
		{
			return userid;
		}
	}
	
	/**
	 * Process the login request. This returns null if the user could not
	 * be logged in.
	 * 
	 * The expected parameters are 'username' and 'password', which should
	 * be hashed.
	 * 
	 * @param requestParams
	 * @return
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public static UserInfo processRequest(JSONObject requestParams, String token) throws ClassNotFoundException, SQLException, IOException
	{
		String username = requestParams.optString("username");
		String password = requestParams.optString("password");
		
		/*
		 * Obtain user information from database
		 */
		
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			c = Database.get();
			ps = c.prepareStatement("SELECT userid, password "
					+ "FROM Users "
					+ "WHERE username = ?");
			ps.setString(1, username);
			rs = ps.executeQuery();
			
			if (rs.next()) {
				/*
				 * If the result is found, hash the entry in the way it would
				 * be hashed by the front end, and compare to see if the
				 * hash codes match. (This requires that the hashed password
				 * stored in the back-end has a consistent capitalization.
				 * We arbitrarily pick lower-case for our SHA-256 hex string.
				 */
				int userID = rs.getInt(1);
				String spassword = rs.getString(2);
				
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
				
				if (spassword.equalsIgnoreCase(password)) {
					return new UserInfo(userID);
				}
			}
			return null;
		}
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
}
