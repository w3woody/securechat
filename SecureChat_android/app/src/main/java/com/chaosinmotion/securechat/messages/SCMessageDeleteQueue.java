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
import com.chaosinmotion.securechat.rsa.SCRSAManager;
import com.chaosinmotion.securechat.rsa.SCSHA256;
import com.chaosinmotion.securechat.utils.ThreadPool;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Runs a delete queue which requests messages be deleted as they are
 * received.
 * Created by woody on 4/10/16.
 */
public class SCMessageDeleteQueue
{
	private static class QueueItem
	{
		private int messageID;
		private String checksum;
		private byte[] message;
	}

	private static SCMessageDeleteQueue shared;
	private Thread decodeQueueThread;
	private LinkedList<QueueItem> decodeQueue;
	private LinkedList<QueueItem> deleteQueue;
	private boolean deleteRunning;

	public static synchronized SCMessageDeleteQueue get()
	{
		if (shared == null) shared = new SCMessageDeleteQueue();
		return shared;
	}

	private SCMessageDeleteQueue()
	{
		decodeQueue = new LinkedList<QueueItem>();
		deleteQueue = new LinkedList<QueueItem>();
		decodeQueueThread = new Thread("DeleteThread") {
			@Override
			public void run()
			{
				decodeProc();
			}
		};
		decodeQueueThread.setDaemon(true);
	}

	/**
	 * Enqueue received message for deletion. This decrypts the message
	 * and sends the delete requests with the found checksums.
	 * @param messageID
	 * @param data
	 */
	public void deleteMessage(int messageID, byte[] data)
	{
		QueueItem item = new QueueItem();
		item.messageID = messageID;
		item.message = data;

		synchronized(decodeQueue) {
			decodeQueue.addFirst(item);
			decodeQueue.notifyAll();
		}
	}
	/*
	 *	decode: the method to handle decoding. Runs in a separate thread. This
	 *	basically runs looking for messages added to our queue, then
	 *	decrypts the message in the thread and adds to the delete queue
	 */

	private void decodeProc()
	{
		boolean done = false;
		QueueItem item;

		while (!done) {
			synchronized(decodeQueue) {
				while ((decodeQueue != null) && (decodeQueue.size() == 0)) {
					try {
						wait();
					} catch (InterruptedException e) {
					}
				}

				item = null;
				if (decodeQueue == null) {
					done = true;
				} else {
					item = decodeQueue.removeLast();
				}
			}

			if (item == null) continue;

			/*
			 *  We have an item which needs a checksum calculated
			 */

			byte[] cdata = SCRSAManager.shared().decodeData(item.message);
			item.checksum = SCSHA256.sha256(cdata);
			item.message = null;

			/*
			 *  Encode into delete queue
			 */

			final QueueItem i = item;
			ThreadPool.get().enqueueMain(new Runnable()
			{
				@Override
				public void run()
				{
					addDelete(i);
				}
			});
		}
	}

	private void addDelete(QueueItem item)
	{
		deleteQueue.addFirst(item);
		if (!deleteRunning) {
			runDeleteCall();
		}
	}

	private void runDeleteCall()
	{
		try {
			JSONArray array = new JSONArray();
			for (QueueItem item : deleteQueue) {
				JSONObject obj = new JSONObject();
				obj.put("messageid", item.messageID);
				obj.put("checksum", item.checksum);
				array.put(obj);
			}
			JSONObject params = new JSONObject();
			params.put("messages", array);

			deleteQueue.clear();
			deleteRunning = true;

			SCNetwork.get().request("messages/dropmessages", params, true, false, this, new SCNetwork.ResponseInterface()
			{
				@Override
				public void responseResult(SCNetwork.Response response)
				{
					/*
					 *  I don't care if this succeeds or fails. Clear running
					 *  state and see if the list is empty or not. If it is
					 *  not empty, something new came along with the network
					 *  request was in process, so we repeat until the list
					 *  is empty.
					 */

					deleteRunning = false;

					if (deleteQueue.size() > 0) {
						runDeleteCall();
					}
				}
			});
		}
		catch (JSONException e) {
		}
	}
}
