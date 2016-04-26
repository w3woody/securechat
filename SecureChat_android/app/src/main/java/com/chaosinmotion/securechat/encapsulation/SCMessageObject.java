/*
 * Copyright (c) 2016. William Edward Woody
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>
 *
 */

package com.chaosinmotion.securechat.encapsulation;

import java.io.UnsupportedEncodingException;

/**
 * Represents a message in our system. Allows us to encapsulate different types.
 * Created by woody on 4/26/16.
 */
public class SCMessageObject
{
    /*
     *  TODO: Different types
     */
    private String message;

    /**
     * Construct using raw serialized data
     * @param data
     */
    public SCMessageObject(byte[] data)
    {
        try {
            message = new String(data,"UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            // Should never happen
        }
    }

    /**
     * Construct a new message object
     * @param msg
     */
    public SCMessageObject(String msg)
    {
        message = msg;
    }

    /**
     * Serialize the message for transmitting
     * @return
     */
    public byte[] dataFromMessage()
    {
        try {
            return message.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            return new byte[0];
        }
    }

    /**
     * Return the message as text. Appropriate if this is a text message
     * @return
     */
    public String getMessageAsText()
    {
        return message;
    }

    /**
     * Return a summary of the message suitable for displaying on the home page
     * @return
     */
    public String getSummaryMessageText()
    {
        return message;
    }
}
