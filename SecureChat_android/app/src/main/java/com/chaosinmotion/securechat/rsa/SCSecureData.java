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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * This is our storage class used to store our server connection, public
 * and private key, and device identifiers. This class simply allows us
 * to quickly serialize the data and validate with a (weak) checksum.
 *
 * Because this is device specific, we don't need to make this a portable
 * data object.
 *
 * Created by woody on 4/8/16.
 */
public class SCSecureData
{
	public String uuid;
	public String publicKey;
	public String privateKey;
	public String username;
	public String password;
	public String serverURL;

	private static void writeString(DataOutputStream dos, String str) throws IOException
	{
		if (str == null) str = "";
		dos.writeUTF(str);
	}

	public byte[] serializeData() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);

		writeString(dos,uuid);
		writeString(dos,publicKey);
		writeString(dos,privateKey);
		writeString(dos,username);
		writeString(dos,password);
		writeString(dos,serverURL);
		dos.flush();

		// pad to 8 byte alignment
		int len = baos.size() + 1;
		if (0 != (len % 8)) {
			len = 8 - len % 8;
			for (int i = 0; i < len; ++i) baos.write(0);
		}

		byte[] array = baos.toByteArray();
		baos.write(0x00FF & SCChecksum.get().calcCRC8((byte)0,array));
		return baos.toByteArray();
	}

	/**
	 * Load the contents. Validates checksum then deserializes the
	 * strings. If there is a problem we plough ahead anyway.
	 * @param b
	 * @return
	 */
	public static SCSecureData deserializeData(byte[] b)
	{
		byte csum = SCChecksum.get().calcCRC8((byte)0,b,0,b.length-1);
		if (csum != b[b.length-1]) return null;

		SCSecureData retval = new SCSecureData();
		retval.uuid = "";
		retval.publicKey = "";
		retval.privateKey = "";
		retval.username = "";
		retval.password = "";
		retval.serverURL = "";

		ByteArrayInputStream bais = new ByteArrayInputStream(b);
		DataInputStream dis = new DataInputStream(bais);

		try {
			retval.uuid = dis.readUTF();
			retval.publicKey = dis.readUTF();
			retval.privateKey = dis.readUTF();
			retval.username = dis.readUTF();
			retval.password = dis.readUTF();
			retval.serverURL = dis.readUTF();
		}
		catch (IOException err) {
			// skip
		}

		return retval;
	}
}
