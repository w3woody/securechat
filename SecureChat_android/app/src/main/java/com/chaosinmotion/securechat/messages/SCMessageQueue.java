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

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.chaosinmotion.securechat.network.SCNetwork;
import com.chaosinmotion.securechat.network.SCNetworkCredentials;
import com.chaosinmotion.securechat.rsa.SCRSAEncoder;
import com.chaosinmotion.securechat.rsa.SCRSAManager;
import com.chaosinmotion.securechat.rsa.SCSHA256;
import com.chaosinmotion.securechat.utils.DateUtils;
import com.chaosinmotion.securechat.utils.NotificationCenter;
import com.chaosinmotion.securechat.utils.ThreadPool;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 *	This is the manager which handles all of the messages that are read
 *	and written on this device.
 *
 * Created by woody on 4/11/16.
 */
public class SCMessageQueue
{
	public interface SenderCompletion
	{
		void senderCallback(boolean success);
	}

	/************************************************************************/
	/*																		*/
	/*	Notification Constants          									*/
	/*																		*/
	/************************************************************************/

	public static final String NOTIFICATION_NEWMESSAGE   = "NOTIFICATION_NEWMESSAGE";
	public static final String NOTIFICATION_STARTQUEUE   = "NOTIFICATION_STARTQUEUE";
	public static final String NOTIFICATION_STOPQUEUE    = "NOTIFICATION_STOPQUEUE";
	public static final String NOTIFICATION_ADMINMESSAGE = "NOTIFICATION_ADMINMESSAGE";

	/************************************************************************/
	/*																		*/
	/*	Constants          													*/
	/*																		*/
	/************************************************************************/

	private static final long POLLRATE = 5000;      // 5 seconds in ms

	/************************************************************************/
	/*																		*/
	/*	Fields          													*/
	/*																		*/
	/************************************************************************/

	// Polling API fields
	private static Timer timer;
	private TimerTask timerTask;
	private boolean receiving;

	// Asynchronous API fields
	private Socket socket;
	private SCInputStream input;
	private SCOutputStream output;

	// SQLITE database of messages
	private SCMessageDatabase database;

	/************************************************************************/
	/*																		*/
	/*	Startup/Shutdown													*/
	/*																		*/
	/************************************************************************/

	private static SCMessageQueue instance;

	/**
	 * Get the singleton object
	 * @return
	 */
	public static synchronized SCMessageQueue get()
	{
		if (null == instance) instance = new SCMessageQueue();
		return instance;
	}

	private SCMessageQueue()
	{
	}

	/************************************************************************/
	/*																		*/
	/*	Notification Queue													*/
	/*																		*/
	/************************************************************************/

	/**
	 * Internal routine for processing insert messages
	 */
	private void insertMessage(int sender, String name, boolean receiveFlag,
                               final int messageID, Date timestamp,
                               final byte[] message)
	{
		/*
		 *  Determine if this is an admin message, and if it is, decrypt
		 *  and send notification. Otherwise, insert into our own database,
		 *  delete the message from the back end, and notify we have a
		 *  new message
		 */

		if (sender != 0) {
			if (database == null) return;   // sanity check.

			/*
			 *  Step 1: insert into our database
			 */

			database.insertMessage(sender,name,receiveFlag,messageID,timestamp,message);

			/*
			 *  Step 2: enqueue for deletion from server
			 */

			SCMessageDeleteQueue.get().deleteMessage(messageID,message);

			/*
			 *  Step 3: send notification of message
			 */

			HashMap<String,Object> d = new HashMap<String,Object>();
			d.put("userid",sender);
			d.put("username",name);
			NotificationCenter.defaultCenter().
					postNotification(NOTIFICATION_NEWMESSAGE,this,d);
		} else {
			/*
			 *  Admin message. Decrypt asynchronously, then send as
			 *  a notification
			 */

			ThreadPool.get().enqueueAsync(new Runnable()
			{
				@Override
				public void run()
				{
					try {
						/*
						 *  Step 1: decrypt and post notification
						 */
						byte[] decrypt = SCRSAManager.shared().decodeData(message);
						String json = new String(decrypt, "UTF-8");
						JSONTokener t = new JSONTokener(json);
						JSONObject obj = (JSONObject)t.nextValue();
						final HashMap<String,Object> d = new HashMap<String,Object>();
						d.put("admin",obj);
						ThreadPool.get().enqueueMain(new Runnable()
						{
							@Override
							public void run()
							{
								NotificationCenter.defaultCenter().
										postNotification(NOTIFICATION_ADMINMESSAGE,this,d);
							}
						});

						/*
						 *  Step 2: delete from server
						 */

						SCMessageDeleteQueue.get().deleteMessage(messageID,message);
					}
					catch (UnsupportedEncodingException e) {
					}
					catch (JSONException e) {
					}
				}
			});
		}
	}

