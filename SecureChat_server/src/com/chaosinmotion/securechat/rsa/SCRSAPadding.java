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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * SCRSAPadding provides the padding functionality of the similarly named
 * C++ class in the iOS/MacOS X client
 * @author woody
 *
 */
public class SCRSAPadding
{
	private int encSize;
	private int msgSize;

	/**
	 * Construct encoding with bit size. Assumption: bits divisible by 8.
	 * @param n Number of bits
	 */
	public SCRSAPadding(int n)
	{
		encSize = n/8;
		msgSize = (7 * encSize)/8;
	}
	
	public int getMessageSize()
	{
		return msgSize;
	}
	
	public int getEncodeSize()
	{
		return encSize;
	}
	
	/**
	 * Encode
	 * @throws NoSuchAlgorithmException 
	 */
	
	boolean encode(byte[] msg, byte[] enc) throws NoSuchAlgorithmException
	{
		if (msg.length != msgSize) throw new RuntimeException();
		if (enc.length != encSize) throw new RuntimeException();
		
		int i,j;
		int msgOffset = encSize - msgSize;
		
		/*
		 * Step 1: move message to bottom bytes of enc, calculate
		 * checksum
		 */
		
		System.arraycopy(msg, 0, enc, msgOffset, msgSize);
		
		/*
		 * Step 2: calculate a CRC checksum of the message
		 */
		enc[msgOffset-1] = SCChecksum.calcCRC8((byte)0, msg);
		
		/*
		 * Step 3: add random data
		 */
		
		SecureRandom sr = SecureRandom.getInstanceStrong();
		byte[] rbuf = new byte[msgOffset-1];
		sr.nextBytes(rbuf);
		System.arraycopy(rbuf, 0, enc, 0, rbuf.length);
		enc[0] &= 0x3F;				// zero out top bits
		
		/*
		 * Calculate G(r)
		 */
		
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        d.update(enc, 0, msgOffset-1);
        byte[] gMask = d.digest();
        d.reset();
        
        /*
         * Calculate G^m. Note the CRC is taken as part of the message
         */
        
    	j = 0;
    	for (i = msgOffset-1; i < encSize; ++i) {
    		enc[i] ^= gMask[j++];
    		if (j >= 32) j = 0;
    	}
    	
    	/*
    	 * Calculate h(m'). Note the CRC is part of the message
    	 */
    	
        d.update(enc, msgOffset-1, msgSize+1);
        byte[] hMask = d.digest();

        /*
         * Calculate r^H. Force top two bits to zero
         */
    	j = 0;
    	for (i = 0; i < msgOffset-1; ++i) {
    		enc[i] ^= hMask[j++];
    		if (j >= 32) j = 0;
    	}
    	enc[0] &= 0x3F;

		return true;
	}
	
	/**
	 * Packet decode
	 * @param enc
	 * @param msg
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	boolean decode(byte[] enc, byte[] msg) throws NoSuchAlgorithmException
	{
		if (msg.length != msgSize) throw new RuntimeException();
		if (enc.length != encSize) throw new RuntimeException();
		
		int i,j,k;
		int msgOffset = encSize - msgSize;
		
		/*
		 * Calculate H(m'). Note CRC is part of the message
		 */
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        d.update(enc, msgOffset-1, msgSize+1);
        byte[] hMask = d.digest();
        byte[] scratch = new byte[hMask.length];
        
        /*
         * Calculate G(r). Done without using more than nominal scratch
         */
        d.reset();
    	j = 0;
    	for (i = 0; i < msgOffset-1; ++i) {
    		scratch[j] = (byte)(hMask[j] ^ enc[i]);
    		++j;

    		if (i == 0) scratch[i] &= 0x3F;			// top two bits force 0
    		if (j >= 32) {
    			d.update(scratch);
    			j = 0;
    		}
    	}
    	if (j > 0) {
    		d.update(scratch, 0, j);
    	}
    	byte[] gMask = d.digest();
    	
    	/*
    	 * Calculate crc, message
    	 */
    	
    	k = 0;
    	j = 0;
    	i = msgOffset - 1;
    	byte crc = (byte)(enc[i++] ^ gMask[j++]);
    	while (i < encSize) {
    		msg[k++] = (byte)(enc[i++] ^ gMask[j++]);
    		if (j >= 32) j = 0;
    	}
    	
    	/*
    	 * Validate CRC8
    	 */

		return SCChecksum.calcCRC8((byte)0, msg) == crc;
	}
}
