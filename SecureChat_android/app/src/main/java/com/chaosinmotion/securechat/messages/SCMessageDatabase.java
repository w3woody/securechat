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
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Wraps our message SQLite database.
 * Created by woody on 4/9/16.
 */
public class SCMessageDatabase
{
	private static final String MESSAGEFILE = "messages.db";

	public static class Sender
	{
		private int senderID;
		private String senderName;
		private byte[] lastMessage;
		private Date lastSent;
		private boolean receivedFlag;
		private int messageID;

		Sender(byte[] lastMessage, Date lastSent, int messageID, boolean receivedFlag, int senderID, String senderName)
		{
			this.lastMessage = lastMessage;
			this.lastSent = lastSent;
			this.messageID = messageID;
			this.receivedFlag = receivedFlag;
			this.senderID = senderID;
			this.senderName = senderName;
		}

		public byte[] getLastMessage()
		{
			return lastMessage;
		}

		public Date getLastSent()
		{
			return lastSent;
		}

		public int getMessageID()
		{
			return messageID;
		}

		public boolean isReceived()
		{
			return receivedFlag;
		}

		public int getSenderID()
		{
			return senderID;
		}

		public String getSenderName()
		{
			return senderName;
		}
	}

	public static class Message
	{
		private boolean receiveFlag;
		private int messageID;
		private byte[] message;
		private Date timestamp;

		Message(byte[] message, int messageID, boolean receiveFlag, Date timestamp)
		{
			this.message = message;
			this.messageID = messageID;
			this.receiveFlag = receiveFlag;
			this.timestamp = timestamp;
		}

		public byte[] getMessage()
		{
			return message;
		}

		public int getMessageID()
		{
			return messageID;
		}

		public boolean isReceiveFlag()
		{
			return receiveFlag;
		}

		public Date getTimestamp()
		{
			return timestamp;
		}
	}

	// SQLite database of messages
	private SQLiteDatabase database;

	// Note: our design assumes no more than perhaps a hundred senders or so,
	// as some operations perform an O(N) search against this list. If we have
	// more senders than that, we can replace this with something more complex,
	// such as a mutable dictionary.
	private ArrayList<Sender> cachedSenders;

	// We also cache messages. We assume a single screen showing the messages
	// between sender and receiver; the loaded range allows us to avoid
	// constant queries to the back end just because the user scrolls down one
	// or two rows.
	private int senderID;
	private int loadedRangeLocation;
	private int loadedRangeLength;
	private ArrayList<Message> cachedMessages;

	/************************************************************************/
	/*																		*/
	/*	Database startup/shutdown											*/
	/*																		*/
	/************************************************************************/

	/**
	 * Return the database location given a file context
	 * @param ctx Context for file operations
	 * @return File location of database
	 */
	public static File databaseFileLocation(Context ctx)
	{
		File f = ctx.getFilesDir();
		f = new File(f,MESSAGEFILE);
		return f;
	}

	/**
	 * Create the tables in our database if they don't exist
	 */

	private static void initializeDatabase(SQLiteDatabase db)
	{
		/*
		 *  Create a version row for later use. (In the future we can use
		 *  the version number in order to intelligently upgrade our SQL
		 *  database.
		 */

		db.execSQL("CREATE TABLE IF NOT EXISTS version " +
				"( versionid INTEGER UNIQUE ON CONFLICT IGNORE )");

		String verval = "INSERT INTO version ( versionid ) VALUES ( 1 )";
		db.execSQL(verval);

		/*
		 *  Now create the tables (well, table). Note our contents are
		 *  stored as a message blob encoded using this device's public
		 *  key
		 */

		String create = "CREATE TABLE IF NOT EXISTS messages ( " +
						"  rowid INTEGER PRIMARY KEY AUTOINCREMENT, " +
						"  messageid INTEGER, " +
						"  received INTEGER, " +	// 1 if this was a response
						"  senderid INTEGER, " +	// sender or recipient
						"  timestamp REAL, " +		// in seconds past epoch
						"  sender TEXT, " +
						"  message BLOB )";
		db.execSQL(create);

		String index1 = "CREATE UNIQUE INDEX IF NOT EXISTS messageix1 " +
				"ON messages ( messageid )";
		db.execSQL(index1);

		String index2 = "CREATE INDEX IF NOT EXISTS messageix2 " +
				"ON messages ( senderid )";
		db.execSQL(index2);

		String index3 = "CREATE INDEX IF NOT EXISTS messageix3 " +
				"ON messages ( timestamp )";
		db.execSQL(index3);
	}

