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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import com.chaosinmotion.securechat.rsa.SCRSAEncoder;
import com.chaosinmotion.securechat.server.database.Database;
import com.chaosinmotion.securechat.server.utils.Hash;
import com.chaosinmotion.securechat.shared.Constants;

/**
 * This class helps manage the process of sending messages. This saves the
 * message into the message array, and sends the notification out indicating
 * a message has been received.
 * @author woody
 *
 */
public class MessageQueue
{
	private static final long DROPPERIOD = 3600000;	/* 60*60*100 = 1 hour in ms */
	private static final long OLDMESSAGETIME = 604800000; /* 7*24*60*60*100 = 1 week in ms */

	private static MessageQueue messageQueue;
	private HashMap<Integer,NotificationSocket> notifications;
	private Timer timer;
	private TimerTask dropTask;
	
	private static class DeviceRecord
	{
		DeviceRecord(int devID, String pubKey)
		{
			deviceID = devID;
			publicKey = pubKey;
		}
		
		int deviceID;
		String publicKey;
	}
	
	public static synchronized MessageQueue getInstance()
	{
		if (messageQueue == null) {
			messageQueue = new MessageQueue();
		}
		return messageQueue;
	}
	
	private MessageQueue()
	{
		notifications = new HashMap<Integer,NotificationSocket>();
		
		/*
		 * Create timer and task to drop old messages
		 */
		
		timer = new Timer();
		dropTask = new TimerTask() {
			@Override
			public void run()
			{
				dropOldMessages();
			}
		};
		timer.scheduleAtFixedRate(dropTask, DROPPERIOD, DROPPERIOD);
	}
	
	/**
	 * Register a notification socket so when we send out messages they're
	 * immediately sent to the device
	 */
	
	synchronized void registerNotification(int deviceID, NotificationSocket socket)
	{
		notifications.put(deviceID, socket);
	}
	
	/**
	 * Unregister a notification socket
	 */
	synchronized void unregisterNotification(int deviceID)
	{
		notifications.remove(deviceID);
	}
	
	/**
	 * The purpose of this method is to drop old messages. Messages can
	 * accumulate because a device was erased without being dropped, or
	 * has not been checked in a while. We drop messages that are more than
	 * a week old, regardless if they've been delivered.
	 * 
	 * This should be called every hour.
	 */
	private void dropOldMessages()
	{
		Connection c = null;
		PreparedStatement ps = null;
		
		Timestamp ts = new Timestamp(System.currentTimeMillis() - OLDMESSAGETIME);
		TimeZone tz = TimeZone.getTimeZone("UTC");
		Calendar cal = Calendar.getInstance(tz);

		try {
			c = Database.get();
			ps = c.prepareStatement(
					"DELETE FROM Messages " +
					"WHERE received < ?");
			ps.setTimestamp(1, ts, cal);
			ps.execute();
		}
		catch (Throwable th) {
			// ignore.
		}
		finally {
			if (ps != null) {
				try {
					ps.close();
				}
				catch (SQLException e) {
				}
			}
			if (c != null) {
				try {
					c.close();
				}
				catch (SQLException e) {
				}
			}
		}
	}
	
	/**
	 * This encodes a cleartext message to be sent to a receiver, coded as a
	 * message from an administrator. This is used to send requests (in JSON
	 * format) for things like resetting a password
	 * @param receiver Receiver UserID
	 * @param message Message (as clear text) to send
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws NoSuchAlgorithmException 
	 */
	public int enqueueAdmin(int receiver, String message) throws ClassNotFoundException, SQLException, IOException, NoSuchAlgorithmException
	{
		/*
		 * Calculate checksum using raw message and salt. This allows us to
		 * prevent someone from deleting messages unless the message was
		 * actually correctly decoded.
		 */
		String checksum = Hash.sha256(message + Constants.SALT3);
		
		/*
		 * Get the list of devices associated with this user, and broadcast
		 * the message to all of them.
		 */
		
		ArrayList<DeviceRecord> deviceList = new ArrayList<DeviceRecord>();
		
		/*
		 * Attempt to insert a new user into the database
		 */
		
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			c = Database.get();
			ps = c.prepareStatement(
					"SELECT deviceid, publickey " +
					"FROM Devices " + 
					"WHERE userid = ?");
			ps.setInt(1, receiver);
			rs = ps.executeQuery();
			while (rs.next()) {
				DeviceRecord dr = new DeviceRecord(rs.getInt(1), rs.getString(2));
				deviceList.add(dr);
			}
		}
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
		
