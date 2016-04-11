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

package com.chaosinmotion.securechat.messages;

import com.chaosinmotion.securechat.network.SCNetwork;
import com.chaosinmotion.securechat.rsa.SCRSAEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by woody on 4/10/16.
 */
public class SCDeviceCache
{
	public static class Device
	{
		private String deviceid;
		private String pubkeytext;
		public SCRSAEncoder publickey;

		private Device(String devID, String pubKey)
		{
			deviceid = devID;
			pubkeytext = pubKey;
			publickey = new SCRSAEncoder(pubKey);
		}

		public String getDeviceID()
		{
			return deviceid;
		}

		public String getPublicKeyText()
		{
			return pubkeytext;
		}

		public SCRSAEncoder getPublicKey()
		{
			return publickey;
		}
	}

	private static class DeviceCacheEntry
	{
		long expires;
		int userid;
		List<Device> devices;
	}

	public interface DeviceCallback
	{
		public void foundDevices(int userID, List<Device> array);
	}

	private static SCDeviceCache shared;
	private HashMap<String,DeviceCacheEntry> store;

	public static synchronized SCDeviceCache get()
	{
		if (shared == null) shared = new SCDeviceCache();
		return shared;
	}

	private SCDeviceCache()
	{
		store = new HashMap<String,DeviceCacheEntry>();
	}

	public void devicesForSender(final String sender, final DeviceCallback callback) throws JSONException
	{
		long t = System.currentTimeMillis();
		DeviceCacheEntry e = store.get(sender);
		if ((e != null) && (e.expires > t)) {
			callback.foundDevices(e.userid,e.devices);
		}

		JSONObject d = new JSONObject();
		d.put("username",sender);
		SCNetwork.get().request("device/devices", d, true, false, this, new SCNetwork.ResponseInterface()
		{
			@Override
			public void responseResult(SCNetwork.Response response)
			{
				if (response.isSuccess()) {
					ArrayList<Device> devices = new ArrayList<Device>();

					JSONArray array = response.getData().optJSONArray("devices");
					int i,len = array.length();
					for (i = 0; i < len; ++i) {
						JSONObject obj = array.optJSONObject(i);
						String deviceid = obj.optString("deviceid");
						String publickeytext = obj.optString("publickey");

						Device dev = new Device(deviceid,publickeytext);
						devices.add(dev);
					}

					DeviceCacheEntry entry = new DeviceCacheEntry();
					entry.expires = System.currentTimeMillis() + 300000; // 5 minutes
					entry.devices = devices;
					entry.userid = response.getData().optInt("userid");

					store.put(sender,entry);

					callback.foundDevices(entry.userid,devices);
				} else {
					callback.foundDevices(0,null);
				}
			}
		});
	}
}
