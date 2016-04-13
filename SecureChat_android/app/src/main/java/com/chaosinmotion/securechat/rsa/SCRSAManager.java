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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

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
	 * @param fis Input stream
	 * @return Data loaded from input stream
	 * @throws IOException
	 */
	private byte[] loadData(InputStream fis) throws IOException
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
	 * @return True if we have a public RSA key loaded into memory
	 */
	public boolean hasRSAKey()
	{
		return publicRSAKey != null;
	}

	/**
	 * Returns true if we have information loaded that we can use to
	 * access the server
	 * @return True if we have a server record, allowing us to connect to
	 * a remote server
	 */
	public boolean canStartServices()
	{
		return server != null;
	}

	public boolean hasSecureData(Context ctx)
	{
		File f = ctx.getFileStreamPath(SECUREFILE);
		return f.exists();
	}

	/**
	 * Set the passcode. If the data buffer exists, this decrypts the
	 * bufer and unrolls the data, returning NO if that fails. Otherwise
	 * this stores the encryption representation of the passcode for
	 * further use.
	 * @param passcode The new passcode to set
	 * @param ctx The application context used to retrieve private storage data
	 * @return True if we were able to set the passcode. If an existing
	 * storage buffer exists, this will validate against that buffer
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
		catch (Exception e) {
			// Should never happen; result of incorrectly set parameters
		}

		try {
			/*
			 * Load the stored data
			 */
			FileInputStream fis = ctx.openFileInput(SECUREFILE);
			byte[] buffer = loadData(fis);
			fis.close();

			if ((buffer.length % 8) != 0) {
				ctx.deleteFile(SECUREFILE);
				return true;        // zero out
			}

			SCBlowfish bf = new SCBlowfish(passHash);
			bf.decryptData(buffer);
			SCSecureData sdata = SCSecureData.deserializeData(buffer);
			if (sdata == null) {
				// decryption failed; incorrect key?
				return false;
			}

			/*
			 *  Carry data across
			 */

			publicRSAKey = sdata.publicKey;
			privateRSAKey = new SCRSAKey(sdata.privateKey);
			deviceIdentifier = sdata.uuid;
			server = sdata.serverURL;
			username = sdata.username;
			passwordHash = sdata.password;

			/*
			 *  And push back to secure store
			 */

			encodeSecureData(ctx);

			return true;
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

	/**
	 * Used to change the passcode for an existing secure data object.
	 * This will fail if there is no stored secure data. If there is, this
	 * will decode the stored secure data and re-encrypt using the new
	 * passcode.
	 * @param passcode The new passcode
	 * @param oldPasscode The old passcode
	 * @param ctx The context for file operations
	 * @return True if we were able to change the passcode.
	 */
	public boolean updatePasscode(String passcode, String oldPasscode, Context ctx)
	{
		byte[] data;

		/*
		 * Load the stored data. If this fails, assume we don't have
		 * any data, and bail with failure.
		 */
		try {
			FileInputStream fis = ctx.openFileInput(SECUREFILE);
			data = loadData(fis);
			fis.close();
		}
		catch (IOException ex) {
			return false;
		}

		/*
		 *  Encode the old, new passcodes
		 */
		byte[] oldHash;
		byte[] newHash;
		try {
			MessageDigest d = MessageDigest.getInstance("SHA-256");
			byte[] p = passcode.getBytes("UTF-8");
			d.update(p);
			newHash = d.digest();
			d.reset();
			p = oldPasscode.getBytes("UTF-8");
			d.update(p);
			oldHash = d.digest();
			d.reset();
		}
		catch (Exception e) {
			// Should never happen; result of incorrectly set parameters
			return false;
		}

		/*
		 *  Now attempt to decrypt the data using the old passcode
		 */

		SCBlowfish bf = new SCBlowfish(oldHash);
		bf.decryptData(data);

		SCSecureData sdata = SCSecureData.deserializeData(data);
		if (sdata == null) return false;    // wrong passcode.

		/*
		 *  Carry the data across into me
		 */

		publicRSAKey = sdata.publicKey;
		privateRSAKey = new SCRSAKey(sdata.privateKey);
		deviceIdentifier = sdata.uuid;
		server = sdata.serverURL;
		username = sdata.username;
		passwordHash = sdata.password;

		/*
		 *  Carry new hash and save
		 */

		passHash = newHash;
		encodeSecureData(ctx);
		return true;
	}

	/************************************************************************/
	/*																		*/
	/*	Obtain Contents          											*/
	/*																		*/
	/************************************************************************/

	public String getDeviceUUID()
	{
		return deviceIdentifier;
	}

	public String getPublicKey()
	{
		return publicRSAKey;
	}

	/************************************************************************/
	/*																		*/
	/*	Decryption Support         											*/
	/*																		*/
	/************************************************************************/

	/**
	 * Decode data. This is computationally expensive so should be done on
	 * a background thread.
	 * @param data The encoded data block
	 * @return The decoded data, or null if there was a problem.
	 */
	public byte[] decodeData(byte[] data)
	{
		if (privateRSAKey == null) {
			return null;
		}

		/*
		 *  Perform decryption process. This preallocates a chunk of
		 *  memory which is of the necessary size
		 */

		SCRSAPadding padding = new SCRSAPadding(privateRSAKey.getSize());
		int msgSize = padding.getMessageSize();
		int blockSize = padding.getEncodeSize();
		int encSize = blockSize / 4;	// in 32-bit words

		int blockLength = data.length;
		blockLength /= padding.getEncodeSize();
		if (blockLength < 1) {
			return null;
		}

		byte[] decode = new byte[blockLength * msgSize];
		byte[] encBuffer = new byte[blockSize];
		byte[] msgBuffer = new byte[msgSize];

		for (int i = 0; i < blockLength; ++i) {
			// Move the next block into enc buffer
			System.arraycopy(data,i*blockSize,encBuffer,0,blockSize);

			// RSA Transform
			BigInteger bi = new BigInteger(encBuffer);
			BigInteger ei = privateRSAKey.transform(bi);
			byte[] dec = ei.toByteArray();

			// Decode
			if (!padding.decode(dec,msgBuffer)) {
				/*
				 *  Failure decoding data; checksum validation. Simply bail
				 */
				return null;
			}

			// Move into next block
			System.arraycopy(msgBuffer,0,decode,i * msgSize, msgSize);
		}

		/*
		 *  Now get the actual size (which is the first 4 bytes of the
		 *  decoded block) and move the bytes down and return.
		 */

		int len = 0;
		for (int i = 0; i < 4; ++i) {
			len = (len << 8) | (0x00FF & decode[i]);
		}
		if (len > decode.length-4) len = decode.length-4;

		byte[] ret = new byte[len];
		System.arraycopy(decode,4,ret,0,len);

		return ret;
	}

	/************************************************************************/
	/*																		*/
	/*	Key Generation          											*/
	/*																		*/
	/************************************************************************/

	public String getUsername()
	{
		return username;
	}

	public String getPasswordHash()
	{
		return passwordHash;
	}

	public String getServer()
	{
		return server;
	}

	public void setCredentials(String uname, String phash)
	{
		username = uname;
		passwordHash = phash;
	}

	public void setServerURL(String s)
	{
		server = s;
	}

	/**
	 * Generate the RSA key and device UUID. This should be called in a
	 * background thread as it requires significant time to execute.
	 * @param size The RSA key size to use. (Should be a power of 2 greater
	 *             than or equal to 128 bits.)
	 * @return True if successful, false if problem (such as no passcode
	 * set)
	 */
	public boolean generateRSAKeyWithSize(int size)
	{
		if (passHash == null) return false;

		/* Generate RSA keys */
		SCRSAGenerator.Pair p = SCRSAGenerator.generateKeyPair(size);
		publicRSAKey = p.getPublicKey().toString();
		privateRSAKey = p.getPrivateKey();

		/* Generate UUID */
		deviceIdentifier = SCUUIDGenerator.generateUUID();

		return true;
	}

	/**
	 * Save our state to the secure store
	 * @param ctx Context to use for file operations
	 */

	public synchronized void encodeSecureData(Context ctx)
	{
		if (passHash == null) return;

		SCSecureData sdata = new SCSecureData();

		sdata.uuid = deviceIdentifier;
		sdata.publicKey = publicRSAKey;
		sdata.privateKey = privateRSAKey.toString();
		sdata.username = username;
		sdata.password = passwordHash;
		sdata.serverURL = server;

		/*
		 * Now encrypt
		 */

		SCBlowfish bf = new SCBlowfish(passHash);
		byte[] data = sdata.serializeData();
		bf.encryptData(data);

		/*
		 * And write to secure store
		 */

		try {
			FileOutputStream fos = ctx.openFileOutput(SECUREFILE,Context.MODE_PRIVATE);
			fos.write(data);
			fos.close();
		}
		catch (IOException e) {
			// Ignore; normally should not happen. (TODO: how should I handle
			// errors???
		}
	}

	/**
	 * Clear the contents of this manager. Done when we log out
	 * @param ctx Context to use for file operations
	 */
	public void clear(Context ctx)
	{
		ctx.deleteFile(SECUREFILE);

		passHash = null;
		deviceIdentifier = null;
		publicRSAKey = null;
		username = null;
		passHash = null;
		server = null;
		privateRSAKey = null;
	}
}
