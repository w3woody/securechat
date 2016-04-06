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

public class AddDevice
{
	/**
	 * Add device request. Takes parameters for deviceid, pubkey, as well as
	 * the user info.
	 * @param userinfo
	 * @param requestParams
	 * @return
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public static boolean processRequest(Login.UserInfo userinfo,
			JSONObject requestParams) throws ClassNotFoundException, SQLException, IOException
	{
		String deviceid = requestParams.optString("deviceid");
		String pubkey = requestParams.optString("pubkey");

		/*
		 * Attempt to insert a new user into the database
		 */
		
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			c = Database.get();

			/*
             * We now have the user index. Insert the device. Note that it is
             * highly unlikely we will have a UUID collision, but we verify
             * we don't by deleting any rows in the device table with the
             * specified UUID. The worse case scenario is a collision which
             * knocks someone else off the air. (The alternative would be
             * to accidentally send the wrong person duplicate messages.)
             * 
             * Note that we don't actually use a device-identifying identifer,
             * choosing instead to pick a UUID, so we need to deal with
             * the possibility (however remote) of duplicate UUIDs.
             * 
             * In the off chance we did have a collision, we also delete all
             * old messages to the device; that prevents messages from being
             * accidentally delivered.
             */
            
            ps = c.prepareStatement(
            		"DELETE FROM Messages " +
            		"WHERE messageid IN " +
            		"    (SELECT Messages.messageid " +
            		"     FROM Messages, Devices " +
            		"     WHERE Messages.deviceid = Devices.deviceid " +
            		"     AND Devices.deviceuuid = ?)");
            ps.setString(1, deviceid);
            ps.execute();
            ps.close();
            ps = null;
            		
            ps = c.prepareStatement("DELETE FROM Devices WHERE deviceuuid = ?");
            ps.setString(1, deviceid);
            ps.execute();
            ps.close();
            ps = null;

            ps = c.prepareStatement(
            		"INSERT INTO Devices " +
            		"    ( userid, deviceuuid, publickey ) " +
            		"VALUES " + 
            		"    ( ?, ?, ?)");
            ps.setInt(1, userinfo.getUserID());
            ps.setString(2, deviceid);
            ps.setString(3, pubkey);
            ps.execute();
            
            /*
             * Complete; return result
             */
            
			return true;
		}
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
}
