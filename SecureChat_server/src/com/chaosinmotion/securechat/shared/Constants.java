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

package com.chaosinmotion.securechat.shared;

/**
 *	Contains various common constants
 */
public interface Constants
{
	/**
	 * SALT is the salt added to the password hash and token
	 */
	String SALT = "PEnSalt194";

	/**
	 * SALT2 provides the hash we use when we're hashing a password for
	 * storage in our database.
	 */
	String SALT2 = "PwdSalt134";

	/**
	 * SALT3 is used to salt messages for removal checksums
	 */
	String SALT3 = "PmsgzhD";
}
