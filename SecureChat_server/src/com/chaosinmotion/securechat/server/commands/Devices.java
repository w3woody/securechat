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
import com.chaosinmotion.securechat.server.json.DeviceReturnResult;
import com.chaosinmotion.securechat.server.json.ReturnResult;
import com.chaosinmotion.securechat.shared.Errors;

public class Devices
{
	/**
	 * Return the list of device identifiers associated with this account.
	 * @param userinfo
	 * @param requestParams
	 * @return
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public static ReturnResult processRequest(Login.UserInfo userinfo,
			JSONObject requestParams) throws ClassNotFoundException, SQLException, IOException
	{
		String username = requestParams.getString("username");
		
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			c = Database.get();

			/*
			 * 	Get user ID
			 */
			
			ps = c.prepareStatement(
					"SELECT userid "
					+ "FROM Users "
					+ "WHERE username = ?");
			ps.setString(1, username);
			rs = ps.executeQuery();
			
			int userid = 0;
			if (rs.next()) {
				userid = rs.getInt(1);
			} else {
				return new ReturnResult(Errors.ERROR_UNKNOWNUSER,"Unknown user");
			}
			rs.close();
			rs = null;
			ps.close();
			ps = null;
			
			/*
			 * Get devices
			 */
			ps = c.prepareStatement(
					"SELECT Devices.deviceuuid, Devices.publickey "
					+ "FROM Devices, Users "
					+ "WHERE Users.userid = Devices.userid "
					+ "AND Users.username = ?");
			ps.setString(1, username);
			rs = ps.executeQuery();
			
			DeviceReturnResult drr = new DeviceReturnResult(userid);
			while (rs.next()) {
				drr.addDeviceUUID(rs.getString(1),rs.getString(2));
			}
			return drr;
		}
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
}
