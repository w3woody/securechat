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

package com.chaosinmotion.securechat.rsa;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This is the equivalent of the SCRSAManager class on iOS. This contains
 * the code for managing the credentials and settings, as well as decoding
 * messages received by this device.
 *
 * Created by woody on 4/8/16.
 */
public class SCRSAManager
{
	private static final String SECUREFILE = "securechat.ks";

	private byte[] passHash;
	private SCRSAKey privateRSAKey;
	private String deviceIdentifier;
	private String publicRSAKey;
	private String username;
	private String passwordHash;
	private String server;

	private static SCRSAManager sharedManager;

	/************************************************************************/
	/*																		*/
	/*	Startup/Shutdown													*/
	/*																		*/
	/************************************************************************/

	private SCRSAManager()
	{
		privateRSAKey = null;
		passHash = new byte[32];
		deviceIdentifier = null;
	}

	public synchronized static SCRSAManager shared()
	{
		if (sharedManager == null) {
			sharedManager = new SCRSAManager();
		}
		return sharedManager;
	}

	/************************************************************************/
	/*																		*/
	/*	Utilities          													*/
	/*																		*/
	/************************************************************************/

	/**
	 * Load the data from the input stream, returning as a byte array
	 * @param fis
	 * @return
	 * @throws IOException
	 */
	private byte[] loadData(FileInputStream fis) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[256];
		int len;

		while (0 < (len = fis.read(buffer))) {
			baos.write(buffer,0,len);
		}
		return baos.toByteArray();
	}

	/************************************************************************/
	/*																		*/
	/*	Access          													*/
	/*																		*/
	/************************************************************************/

	/**
	 * Returns true if we have a secure key loaded into memory. Used to
	 * determine if we need to log in by setting the passcode
	 * @return
	 */
	public boolean hasRSAKey()
	{
		return publicRSAKey != null;
	}

	/**
	 * Returns true if we have information loaded that we can use to
	 * access the server
	 * @return
	 */
	public boolean canStartServices()
	{
		return server != null;
	}

	/**
	 * Set the passcode. If the data buffer exists, this decrypts the
	 * bufer and unrolls the data, returning NO if that fails. Otherwise
	 * this stores the encryption representation of the passcode for
	 * further use.
	 * @param passcode
	 * @param ctx The application context used to retrieve private storage data
	 * @return
	 */
	public boolean setPasscode(String passcode, Context ctx)
	{
		/*
		 * Encode passcode to hash
		 */
		try {
			MessageDigest d = MessageDigest.getInstance("SHA-256");
			byte[] data = passcode.getBytes("UTF-8");
			d.update(data);
			passHash = d.digest();
			d.reset();
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		try {
			/*
			 * Load the stored data
			 */
			FileInputStream fis = ctx.openFileInput(SECUREFILE);
			byte[] buffer = loadData(fis);

			SCBlowfish bf = new SCBlowfish(passHash);

			/* TODO: Finish */
			return false;
		}
		catch (FileNotFoundException e) {
			// No storage, so just store passcode and return
			return true;
		}
		catch (IOException e) {
			// Problem loading. Should not happen. Kills file and starts over
			ctx.deleteFile(SECUREFILE);
			return true;
		}
	}
}