	/**
	 * Internal routine for polling for messages, used if we cannot open
	 * a socket for notifications.
	 */
	private void pollForMessages()
	{
		if (receiving) return;
		receiving = true;

		/*
		 *  Poll messages
		 */

		JSONObject obj = new JSONObject();
		try {
			obj.put("deviceid",SCRSAManager.shared().getDeviceUUID());
		}
		catch (JSONException e) {
			// Should never happen
		}
		SCNetwork.get().request("messages/getmessages", obj, false, this, new SCNetwork.ResponseInterface()
		{
			@Override
			public void responseResult(SCNetwork.Response response)
			{
				if (response.isSuccess()) {
					JSONArray a = response.getData().optJSONArray("messages");
					int i,len = a.length();
					for (i = 0; i < len; ++i) {
						JSONObject d = a.optJSONObject(i);

						int messageID = d.optInt("messageID");
						int senderID = d.optInt("senderID");
						String senderName = d.optString("senderName");
						String received = d.optString("received");
						Date timestamp = DateUtils.parseServerDate(received);
						boolean toFlag = d.optBoolean("toflag");
						byte[] data = Base64.decode(d.optString("message"),Base64.DEFAULT);

						insertMessage(senderID,senderName,toFlag,messageID,timestamp,data);
					}
				}
				receiving = false;
			}
		});
	}

	/**
	 * Start polling. Reason is for debugging only
	 */

