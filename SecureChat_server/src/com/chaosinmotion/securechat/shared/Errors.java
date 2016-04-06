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
 * Gives the list of error codes that can be returned by the server. If
 * the return result is in error, an error field will be returned which
 * gives one of the following error codes. An error code field is not 
 * present if there is no error.
 */
public interface Errors
{
	/**
	 * The error code returned if there was an exception on the server. This
	 * is the only error which will also present an exception stack for
	 * debugging purposes.
	 */
	public static final int	ERROR_EXCEPTION	= 1;
	
	/**
	 * This error code is returned if the username/password pair could not be
	 * validated against the back end.
	 */
	public static final int ERROR_LOGIN = 2;

	/**
	 * This is returned if there is an internal unexpected problem. This can
	 * happen during onboarding, for example, if the user attempts to pick a
	 * username which is already in use.
	 */
	public static final int ERROR_INTERNAL = 3;

	/**
	 * This error code is returned if the user is attempting to perform a
	 * command that requires the user to be authenticated.
	 */
	public static final int ERROR_UNAUTHORIZED = 4;

	/**
	 * Unable to create a new account because the username is already in use.
	 */
	public static final int ERROR_DUPLICATEUSER = 5;

	/**
	 * Unknown device ID
	 */
	public static final int ERROR_UNKNOWNDEVICE = 6;

	/**
	 * Notification service is not running.
	 */
	public static final int ERROR_NOTIFICATION = 7;
	
	/**
	 * User unknown
	 */
	public static final int ERROR_UNKNOWNUSER = 8;
}
