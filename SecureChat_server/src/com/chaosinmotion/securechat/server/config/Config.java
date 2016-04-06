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

package com.chaosinmotion.securechat.server.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The Config class attempts to get configuration information from a 
 * variety of known directories, so we can configure the database the web
 * server talks to.
 * 
 * This looks for a database properties file in a couple of places; if this
 * doesn't work, rewrite this as needed for your server.
 * 
 * The intent here is to provide the location of the back-end database,
 * along with the username and password required to access the database,
 * and the name of the database to access.
 * 
 * @author woody
 *
 */
public class Config
{
    /**
     * Get the configuration file for this. 
     * @return
     */
    
    private static Properties gProperties;
    
    private static Properties getFromStore()
    {
        try {
            File f = new File("/home/s/securechat.properties");
            InputStream is = new FileInputStream(f);
            Properties p = new Properties();
            p.load(is);
            is.close();
            return p;
        }
        catch (IOException ex) {
            return null;
        }
    }
    
    private static Properties getFromHome()
    {
        try {
            File f = new File(System.getProperty("user.home"));
            f = new File(f,".warconfig/securechat.properties");
            InputStream is = new FileInputStream(f);
            Properties p = new Properties();
            p.load(is);
            is.close();
            return p;
        }
        catch (IOException ex) {
            return null;
        }
    }
    
    /**
     * Get the properties
     * @return
     */
    public static Properties get()
    {
        if (gProperties == null) {
            /*
             * First attempt: user directory
             */
            
            gProperties = getFromStore();
            if (gProperties != null) return gProperties;
            
            gProperties = getFromHome();
            if (gProperties != null) return gProperties;

            /*
             * Second attempt: pull from internal
             */

            try {
                InputStream is = Config.class.getResourceAsStream("database.properties");
                gProperties = new Properties();
                gProperties.load(is);
            }
            catch (Exception ex) {
                return null;
            }
        }
        return gProperties;
    }
}


