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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Write data packets wrapped according to our pcket rules
 * @author woody
 *
 */
public class SCOutputStream
{
	private static final int MAXBUFFER = 4096;

	private int bufPos;
	private byte[] buffer;
	private OutputStream outStream;

	public SCOutputStream(OutputStream os)
	{
		outStream = os;
		buffer = new byte[MAXBUFFER];
	}

	public void close() throws IOException
	{
		outStream.close();
	}

	private void writeByte(byte b) throws IOException
	{
		buffer[bufPos++] = b;
		if (bufPos >= MAXBUFFER) {
			flush();
		}
	}

	private void writeWrappedByte(byte b) throws IOException
	{
		if ((b == 0) || (b == 1)) {
			writeByte((byte)1);
			writeByte((byte)(b+1));
		} else {
			writeByte(b);
		}
	}

	private void flush() throws IOException
	{
		outStream.write(buffer,0,bufPos);
		bufPos = 0;
	}

	public void writeData(byte[] data) throws IOException
	{
		byte checksum = SCChecksum.get().calcCRC8((byte) 0, data);

		for (byte b: data) writeWrappedByte(b);
		writeWrappedByte(checksum);
		writeByte((byte)0);
		flush();
	}
}
