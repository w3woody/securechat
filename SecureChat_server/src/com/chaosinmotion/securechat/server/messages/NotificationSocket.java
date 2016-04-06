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

package com.chaosinmotion.securechat.server.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.UUID;
import org.json.JSONObject;
import org.json.JSONTokener;
import com.chaosinmotion.securechat.server.database.Database;
import com.chaosinmotion.securechat.server.utils.Hash;
import com.chaosinmotion.securechat.shared.Constants;

/**
 * Network socket listening for incoming requests form a particular device.
 * @author woody
 *
 */
public class NotificationSocket implements Runnable
{
	private Socket socket;
	private NotificationService ns;
	private Thread thread;
	
	private SCOutputStream out;
	private SCInputStream in;
	
	private String token;
	private int deviceID;

	private static SimpleDateFormat format;
	static {
		format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	}


	public NotificationSocket(Socket s, NotificationService n)
	{
		socket = s;
		ns = n;
		thread = new Thread(this);
		thread.start();
	}

	/**
	 * Force terminate of this socket.
	 */
	public synchronized void terminate()
	{
		try {
			out.close();
			in.close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Validate username/password and start message pump
	 * @param username
	 * @param password
	 * @return
	 */
	private boolean validate(String username, String password, String deviceid)
	{
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		int dev = 0;

		try {
			c = Database.get();
			ps = c.prepareStatement(
					"SELECT Users.password, Devices.deviceid " +
					"FROM Users, Devices " +
					"WHERE Users.username = ? " + 
					"  AND Devices.userid = Users.userid " + 
					"  AND Devices.deviceuuid = ?");
			ps.setString(1, username);
			ps.setString(2, deviceid);
			rs = ps.executeQuery();
			
			if (rs.next()) {
				/*
				 * If the result is found, hash the entry in the way it would
				 * be hashed by the front end, and compare to see if the
				 * hash codes match. (This requires that the hashed password
				 * stored in the back-end has a consistent capitalization.
				 * We arbitrarily pick lower-case for our SHA-256 hex string.
				 */
				String spassword = rs.getString(1);
				dev = rs.getInt(2);
				
				/*
				 * Encrypt password with token and salt
				 */
				
				spassword = spassword + Constants.SALT + token;
				spassword = Hash.sha256(spassword);
				
				/*
				 * Compare; if matches, then return the user info record
				 * so we can store away. While the SHA256 process returns
				 * consistent case, we compare ignoring case anyway, just
				 * because. :-)
				 */
				
				if (!spassword.equalsIgnoreCase(password)) {
					/*
					 * This fails to run.
					 */
					
					return false;
				}
			} else {
				return false;
			}
			
			/*
			 * At this point we're logged in. Register for real time messages
			 * and send all of the stored messages for this device
			 */
			
			rs.close();
			rs = null;
			ps.close();
			ps = null;
			
			/*
			 * Register for real-time messages. There is a small window in
			 * which we may write messages out of order. We rely on the
			 * client to sort the messages correctly.
			 */
			
			deviceID = dev;
			MessageQueue.getInstance().registerNotification(dev, NotificationSocket.this);
						
			/*
			 * Run query to get messages, and send them to the calling
			 * device. 
			 */
			
			ps = c.prepareStatement("SELECT Messages.messageid, "
					+ "    Messages.senderid, "
					+ "    Users.username, "
					+ "    Messages.toflag, "
					+ "    Messages.received, "
					+ "    Messages.message "
					+ "FROM Messages, Users "
					+ "WHERE Messages.deviceid = ? "
					+ "  AND Messages.senderid = Users.userid");
			ps.setInt(1, deviceID);
			
			rs = ps.executeQuery();
			while (rs.next()) {
				int messageID = rs.getInt(1);
				int senderID = rs.getInt(2);
				String senderName = rs.getString(3);
				boolean toflag = rs.getBoolean(4);
				Timestamp received = rs.getTimestamp(5);
				byte[] message = rs.getBytes(6);
				
				sendMessage(messageID,senderID,senderName,toflag,received,message);
			}
		}
		catch (Exception ignore) {
		}
		finally {
			try {
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (c != null) c.close();
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}
	

	/**
	 * Parse the JSON command. We handle the commands 'token' and 'login'.
	 * @param obj
	 */
	private void processJSONCommand(JSONObject obj)
	{
		// Unknown commands are ignored.

		String cmd = obj.getString("cmd");
		
		if (cmd.equalsIgnoreCase("token")) {
			/*
			 * Write header and token (as string without length)
			 */
			try {
				token = UUID.randomUUID().toString();
				byte[] b = token.getBytes("UTF-8");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				baos.write(0x21);
				baos.write(b);
				out.writeData(baos.toByteArray());
			}
			catch (Exception ex) {
			}
			
		} else if (cmd.equalsIgnoreCase("login")) {
			/*
			 * Validate login for this
			 */
			
			String username = obj.getString("username");
			String password = obj.getString("password");
			String deviceid = obj.getString("deviceid");
			
			/*
			 * Validate
			 */
			
			if (!validate(username,password,deviceid)) {
				byte[] b = new byte[1];
				b[0] = 0x22;
				try {
					out.writeData(b);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Listen for incoming requests on the socket. This starts up an
	 * input stream, which receives commands in JSON format with the
	 * format { "cmd": command, ... params ... }
	 */
	@Override
	public void run()
	{
		try {
			/*
			 * Wrap the input/output streams and start processing requests.
			 */
			out = new SCOutputStream(socket.getOutputStream());
			in = new SCInputStream(socket.getInputStream()) {
				@Override
				public void processPacket(byte[] data)
				{
					try {
						String json = new String(data,"UTF-8");
						JSONTokener t = new JSONTokener(json);
						JSONObject obj = new JSONObject(t);
						
						/*
						 * Parse and process the command
						 */
						
						processJSONCommand(obj);
					}
					catch (UnsupportedEncodingException e) {
						// Should never happen
						e.printStackTrace();
					}
				}
			};
			in.processStream();
			
			/*
			 * When we reach here, we've been closed.
			 */
			
			ns.removeNotificationSocket(NotificationSocket.this);
			if (deviceID != 0) {
				MessageQueue.getInstance().unregisterNotification(deviceID);
			}
		}
		catch (Throwable th) {
			// ignore.
		}
	}

	/**
	 * Internal method for sending a message to the specified device. This
	 * encodes the message as a binary array and transmits it as a single 
	 * packet to the listening device. This allows users to receive messages
	 * during chat as soon as we are able to, for (more or less) just in time
	 * messaging.
	 * 
	 * The packet returned here is similar to the packet returned by the
	 * getmessages api, except we serialize as binary.
	 * 
	 * @param messageid
	 * @param senderid
	 * @param sendername
	 * @param ts
	 * @param message
	 * @throws IOException 
	 */
	void sendMessage(int messageid, int senderid, String sendername, 
			boolean toflag, Timestamp ts, byte[] message) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		
		/*
		 * Encode. First byte is 0x20
		 */
		
		String date;
		synchronized(format) {
			date = format.format(ts);
		}
		
		/*
		 * Formulate packet in expected format
		 */
		dos.writeByte(0x20);		// marker
		dos.writeBoolean(toflag);
		dos.writeInt(messageid);
		dos.writeInt(senderid);
		dos.writeUTF(date);
		dos.writeUTF(sendername);
		dos.writeInt(message.length);
		dos.write(message);
		
		/*
		 * Flush and write packet to device. Our protocol does not depend on
		 * the device actually receiving this message, as we wait until the
		 * device deletes the messages by a separate command.
		 */
		dos.flush();
		out.writeData(baos.toByteArray());
	}

}
