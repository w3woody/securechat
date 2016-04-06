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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Properties;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import com.chaosinmotion.securechat.server.config.Config;

/**
 * Notification service; this attempts to open a socket to receive
 * connections. This will be started up the first time a device attempts
 * to get the connection location, and listens for incoming requests by
 * devices seeking for real-time message notifications.
 * 
 * Note that this won't scale. Meaning there is a finite number of devices
 * that can connect to this service. The assumption is if you want a system
 * which can scale beyond a few hundred or thousand users, you would want
 * to rewrite this appropriately. (One way to do this is by sharing the
 * back-end database, and to use multiple servers, a switch, and additional
 * code to coordinate message notifications on a private network through
 * broadcasts.)
 * 
 * Also note this can be disabled using the SecureChat.properties file
 * by setting notifications = no
 * 
 * @author woody
 *
 */
public class NotificationService
{
	private static NotificationService shared;
	private ServerSocket socket;
	private Thread socketThread;
	private boolean stopService;
	private ArrayList<NotificationSocket> notArray;
	
	private String hostname;
	private boolean useSSLFlag;
	private Throwable startError;

	/**
	 * Get the shared notification service
	 */
	public synchronized static NotificationService getShared()
	{
		if (shared == null) {
			shared = new NotificationService();
		}
		return shared;
	}
	
	/**
	 * Construct the service and start.
	 */
	private NotificationService()
	{
		/*
		 * Verify notifications are enabled. If not, return without starting.
		 */
		Properties p = Config.get();
		String value = p.getProperty("notifications");
		if ((value != null) && (value.equalsIgnoreCase("no"))) return;
		
		/*
		 * Get fields for the hostname and port
		 */
		hostname = p.getProperty("hostname");
		value = p.getProperty("hostport");
		
		/*
		 * Determine notification port
		 */
		int port = 0;
		if (value != null) {
			try {
				port = Integer.parseInt(value);
			}
			catch (NumberFormatException ex) {
				port = 0;
			}
		}
		
		/*
		 * Determine if we should use an SSL connection for our notification
		 * socket
		 */
		
		String keystore = p.getProperty("keystorefile");
		String password = p.getProperty("keystorepassword");
		String usessl = p.getProperty("notificationssl");
		useSSLFlag = false;
		if ((usessl != null) && usessl.equals("yes")) {
			useSSLFlag = true;
		}
		
		/*
		 * Now attempt to start notifications.
		 */
		notArray = new ArrayList<NotificationSocket>();

		try {
			if (useSSLFlag) {
				/*
				 * Create SSL server socket.
				 */
				
				FileInputStream keyFile = new FileInputStream(keystore); 
				KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
				keyStore.load(keyFile, password.toCharArray());

				KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				keyManagerFactory.init(keyStore, password.toCharArray());

				KeyManager keyManagers[] = keyManagerFactory.getKeyManagers();

				SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
				sslContext.init(keyManagers, null, new SecureRandom());

				SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();

				socket = socketFactory.createServerSocket(port, 50);
			} else {
				socket = new ServerSocket(port,50);
			}
			
			/*
			 *  Capture hostname
			 */
			if (hostname == null) {
				InetAddress address = InetAddress.getLocalHost();
				hostname = address.getHostName();
			}
			
			stopService = false;
			socketThread = new Thread(new Runnable() {
				@Override
				public void run()
				{
					/*
					 * Internal thread; get socket; if one, then bind to
					 * our notification socket handler.
					 */
					while (!stopService) {
						try {
							Socket s = socket.accept();
							NotificationSocket socket = new NotificationSocket(s,NotificationService.this);
							notArray.add(socket);
						}
						catch (IOException e) {
						}
					}
				}
			});
			socketThread.start();
		}
		catch (Exception ex) {
			startError = ex;
			socket = null;
		}
	}
	
	/**
	 * Return running
	 * @return
	 */
	public boolean isRunning()
	{
		return socket != null;
	}
		
	/**
	 * If we were able to open a server port, returns the port ID. Otherwise
	 * returns 0.
	 * @return
	 */
	public int getServerPort()
	{
		if (socket != null) {
			return socket.getLocalPort();
		} else {
			return 0;
		}
	}
	
	/**
	 * Return true if we're using SSL for this connection
	 * @return
	 */
	public boolean getSSLFlag()
	{
		if (socket != null) {
			return useSSLFlag;
		} else {
			return false;
		}
	}
	
	/**
	 * If we were able to open a server port, return the server address as
	 * dot notation
	 * @return
	 */
	public String getServerAddress()
	{
		if (socket != null) {
			return hostname;
		} else {
			return null;
		}
	}
	
	public Throwable getStartException()
	{
		return startError;
	}
	
	void removeNotificationSocket(NotificationSocket s)
	{
		notArray.remove(s);
	}
	
	/**
	 * Terminate the notification service
	 */
	public void terminate()
	{
		if (socket != null) {
			/*
			 * Mark as closed
			 */
			synchronized(this) {
				stopService = true;
			}
			
			/*
			 * Close socket
			 */
			try {
				socket.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			socket = null;
		}
		
		/*
		 * Send termination to all current connections
		 */
		ArrayList<NotificationSocket> c = new ArrayList<NotificationSocket>(notArray);
		for (NotificationSocket ns: c) {
			ns.terminate();
		}
	}
}
