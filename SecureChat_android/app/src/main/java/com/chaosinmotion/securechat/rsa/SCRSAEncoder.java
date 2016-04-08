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

package com.chaosinmotion.securechat.rsa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Encoder utility. Takes a public key and encrypts the data. Unlike the
 * C++ counterpart, this operates synchronously.
 * @author woody
 *
 */
public class SCRSAEncoder
{
	private SCRSAKey publicRSAKey;
	
	public SCRSAEncoder(String publicKey)
	{
		publicRSAKey = new SCRSAKey(publicKey);
	}
	
	/**
	 * Encode data using the public key provided when this was initialized
	 * @param data
	 * @return
	 * @throws NoSuchAlgorithmException 
	 * @throws IOException 
	 */
	public byte[] encodeData(byte[] data) throws NoSuchAlgorithmException, IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		/*
		 * Perform encryption process. First set up internal buffers
		 */
		
		SCRSAPadding padding = new SCRSAPadding(publicRSAKey.getSize());
		int msgSize = padding.getMessageSize();
		int encSize = padding.getEncodeSize();
		byte[] msgBuffer = new byte[msgSize];
		byte[] encBuffer = new byte[encSize];
		
		int len = data.length;
		int msgPos = 0;
		int dataPos = 0;
		
		/*
		 * Prepend 4 bytes for length. Assumes message size is bigger than
		 * 4 bytes
		 */

		msgBuffer[msgPos++] = (byte)(len >> 24);
		msgBuffer[msgPos++] = (byte)(len >> 16);
		msgBuffer[msgPos++] = (byte)(len >> 8);
		msgBuffer[msgPos++] = (byte)(len);

		/*
		 *	Now run the rest of the data through
		 */

		while (dataPos < len) {
			msgBuffer[msgPos++] = data[dataPos++];
			if (msgPos >= padding.getMessageSize()) {
				msgPos = 0;

				/*
				 *	Encode the buffer and encrypt it
				 */

				padding.encode(msgBuffer, encBuffer);
				
				/*
				 * 	Convert to an integer and transform
				 */
				BigInteger bi = new BigInteger(encBuffer);
				BigInteger ei = publicRSAKey.transform(bi);
				byte[] enc = ei.toByteArray();
				Arrays.fill(encBuffer, (byte)0);
				System.arraycopy(enc, encSize - enc.length, encBuffer, 0, enc.length);
				
				baos.write(encBuffer);
			}
		}

		if (msgPos > 0) {
			// zero tail of buffer
			for (int p = msgPos; p < msgSize; ++p) msgBuffer[p] = 0;

			/*
			 *	Encode the buffer and encrypt it
			 */

			padding.encode(msgBuffer, encBuffer);

			BigInteger bi = new BigInteger(encBuffer);
			BigInteger ei = publicRSAKey.transform(bi);
			byte[] enc = ei.toByteArray();
			Arrays.fill(encBuffer, (byte)0);
			System.arraycopy(enc, encSize - enc.length, encBuffer, 0, enc.length);
			
			baos.write(encBuffer);
		}

		/*
		 *	Completed encryption process. Free scratch buffers and return
		 *	our built data
		 */

		return baos.toByteArray();
	}
}