	private void startPolling(String reason)
	{
		Log.d("SecureChat","Polling because " + reason);

		if (timer == null) {
			timer = new Timer();
		}
		timerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				pollForMessages();
			}
		};

		timer.schedule(timerTask,0,POLLRATE);
	}

	/************************************************************************/
	/*																		*/
	/*	Notification Stream													*/
	/*																		*/
	/************************************************************************/

	/**
	 *	Notification stream login phase two: this is sent in response to a
	 *	token request; this sends the username/password pair for logging in,
	 *	as well as the device on this connection that is listening for
	 *	messages.
	 */

	private void loginPhaseTwo(String token)
	{
		String username = SCRSAManager.shared().getUsername();
		String password = SCRSAManager.shared().getPasswordHash();
		SCNetworkCredentials creds = new SCNetworkCredentials(username,password);

		JSONObject d = new JSONObject();
		try {
			d.put("cmd","login");
			d.put("deviceid",SCRSAManager.shared().getDeviceUUID());
			d.put("username",creds.getUsername());
			d.put("password",creds.hashPasswordWithToken(token));
			final byte[] data = d.toString().getBytes("UTF-8");

			ThreadPool.get().enqueueAsync(new Runnable()
			{
				@Override
				public void run()
				{
					try {
						output.writeData(data);
					}
					catch (IOException e) {
						// Should never happen.
					}
				}
			});
		}
		catch (JSONException e) {
			// Should never happen
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 *  Process a data packet from the back end notification service. A data
	 *  packet response form the back end has the format:
	 *
	 *  first byte
	 *  0x20        Message
	 *  0x21        Token response
	 *  0x22        Login failure
	 *
	 *  Note login success is implicit; if login worked, we start receiving
	 *  message notifications, starting with the backlog of stored messages
	 *  waiting for us
	 */

	private void processDataPacket(byte[] data)
	{
		if (data.length == 0) return;

		if (data[0] == 0x20) {
			/*
			 *  Process received message.
			 */

			ByteArrayInputStream bais = new ByteArrayInputStream(data,1,data.length-1);
			DataInputStream dis = new DataInputStream(bais);

			try {
				boolean toflag = dis.readBoolean();
				int messageID = dis.readInt();
				int senderID = dis.readInt();
				String ts = dis.readUTF();
				String senderName = dis.readUTF();
				int messagelen = dis.readInt();
				byte[] message = new byte[messagelen];
				dis.read(message);

				dis.close();

				insertMessage(senderID,senderName,toflag,messageID,DateUtils.parseServerDate(ts),message);

			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (data[0] == 0x21) {
			/*
			 * Received token; rest is string
			 */

			try {
				String token = new String(data,1,data.length-1,"UTF-8");
				loginPhaseTwo(token);
			}
			catch (UnsupportedEncodingException e) {
				// SHould never happen
			}
		} else if (data[0] == 0x22) {
			/*
			 *  Login failure. Close connection and start polling
			 */

			closeConnection();
			startPolling("Login failure");
		}
	}

	/**
	 * Close network connection to notification stream
	 */
	private void closeConnection()
	{
		try {
			input.close();
		}
		catch (IOException e) {
		}

		try {
			output.close();
		}
		catch (IOException e) {
		}

		try {
			socket.close();
		}
		catch (IOException e) {
		}

		socket = null;
		input = null;
		output = null;
	}

	/**
	 * The back end is advertising an endpoint we can connect to for
	 * asynchronous networking. Attempt to open a connection. Note that
	 * this must be kicked off in a background thread.
	 */

	private void openConnection(String host, int port, boolean ssl) throws NoSuchAlgorithmException, KeyManagementException, IOException, JSONException
	{
		if (ssl) {
			TrustManager acceptAllTrustManager = new X509TrustManager() {
				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
				{
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
				{
				}

				@Override
				public X509Certificate[] getAcceptedIssuers()
				{
					return new X509Certificate[0];
				}
			};
			TrustManager[] tm = new TrustManager[] { acceptAllTrustManager };
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(new KeyManager[0],tm,new SecureRandom());

			SSLSocketFactory factory = context.getSocketFactory();

			socket = factory.createSocket(host,port);
		} else {
			socket = new Socket(host,port);
		}

		/*
		 *  Kick off an output stream
		 */

		output = new SCOutputStream(socket.getOutputStream());

		/*
		 *  Kick off a thread to process the input stream
		 */

		Thread thread = new Thread() {
			@Override
			public void run()
			{
				try {
					input = new SCInputStream(socket.getInputStream())
					{
						@Override
						public void processPacket(byte[] data)
						{
							processDataPacket(data);
						}
					};
					input.processStream();
					input.close();

					/*
					 *  When the input closes, we simply quit the thread.
					 *  TODO: I'm not sure if that's the correct answer.
					 */
				}
				catch (final Exception ex) {
					ThreadPool.get().enqueueMain(new Runnable()
					{
						@Override
						public void run()
						{
							startPolling("Unknown exception " + ex.getMessage());
							Log.d("SecureChat","Exception while opening socket",ex);
						}
					});
				}
			}
		};
		thread.start();

		/*
		 *	Now the first packet we need to send to the writer (and our
		 *	output stream will cache this) is a JSON request to log in.
		 *
		 *	On the off chance logging in fails, the back end will simply
		 *	close the connection.
		 *
		 *	Because there is no one-to-one (in theory) of data sent and
		 *	received, we drive this through a state machine.
		 */

		JSONObject obj = new JSONObject();
		obj.put("cmd","token");
		byte[] data = obj.toString().getBytes("UTF-8");
		output.writeData(data);
	}

	/**
	 * Ask the back end if we can connect to a separate port for async
	 * message handling; if not, start background polling
	 */
	private void startNetworkQueue()
	{
		SCNetwork.get().request("messages/notifications", null, false, this, new SCNetwork.ResponseInterface()
		{
			@Override
			public void responseResult(SCNetwork.Response response)
			{
				/*
				 *  If we get here but we're already running, bail.
				 */

				if ((socket != null) || (timerTask != null)) return;

				/*
				 *  Success or error?
				 */

				if (response.isSuccess()) {
					final String host = response.getData().optString("host");
					final int port = response.getData().optInt("port");
					final boolean ssl = response.getData().optBoolean("ssl");

					ThreadPool.get().enqueueAsync(new Runnable()
					{
						@Override
						public void run()
						{
							try {
								openConnection(host, port, ssl);
							}
							catch (final Exception ex) {
								ThreadPool.get().enqueueMain(new Runnable()
								{
									@Override
									public void run()
									{
										startPolling("Unknown exception " + ex.getMessage());
										Log.d("SecureChat","Exception while opening socket",ex);
									}
								});
							}
						}
					});
				} else {
					startPolling("Server responsed unavailable");
				}
			}
		});
	}

	/************************************************************************/
	/*																		*/
	/*	External Methods													*/
	/*																		*/
	/************************************************************************/

	/**
	 *	Start the message queue. This makes sure the messages are loaded from
	 *	memory and starts either the periodic timer or the direct network
	 *	connection to the server to send and receive messages
	 */

	public void startQueue(Context ctx)
	{
		if (!SCRSAManager.shared().canStartServices()) return;
		if (database != null) return;   // already started

		/*
		 *  Open database
		 */

		if (database == null) {
			database = new SCMessageDatabase();
			database.openDatabase(ctx);
		}

		/*
		 *  Start network queue and send notification
		 */

		startNetworkQueue();

		NotificationCenter.defaultCenter().postNotification(NOTIFICATION_STARTQUEUE,this);
	}

	/**
	 * Stop message queue
	 */

	public void stopQueue()
	{
		if (database == null) return;       // Already stopped
		NotificationCenter.defaultCenter().postNotification(NOTIFICATION_STOPQUEUE,this);

		/*
		 *  Close connection or stop polling
		 */

		if (null != socket) {
			closeConnection();
		}
		if (null != timerTask) {
			timerTask.cancel();
			timerTask = null;
		}

		/*
		 *  Close database
		 */

		if (database != null) {
			database.closeDatabase();
			database = null;
		}
	}

	/**
	 * Clear queue
	 */

	public void clearQueue(Context ctx)
	{
		stopQueue();
		SCMessageDatabase.removeDatabase(ctx);
	}

	/************************************************************************/
	/*																		*/
	/*	Database Access														*/
	/*																		*/
	/************************************************************************/

	public List<SCMessageDatabase.Sender> getSenders()
	{
		if (database == null) return new ArrayList<SCMessageDatabase.Sender>();   // empty list.
		return database.getSenders();
	}

	public int getMessagesForSender(int senderID)
	{
		if (database == null) return 0;
		return database.messageCountForSender(senderID);
	}

	public List<SCMessageDatabase.Message> getMessagesInRange(int senderID, int location, int length)
	{
		if (database == null) return new ArrayList<SCMessageDatabase.Message>();
		return database.messages(senderID,location,length);
	}

	public boolean deleteSender(int senderID)
	{
		if (database == null) return false;
		return database.deleteSenderForIdent(senderID);
	}

	public boolean deleteMessage(int messageID)
	{
		if (database == null) return false;
		return database.deleteMessageForIdent(messageID);
	}

	/************************************************************************/
	/*																		*/
	/*	Sending Messages													*/
	/*																		*/
	/************************************************************************/

	private void encodeMessages(String clearText, final String sender, final int senderID,
	                            List<SCDeviceCache.Device> sarray,
	                            List<SCDeviceCache.Device> marray,
	                            final SenderCompletion callback) throws UnsupportedEncodingException
	{
		/*
		 *  Calculate message checksum
		 */

		byte[] cdata = clearText.getBytes("UTF-8");
		String checksum = SCSHA256.sha256(cdata);

		/*
		 *  Build the encoding list to encode all sent messages.
		 */

		JSONArray messages = new JSONArray();

		// Devices we're sending to
		for (SCDeviceCache.Device d: sarray) {
			SCRSAEncoder encoder = d.getPublicKey();
			byte[] encoded = new byte[0];
			try {
				encoded = encoder.encodeData(cdata);
				String message = Base64.encodeToString(encoded,Base64.DEFAULT);
				JSONObject ds = new JSONObject();
				ds.put("checksum",checksum);
				ds.put("message",message);
				ds.put("deviceid",d.getDeviceID());
				messages.put(ds);
			} catch (Exception e) {
				Log.d("SecureChat","Exception",e);
				// Should not happen; only if there is a constant error above
			}
		}

		// My devices
		for (SCDeviceCache.Device d: marray) {
			if (d.getDeviceID().equals(SCRSAManager.shared().getDeviceUUID())) {
				// Skip me; we put me last
				continue;
			}

			SCRSAEncoder encoder = d.getPublicKey();
			byte[] encoded = new byte[0];
			try {
				encoded = encoder.encodeData(cdata);
				String message = Base64.encodeToString(encoded,Base64.DEFAULT);
				JSONObject ds = new JSONObject();
				ds.put("checksum",checksum);
				ds.put("message",message);
				ds.put("deviceid",d.getDeviceID());
				ds.put("destuser",senderID);
				messages.put(ds);
			} catch (Exception e) {
				Log.d("SecureChat","Exception",e);
				// Should not happen; only if there is a constant error above
			}
		}

		// Encode for myself. This is kind of a kludge; we need
		// the message ID from the back end to assure proper sorting.
		// But we only get that if this is the last message in the
		// array of messages. (See SendMessages.java.)
		SCRSAEncoder encoder = new SCRSAEncoder(SCRSAManager.shared().getPublicKey());
		byte[] encoded = new byte[0];
		try {
			encoded = encoder.encodeData(cdata);
			String message = Base64.encodeToString(encoded,Base64.DEFAULT);
			JSONObject ds = new JSONObject();
			ds.put("checksum",checksum);
			ds.put("message",message);
			ds.put("deviceid",SCRSAManager.shared().getDeviceUUID());
			ds.put("destuser",senderID);
			messages.put(ds);
		} catch (Exception e) {
			Log.d("SecureChat","Exception",e);
			// Should not happen; only if there is a constant error above
		}

		/*
		 *  Send all messages to the back end.
		 */

		JSONObject params = new JSONObject();
		try {
			params.put("messages",messages);
		}
		catch (JSONException ex) {
			// Should never happen
		}

		final byte[] encodedData = encoded;
		SCNetwork.get().request("messages/sendmessages", params, false, this, new SCNetwork.ResponseInterface()
		{
			@Override
			public void responseResult(SCNetwork.Response response)
			{
				if (response.isSuccess()) {
					int messageID = response.getData().optInt("messageid");

					// Insert sent message into myself. This is so we
					// immediately see the sent message right away.
					// Note we may have a race condition but we don't
					// care; the messageID will screen out duplicates.
					insertMessage(senderID,sender,true,messageID,new Date(),encodedData);
				}

				callback.senderCallback(response.isSuccess());
			}
		});
	}

	/**
	 *	Message send. This also handles encryption and enqueuing into our own
	 *	internal queue. Note that internally we cache senders and the devices
	 *	on which messages are sent for a user, refreshing every 5 minutes.
	 */

	public void sendMessage(final String clearText, final String sender,
	                        final SenderCompletion callback)
	{
		/*
		 *  This gets the devices for the sender, for me, and then
		 *  runs the encoding process on a background thread.
		 */
		SCDeviceCache.get().devicesForSender(sender, new SCDeviceCache.DeviceCallback()
		{
			@Override
			public void foundDevices(final int userID, final List<SCDeviceCache.Device> sarray)
			{
				if (sarray == null) {
					callback.senderCallback(false);
				}

				/*
				 *  Get our devices as well.
				 */

				String me = SCRSAManager.shared().getUsername();
				SCDeviceCache.get().devicesForSender(me, new SCDeviceCache.DeviceCallback()
				{
					@Override
					public void foundDevices(int meID, final List<SCDeviceCache.Device> marray)
					{
						if (marray == null) {
							callback.senderCallback(false);
						}


						/*
						 *  Now encode the message and send to the back end. Note
						 *  that we run this on a background thread.
						 */

						ThreadPool.get().enqueueAsync(new Runnable()
						{
							@Override
							public void run()
							{
								try {
									encodeMessages(clearText,sender,userID,sarray,marray,callback);
								}
								catch (UnsupportedEncodingException e) {
									callback.senderCallback(false);
								}
							}
						});
					}
				});
			}
		});
	}
}
