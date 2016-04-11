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

import com.chaosinmotion.securechat.rsa.SCChecksum;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads from the input stream, calling a callback when each data packet
 * is successfully extracted. Note that this should be run in a separate
 * thread, in that this will not exit the process loop until we see the end
 * of the input stream.
 * @author woody
 *
 */
public abstract class SCInputStream
{
	private static final int MAXSIZE = 4096;

	private InputStream inStream;
	private byte[] buffer;
	private int bufpos;
	private int buflen;

	/**
	 * Input stream constructor
	 * @param is
	 */
	public SCInputStream(InputStream is)
	{
		inStream = is;
		buffer = new byte[MAXSIZE];
	}

	/**
	 * Internal read next byte from stream.
	 * @return next byte
	 * @throws IOException n
	 */
	private int nextByte() throws IOException
	{
		if (bufpos >= buflen) {
			bufpos = 0;
			buflen = inStream.read(buffer);
			if (buflen == -1) return -1;			// end of stream
		}

		return 0x00FF & buffer[bufpos++];
	}

	/**
	 * This will not exit until we reach the end of the stream. Thus, this
	 * should be called in a separate thread.
	 * @throws IOException
	 */
	public void processStream() throws IOException
	{
		byte lastByte = 0;
		boolean startFlag = false;
		boolean nextFlag = false;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		/*
		 *	Run the stream, identifying 0x01 and 0x00.
		 */
		for (;;) {
			int b = nextByte();
			if (b == -1) return;		// end of stream.

			if (b == 0) {
				/*
				 * Dump the processed buffer. We never escape 0.
				 */

				processBuffer(baos.toByteArray(),lastByte);
				baos.reset();
				nextFlag = false;
				startFlag = false;
				lastByte = 0;

			} else if (nextFlag) {
				if (startFlag) {
					baos.write(lastByte);
				} else {
					startFlag = true;
				}
				lastByte = (byte)(b - 1);
				nextFlag = false;

			} else if (b == 1) {
				nextFlag = true;

			} else {
				if (startFlag) {
					baos.write(lastByte);
				} else {
					startFlag = true;
				}
				lastByte = (byte)b;
			}
		}
	}

	/**
	 * validates checksum. If passed, passes the buffer upwards for
	 * further processing
	 * @param bytes
	 * @param checksum
	 */
	private void processBuffer(byte[] bytes, byte checksum)
	{
		byte c = SCChecksum.get().calcCRC8((byte)0, bytes);
		if (c == checksum) processPacket(bytes);
	}

	public abstract void processPacket(byte[] data);

	/**
	 * Close underlying stream.
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		inStream.close();
	}
}
