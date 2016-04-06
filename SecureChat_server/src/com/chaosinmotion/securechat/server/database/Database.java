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

package com.chaosinmotion.securechat.server.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Database
{
    /**
     * Initialize the database and returns a database connection 
     * that can be used for managing the database
     * @return
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    public static Connection get() throws SQLException, ClassNotFoundException, IOException
    {
        return DatabaseBuilder.openConnection();
    }
    
    /**
     * Validate; return version of schema or throw exception
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    public static int validate() throws ClassNotFoundException, SQLException, IOException
    {
    	Connection c = get();
		int version = 0;
        PreparedStatement ps = c.prepareStatement("SELECT MAX(version) FROM DBVERSION");
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            version = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return version;
    }
}


