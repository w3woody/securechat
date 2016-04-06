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
import java.io.LineNumberReader;
import java.io.Reader;

/**
 * Internal routine which reads SQL statements from a file for execution. The format
 * of the input stream is:<br><br>
 * 
 * Lines starting with '#' are comment lines<br>
 * Lines ending with ';' are considered an end of statement.
 */
public class SQLReader
{
    private LineNumberReader fReader;
    private int fLastRead;
    
    /**
     * Create a SQL reader
     * @param r
     */
    public SQLReader(Reader r)
    {
        fReader = new LineNumberReader(r);
        fLastRead = 0;
    }
    
    /**
     * Return the last line read
     * @return
     */
    public int getLastLine()
    {
        return fLastRead;
    }
    
    /**
     * Read the next statement.
     * @return The next SQL statement or null if no more can be read.
     * @throws IOException 
     */
    public String getStatement() throws IOException
    {
        StringBuffer buffer = null;
        boolean eol = false;
        String line;
        
        while (!eol && (null != (line = fReader.readLine()))) {
            line = line.trim();
            if (line.length() == 0) continue;
            if (line.startsWith("#")) continue;
            
            if (buffer == null) {
                fLastRead = fReader.getLineNumber();
                buffer = new StringBuffer();
            }
            if (line.endsWith(";")) {
                eol = true;
                line = line.substring(0,line.length()-1);
            }
            buffer.append(line).append('\n');
        }
        
        if (buffer == null) return null;
        return buffer.toString();
    }
    
    public void close() throws IOException
    {
        fReader.close();
    }
}


