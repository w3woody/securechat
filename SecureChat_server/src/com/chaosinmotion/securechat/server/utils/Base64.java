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

package com.chaosinmotion.securechat.server.utils;

import java.io.ByteArrayOutputStream;

/**
 * Encode/decode Base64. I know there is code built into the Java RTL, but
 * that doesn't show up until v8, so I toss something in here.
 *
 * @author woody
 *
 */
public class Base64
{
    private static final char[] gEncode = {
            'A', 'B', 'C', 'D', 'E',
            'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O',
            'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y',
            'Z', 'a', 'b', 'c', 'd',
            'e', 'f', 'g', 'h', 'i',
            'j', 'k', 'l', 'm', 'n',
            'o', 'p', 'q', 'r', 's',
            't', 'u', 'v', 'w', 'x',
            'y', 'z', '0', '1', '2',
            '3', '4', '5', '6', '7',
            '8', '9', '+', '/'
        };
    
    /**
     * Encode as base64
     * @param data
     * @return
     */
    public static String encode(byte[] data)
    {
    	StringBuffer buffer = new StringBuffer();
        int epos = 0;
        int eval = 0;
    	int wpos = 0;
    	
    	for (byte b: data) {
    		eval = (eval << 8) | (0x00FF & b);
    		++epos;
    		if (epos >= 3) {
    			buffer.append(gEncode[0x3F & (eval >> 18)]);
    			buffer.append(gEncode[0x3F & (eval >> 12)]);
    			buffer.append(gEncode[0x3F & (eval >> 6)]);
    			buffer.append(gEncode[0x3F & eval]);
    			
    			wpos += 4;
    			if (wpos >= 72) {
    				buffer.append('\n');
    				wpos = 0;
    			}
    			
    			eval = 0;
    			epos = 0;
    		}
    	}
    	
    	if (epos > 0) {
    		eval = (eval << (24 - 8 * epos));
    		
    		buffer.append(gEncode[0x3F & (eval >> 18)]);
            buffer.append(gEncode[0x3F & (eval >> 12)]);
            if (epos > 1) {
            	buffer.append(gEncode[0x3F & (eval >> 6)]);
            } else {
            	buffer.append('=');
            }
            buffer.append('=');
    	}
    	
    	return buffer.toString();
    }
    
    /**
     * Internal Base64 character decoding mechanism
     * @param ch The character to decode
     * @return What the character decodes to
     */
    private static int getValue(int ch)
    {
        if ((ch >= 'A') && (ch <= 'Z')) return ch - 'A';
        if ((ch >= 'a') && (ch <= 'z')) return ch - 'a' + 26;
        if ((ch >= '0') && (ch <= '9')) return ch - '0' + 52;
        if (ch == '+') return 62;
        if (ch == '/') return 63;
        if (ch == '=') return -1;
        return -2;
    }

    /**
     * 
     * Decode from base64
     * @param str
     * @return
     */
    public static byte[] decode(String str)
    {
        int epos,eval;
        int read;
        int v;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        eval = 0;
        epos = 0;
        int i,len = str.length();
        for (i = 0; i < len; ++i) {
        	read = str.charAt(i);
            v = getValue(read);
            if (v == -2) continue;
            if (v == -1) break;
            
            eval |= v << (18 - 6 * epos);
            ++epos;
            
            if (epos >= 4) {
            	baos.write(0x00FF & (eval >> 16));
            	baos.write(0x00FF & (eval >> 8));
            	baos.write(0x00FF & eval);
                
                eval = 0;
                epos = 0;
            }
        }
        
        if (epos >= 2) {
        	baos.write(0x00FF & (eval >> 16));
            if (epos >= 3) {
            	baos.write(0x00FF & (eval >> 8));
            }
        }
        
        return baos.toByteArray();
    }
}
