/*	TestSSLServer.java
 * 
 *		SecureChat Server Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.securechat;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Properties;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import com.chaosinmotion.securechat.server.config.Config;

/**
 * @author woody
 *
 */
public class TestSSLServer
{
	private ServerSocket socket;
	private boolean canHalt;
	
	private void testConnection(Socket s)
	{
		try {
			InputStream is = s.getInputStream();
			OutputStream os = s.getOutputStream();
			
			for (;;) {
				int ch;
				ch = is.read();
				if (ch == -1) {
					is.close();
					os.close();
					return;
				}
				os.write(ch);
				if (ch == 'c') {
					canHalt = true;
					notifyAll();
				}
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void testSocket()
	{
		// spin, waiting for connections. When we get a connection that enters
		// 'c', we kill this.
		for (;;) {
			Socket s;
			try {
				s = socket.accept();
				if (s != null) {
					Thread thread = new Thread() {
						@Override
						public void run()
						{
							testConnection(s);
						}
					};
					thread.setDaemon(true);
					thread.start();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void openServer() throws Exception
	{
		Properties p = Config.get();
		String keystore = p.getProperty("keystorefile");
		String password = p.getProperty("keystorepassword");

		int port = 12345;
		
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
		canHalt = false;
		
		Thread thread = new Thread() {
			@Override
			public void run()
			{
				testSocket();
			}
		};
		thread.setDaemon(true);
		thread.start();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		TestSSLServer test = new TestSSLServer();
		
		try {
			test.openServer();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		
		synchronized(test) {
			while (!test.canHalt) {
				try {
					test.wait();
				}
				catch (InterruptedException e) {
				}
			}
		}
	}
	
}
