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

import java.math.BigInteger;

/**
 * SCRSAKey encodes a custom RSA key
 * @author woody
 *
 */
public class SCRSAKey
{
	private BigInteger e;
	private BigInteger m;
	private int n;
	
	/**
	 * Decode an RSA key pair (exp,size,mod) and convert into internal
	 * storage
	 * @param str
	 */
	public SCRSAKey(String str)
	{
		String[] split = str.split(",");
		e = new BigInteger(split[0]);
		n = Integer.parseInt(split[1]);
		m = new BigInteger(split[2]);
	}

	/**
	 * Generate RSA key from exponent and modulus, tracking the number of
	 * bits from which this was generated.
	 * @param nbits
	 * @param eval
	 * @param mval
	 */
	public SCRSAKey(int nbits, BigInteger eval, BigInteger mval)
	{
		n = nbits;
		e = eval;
		m = mval;
	}
	
	public String toString()
	{
		return e.toString() + "," + n + "," + m.toString();
	}
	
	public int getSize()
	{
		return n;
	}
	
	public BigInteger getExponent()
	{
		return e;
	}
	
	public BigInteger getModulus()
	{
		return m;
	}
	
	public BigInteger transform(BigInteger v)
	{
		return v.modPow(e, m);
	}
}
