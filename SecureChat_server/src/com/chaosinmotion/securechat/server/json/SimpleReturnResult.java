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

package com.chaosinmotion.securechat.server.json;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/**
 * Simple return result. This is for those returns which just take one or
 * two parameters.
 */
public class SimpleReturnResult extends ReturnResult
{
	private HashMap<String,Object> data;
	
	/**
	 * Create an empty simple return result.
	 */
	public SimpleReturnResult()
	{
		super();
		data = new HashMap<String,Object>();
	}
	
	/**
	 * Create a simple return result with a single key/value pair.
	 * @param key
	 * @param value
	 */
	public SimpleReturnResult(String key, String value)
	{
		this();
		put(key,value);
	}
	
	/**
	 * Create a simple return result with a single key/value pair.
	 * @param key
	 * @param value
	 */
	public SimpleReturnResult(String key, int value)
	{
		this();
		put(key,value);
	}
	
	/**
	 * Add a string return value by key.
	 * @param key
	 * @param value
	 */
	public void put(String key, String value)
	{
		data.put(key, value);
	}
	
	/**
	 * Add an integer return value by key
	 * @param key
	 * @param value
	 */
	public void put(String key, int value)
	{
		data.put(key, new Integer(value));
	}
	
	/**
	 * Add a boolean return value by key
	 * @param key
	 * @param value
	 */
	public void put(String key, boolean value)
	{
		data.put(key, value ? Boolean.TRUE : Boolean.FALSE);
	}
	
	/**
	 * Convert the stored values into a JSON object unless the set is empty.
	 */
	public JSONObject returnData()
	{
		if (data.size() != 0) {
			JSONObject obj = new JSONObject();
			for (Map.Entry<String, Object> e: data.entrySet()) {
				obj.put(e.getKey(), e.getValue());
			}
			return obj;
		} else {
			return null;
		}
	}
}
