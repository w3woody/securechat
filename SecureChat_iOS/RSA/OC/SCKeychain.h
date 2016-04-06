//
//  SCKeychain.h
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
/*	Keychain Storage													*/
/*																		*/
/************************************************************************/

#ifdef __cplusplus
extern "C" {
#endif

/*
 *	We do not rely on the security of the Apple Keychain, though we 
 *	use it as an extra layer of security. The idea of this app is that
 *	we treat the keychain as if a future version of the iOS operating
 *	system will be compromised by the government, and a backdoor will
 *	be provided that allows us to obtain the contents of our data.
 *
 *	That said, at present the Keychain is secure, so it does provide an
 *	extra layer of security.
 *
 *	The methods here provide us a way to store a blob of data in a
 *	place which is separate from the directory storage area, which is
 *	preserved on the device, which is not easily accessable. (And of
 *	course on top we use Blowfish to encrypt that data using the app
 *	password.
 */

/*	SCHasSecureData
 *
 *		Returns true if we have a blob of saved secure data we can
 *	return.
 */

extern BOOL SCHasSecureData();

/*	SCGetSecureData
 *
 *		Return the bob of secure data stored on the device. If the
 *	data is not found, return nil
 */

extern NSData *SCGetSecureData();


/*	SCSaveSecureData
 *
 *		Save the secure data blob to the keychain
 */

extern void SCSaveSecureData(NSData *data);

/*	SCClearSecureData
 *
 *		Clear the secure data blob in the keychain
 */

extern void SCClearSecureData(void);

#ifdef __cplusplus
} // extern "C"
#endif
