//
//  SCRSAManager.h
//  SecureChat
//
//  Created by William Woody on 2/26/16.
//  Copyright © 2016 by William Edward Woody.
//

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

#import <Foundation/Foundation.h>

/************************************************************************/
/*																		*/
/*	RSA Encryption Wrapper												*/
/*																		*/
/************************************************************************/

/*	SCRSAManager
 *
 *		This wraps the C++ code to generate and maintain an RSA public and
 *	private key, and supports encoding and decoding blocks of data. This
 *	also provides a mechanism for storing the RSA data in the keychain in
 *	an encrypted format.
 *
 *		This also stores a UUID which is used to help identify this
 *	device remotely. The UUID is a randomly generated UUID key.
 */

@interface SCRSAManager : NSObject

+ (SCRSAManager *)shared;

/*
 *	Encode the data and save to the keychain. This is used to save the
 *	contents once we update one or more parameters, and must be called
 *	when we want to save changes made after generating a new RSA key,
 *	or updating the server or username/password pair
 */

- (void)encodeSecureData;

/*
 *	Clear stored data
 */

- (void)clear;

/*
 *	Can start the server connection. This returns true only when we have
 *	data loaded that can be used to start the server
 */

- (BOOL)canStartServices;

/*
 *	Set the passcode used to encrypt the data. This is used to generate an
 *	encryption key used to encrypt and decrypt the data stored away to
 *	represent our encryption block.
 *
 *	Will return NO if there is data stored and the passcode doesn't properly
 *	match.
 *
 *	Note: sometimes this may return YES even though the wrong passcode was
 *	entered. This is a deliberate design decision.
 */

- (BOOL)setPasscode:(NSString *)passcode;

/*
 *	Change passcode. There is a small chance this can go haywire
 */

- (BOOL)updatePasscode:(NSString *)passcode withOldPasscode:(NSString *)oldPasscode;

/*
 *	Returns true if an RSA key has already been generated
 */

- (BOOL)hasRSAKey;

/*
 *	Device UUID. Used to identify this device on the remote server for
 *	obtaining messages. This was generated with the RSA Key, and ties this
 *	item with the key
 */

- (NSString *)deviceUUID;

/*
 *	Back-end credentials
 */

- (NSString *)username;
- (NSString *)passwordHash;
- (NSString *)server;

- (void)setUsername:(NSString *)username passwordHash:(NSString *)passwordHash;
- (void)setServerUrl:(NSString *)server;

/*
 *	Generates an RSA key. This should be done on a background thread as this
 *	is computationally expensive.
 *
 *	We also generate a device UUID for device identification.
 */

- (BOOL)generateRSAKeyWithSize:(uint32_t)size;

/*
 *	Present public key for encryption. Returns nil if there was a problem,
 *	such as the public key not being available.
 */

- (NSString *)publicKey;

/*
 *	Decoder support for our RSA system. Note that this is a computationally
 *	expensive operation and must be performed on a background thread.
 */

- (NSData *)decodeData:(NSData *)data;

@end