	/**
	 * Open the database.
	 * @param ctx Context for file operations
	 * @return True if the database was opened.
	 */
	public boolean openDatabase(Context ctx)
	{
		if (database != null) return true;

		File path = databaseFileLocation(ctx);

		try {
			database = SQLiteDatabase.openOrCreateDatabase(path, null);
		}
		catch (SQLException ex) {
			// Problem opening database. Delete file and try again
			ctx.deleteFile(MESSAGEFILE);
			try {
				database = SQLiteDatabase.openOrCreateDatabase(path, null);
			}
			catch (SQLException ex2) {
				// Still can't open???
				Log.d("SecureChat","Database could not be opened or re-created. Panic");
				return false;
			}
		}

		initializeDatabase(database);
		return true;
	}

	/**
	 * Close the message database
	 */
	public void closeDatabase()
	{
		if (database != null) {
			database.close();
			database = null;
		}
	}

	/**
	 * Remove the SQLite database file
	 * @param ctx Context for file operations
	 */
	public static void removeDatabase(Context ctx)
	{
		ctx.deleteFile(MESSAGEFILE);
	}

	/************************************************************************/
	/*																		*/
	/*	Basic operations													*/
	/*																		*/
	/************************************************************************/

	/**
	 * insert a received message
	 * @param sender Sender ID of sender
	 * @param name String name of sender
	 * @param receiveFlag True if this is received
	 * @param messageID Message identifier
	 * @param timestamp Date when message sent
	 * @param message Message blob
	 * @return True if inserted, false if not.
	 */
	public boolean insertMessage(int sender, String name, boolean receiveFlag,
                                 int messageID, Date timestamp, byte[] message)
	{
		if (database == null) return false;

		SQLiteStatement stmt;

		/*
		 *	Preflight: if we don't have a timestamp, use now as the timestamp.
		 */

		if (timestamp == null) timestamp = new Date();

		/*
		 *	Prepare the insert
		 */

		String statement = "INSERT OR ABORT INTO Messages " +
						   "    ( messageid, received, senderid, sender, " +
						   "      timestamp, message ) " +
						   "VALUES " +
						   "    ( ?, ?, ?, ?, ?, ? )";

		stmt = database.compileStatement(statement);
		stmt.bindLong(1, messageID);
		stmt.bindLong(2, receiveFlag ? 1 : 0);
		stmt.bindLong(3, sender);
		stmt.bindString(4, name);
		stmt.bindDouble(5, timestamp.getTime()/1000.0);
		stmt.bindBlob(6, message);

		stmt.executeInsert();

		/*
		 *	Delete cached messages
		 */

		cachedMessages = null;

		/*
		 *	Now if we have a new message, update the sender state. If the sender
		 *	is not in the list, we wipe out the cached list of senders, so that
		 *	the senders call requeries the database.
		 */

		if (cachedSenders != null) {
			int i, len = cachedSenders.size();
			for (i = 0; i < len; ++i) {
				Sender s = cachedSenders.get(i);
				if (s.senderID == sender) break;
			}

			if (i < len) {
				Sender s = cachedSenders.get(i);
				if (messageID > s.messageID) {
					s.lastMessage = message;
					s.messageID = messageID;
					s.lastSent = timestamp;
					s.receivedFlag = receiveFlag;

					// Move to top of list. Keep in mind we're assuming the
					// number of senders here is small.

					cachedSenders.remove(i);
					cachedSenders.add(0, s);
				}
			} else {
				cachedSenders = null;
			}
		}

		return true;
	}


