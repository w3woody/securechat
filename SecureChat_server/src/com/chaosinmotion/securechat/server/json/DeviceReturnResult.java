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

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Return result for the devices associated with a user. This returns a list
 * of UUIDs, one for each device. When sending a message, one message per
 * device must be encoded and sent.
 */
public class DeviceReturnResult extends ReturnResult
{
	private ArrayList<JSONObject> devices;
	private int userid;
	
	public DeviceReturnResult(int u)
	{
		userid = u;
		devices = new ArrayList<JSONObject>();
	}
	
	public void addDeviceUUID(String uuid, String publicKey)
	{
		JSONObject json = new JSONObject();
		json.put("deviceid", uuid);
		json.put("publickey", publicKey);
		devices.add(json);
	}
	
	public JSONObject returnData()
	{
		JSONArray array = new JSONArray();
		for (JSONObject m: devices) {
			array.put(m);
		}
		
		JSONObject obj = new JSONObject();
		obj.put("devices", array);
		obj.put("userid", userid);
		return obj;
	}
}
