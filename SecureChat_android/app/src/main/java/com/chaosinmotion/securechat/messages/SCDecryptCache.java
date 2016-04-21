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

import com.chaosinmotion.securechat.rsa.SCRSAManager;
import com.chaosinmotion.securechat.utils.ThreadPool;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;

/**
 * Decryption class. This takes a block of data and an index, and
 * decrypts the message by messageID. If the message has already
 * been decrypted, the data is simply returned from the cache. Note
 * the data is stored in memory only.
 *
 * Created by woody on 4/10/16.
 */
public class SCDecryptCache
{
	private static class Cache extends LinkedHashMap<Integer,String>
	{
		public Cache()
		{
			super(100,0.75f,true);
		}
		@Override
		protected boolean removeEldestEntry(Entry<Integer, String> eldest)
		{
			return size() > 250;        // 250 messages, arbitrary limit
		}
	}

	public interface DecryptCallback
	{
		void decryptedMessage(int messageID, String msg);
	}

	private static SCDecryptCache shared;
	private Cache cache;

	private SCDecryptCache()
	{
		cache = new Cache();
	}

	public static synchronized SCDecryptCache get()
	{
		if (shared == null) {
			shared = new SCDecryptCache();
		}
		return shared;
	}

	/**
	 * Decrypt the message. If stored in the cache, the message is returned
	 * immediately. Otherwise, the message is decrypted in a background
	 * thread and returned once decrypted.
	 * @param data The data to decrypt
	 * @param index The message index of the message to decrypt
	 * @param callback The callback if this is decoded asynchronously
	 * @return The message or null if not in the cache
	 */
	public String decrypt(final byte[] data, final int index, final DecryptCallback callback)
	{
		String ret = cache.get(index);
		if (ret != null) return ret;

		ThreadPool.get().enqueueAsync(new Runnable()
		{
			@Override
			public void run()
			{
				try {
					byte[] decrypt = SCRSAManager.shared().decodeData(data);
					final String str = new String(decrypt,"UTF-8");
					ThreadPool.get().enqueueMain(new Runnable()
					{
						@Override
						public void run()
						{
							cache.put(index,str);
							callback.decryptedMessage(index,str);
						}
					});
				} catch (UnsupportedEncodingException e) {
					callback.decryptedMessage(index,"");
				}
			}
		});
		return null;
	}
}