	/**
	 *	Perform a "group by" query to get the senders.
	 */

	public List<Sender> getSenders()
	{
		if (null != cachedSenders) return cachedSenders;

		/*
		 *	Run grouped query to get the list of senders and the last messages
		 *	associated with them. Note we order by the messageid from the
		 *	server (even if messages have been removed from the server) as this
		 *	assures the order in which messages were really received.
		 */

		String statement =  "SELECT messageid, received, senderid, " +
							"    sender, timestamp, message " +
							"FROM messages " +
							"WHERE messageid IN ( " +
							"    SELECT max(messageid) " +
							"    FROM messages " +
							"    GROUP BY senderid ) " +
							"ORDER BY timestamp";

		Cursor cursor = database.rawQuery(statement,new String[0]);

		cachedSenders = new ArrayList<Sender>();
		while (cursor.moveToNext()) {
			int messageID = cursor.getInt(0);
			int received = cursor.getInt(1);
			int senderID = cursor.getInt(2);

			String sendername = cursor.getString(3);
			Date date = new Date((long)(cursor.getDouble(4) * 1000));
			byte[] message = cursor.getBlob(5);

			Sender s = new Sender(message,date,messageID,received != 0,
					senderID,sendername);
			cachedSenders.add(s);
		}

		cursor.close();

		return cachedSenders;
	}

	public int messageCountForSender(int senderID)
	{
		SQLiteStatement stmt;

		String statement =  "SELECT COUNT(*) " +
							"FROM messages " +
							"WHERE senderid = ? ";

		stmt = database.compileStatement(statement);
		stmt.bindLong(1, senderID);
		long count = stmt.simpleQueryForLong();

		return (int)count;
	}

/**
 *	Get the rows by range for this sender, in order
 */

	public List<Message> messages(int sender, int location, int length)
	{
		if ((cachedMessages != null) && (senderID == sender) &&
				(loadedRangeLocation == location) &&
				(loadedRangeLength == length)) {
			return cachedMessages;
		}

		/*
		 *	Run the query
		 */

		String statement =  "SELECT messageid, message, received, timestamp " +
							"FROM messages " +
							"WHERE senderid = ? " +
							"ORDER BY messageid ASC " +
							"LIMIT ? OFFSET ?";

		String[] args = new String[3];
		args[0] = Integer.toString(sender);
		args[1] = Integer.toString(length);
		args[2] = Integer.toString(location);
		Cursor cursor = database.rawQuery(statement,args);

		cachedMessages = new ArrayList<Message>();
		while (cursor.moveToNext()) {
			int messageID = cursor.getInt(0);
			byte[] message = cursor.getBlob(1);
			int received = cursor.getInt(2);
			Date date = new Date((long)(cursor.getDouble(3) * 1000.0));

			Message s = new Message(message,messageID,received == 1,date);

			cachedMessages.add(s);
		}
		cursor.close();

		/*
		 *	Store the parameters which achieved this result and return
		 */

		senderID = sender;
		loadedRangeLocation = location;
		loadedRangeLength = length;
		return cachedMessages;
	}

	public boolean deleteSenderForIdent(int ident)
	{
		if (database == null) return false;	// database not open.

		SQLiteStatement stmt;

		/*
		 *	Prepare the delete
		 */

		String statement =  "DELETE FROM Messages " +
		                    "WHERE senderid = ?";

		stmt = database.compileStatement(statement);
		stmt.bindLong(1,ident);
		stmt.executeUpdateDelete();

		cachedSenders = null;
		cachedMessages = null;

		return true;
	}

	public boolean deleteMessageForIdent(int ident)
	{
		if (database == null) return false;	// database not open.

		SQLiteStatement stmt;

		/*
		 *	Prepare the delete
		 */

		String statement =  "DELETE FROM Messages " +
							"WHERE messageid = ?";

		stmt = database.compileStatement(statement);
		stmt.bindLong(1, ident);
		stmt.executeUpdateDelete();

		/*
		 *	Delete cached messages
		 */

		cachedSenders = null;
		cachedMessages = null;

		return true;
	}
}