		/*
		 * Now encode the message for each device
		 */
		
		int retID = 0;
		for (DeviceRecord dr: deviceList) {
			SCRSAEncoder encoder = new SCRSAEncoder(dr.publicKey);
			byte[] encMsg = encoder.encodeData(message.getBytes("UTF-8"));
			retID = enqueue(0,dr.deviceID,false,encMsg,checksum);
		}
		return retID;
	}
	
	/**
	 * Enqueue a message from the sender specified to the end-user given. This
	 * encodes the message for this user
	 * 
	 * @param senderid. If toflag is set, then this is being enqueued onto the
	 * user's own device as being sent to the specified sender. Otherwise, the
	 * senderid is the person who sent this message.
	 * @param deviceuuid
	 * @param message
	 * @param checksum
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public int enqueue(int senderid, String deviceuuid, boolean toflag, byte[] message, String checksum) throws ClassNotFoundException, SQLException, IOException
	{
		/*
		 * Convert device UUID into a device index.
		 */
		
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		int deviceID = 0;

		try {
			c = Database.get();
			ps = c.prepareStatement(
					"SELECT deviceid " +
					"FROM Devices " + 
					"WHERE deviceuuid = ?");
			ps.setString(1, deviceuuid);
			rs = ps.executeQuery();
			if (rs.next()) {
				deviceID = rs.getInt(1);
			}
		}
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
		
		if (deviceID != 0) {
			return enqueue(senderid,deviceID,toflag,message,checksum);
		} else {
			return 0;
		}
	}
	
	/**
	 * Enqueue message for device by device ID. See discussion here for more
	 * information:
	 * 
	 * https://jdbc.postgresql.org/documentation/head/binary-data.html
	 * 
	 * @param senderid
	 * @param deviceid
	 * @param message
	 * @param checksum
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	private int enqueue(int senderid, int deviceid, boolean toflag, byte[] message, String checksum) throws ClassNotFoundException, SQLException, IOException
	{
		/*
		 * Save message to the database.
		 */
		
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Timestamp ts = new Timestamp(System.currentTimeMillis());
		TimeZone tz = TimeZone.getTimeZone("UTC");
		Calendar cal = Calendar.getInstance(tz);

		try {
			/*
			 * Insert the message, retrieving the message ID. (Used later)
			 */
			c = Database.get();
			ps = c.prepareStatement(
					"INSERT INTO Messages " +
					"    ( deviceid, senderid, toflag, received, checksum, message ) " +
					"VALUES " +
					"    ( ?, ?, ?, ?, ?, ? ); SELECT currval('Messages_messageid_seq')");
			ps.setInt(1, deviceid);
			ps.setInt(2, senderid);
			ps.setBoolean(3, toflag);
			ps.setTimestamp(4, ts, cal);
			ps.setString(5, checksum);
			ps.setBytes(6, message);
			
			ps.execute();
			int utc = ps.getUpdateCount();
            int messageid = 0;
            if ((utc == 1) && ps.getMoreResults()) {
                rs = ps.getResultSet();
                if (rs.next()) {
                	messageid = rs.getInt(1);
                }
                rs.close();
                rs = null;
            }

            ps.close();
            ps = null;
			
			/*
			 * If the device is registered for notifications, then we look up
			 * the extra information we need (such as the sender's name) and
			 * immediately send a notification.
			 */
			
			NotificationSocket socket;
			synchronized(this) {
				socket = notifications.get(deviceid);
			}
			if (socket != null) {
				ps = c.prepareStatement("SELECT username FROM users WHERE userid = ?");
				ps.setInt(1, senderid);
				rs = ps.executeQuery();
				String sendername = "";
				if (rs.next()) {
					sendername = rs.getString(1);
				}
				socket.sendMessage(messageid, senderid, sendername, toflag, ts, message);
			}

			return messageid;
		}
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
}
