//
//  SCKeychain.m
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

#import "SCKeychain.h"

/************************************************************************/
/*																		*/
/*	Constants															*/
/*																		*/
/************************************************************************/

#define SECSERVICENAME	@"SecureChat.iOS"

/************************************************************************/
/*																		*/
/*	Keychain Storage													*/
/*																		*/
/************************************************************************/

/*	SCHasSecureData
 *
 *		Returns true if we have a blob of saved secure data we can
 *	return.
 */

BOOL SCHasSecureData()
{
	NSDictionary *d;

	d = @{ (__bridge id)kSecClass: (__bridge id)kSecClassGenericPassword,
		   (__bridge id)kSecAttrService: SECSERVICENAME,
		   (__bridge id)kSecAttrAccessible: (__bridge id)kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
		   (__bridge id)kSecReturnAttributes: (__bridge id)kCFBooleanTrue };

	CFDictionaryRef *found = nil;
	SecItemCopyMatching((__bridge CFDictionaryRef)d, (CFTypeRef *)&found);

	if (!found) return false;
	return true;
}

/*	SCGetSecureData
 *
 *		Return the bob of secure data stored on the device. If the
 *	data is not found, return nil
 */

NSData *SCGetSecureData()
{
	NSDictionary *d;
	d = @{ (__bridge id)kSecClass: (__bridge id)kSecClassGenericPassword,
		   (__bridge id)kSecAttrService: SECSERVICENAME,
		   (__bridge id)kSecAttrAccessible: (__bridge id)kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
		   (__bridge id)kSecReturnAttributes: (__bridge id)kCFBooleanTrue,
		   (__bridge id)kSecReturnData: (__bridge id)kCFBooleanTrue };
	CFDictionaryRef found = nil;
	SecItemCopyMatching((__bridge CFDictionaryRef)d, (CFTypeRef *)&found);

	/*
	 *	If we have credentials, then try to log in. If the login process
	 *	fails, or if we don't have credentials, display a UIAlertView
	 *	asking for those credentials. (TODO: Should that be done as a
	 *	callback for MacOS X?) Then attempt to log in. 
	 */

	if (!found) {
		/*
		 *	No credentials.
		 */

		return nil;
	} else {
		/*
		 *	We found a username and password. Return them
		 */

		NSDictionary *data = (__bridge_transfer NSDictionary *)found;
		return data[(__bridge id)kSecValueData];
	}
}

/*	SCSaveSecureData
 *
 *		Save the secure data blob to the keychain
 */

void SCSaveSecureData(NSData *data)
{
	NSDictionary *d;

	d = @{ (__bridge id)kSecClass: (__bridge id)kSecClassGenericPassword,
		   (__bridge id)kSecAttrService: SECSERVICENAME,
		   (__bridge id)kSecAttrAccessible: (__bridge id)kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
		   (__bridge id)kSecReturnAttributes: (__bridge id)kCFBooleanTrue };
	SecItemDelete((__bridge CFDictionaryRef)d);

	// Create dictionary
	d = @{ (__bridge id)kSecClass: (__bridge id)kSecClassGenericPassword,
		   (__bridge id)kSecAttrService: SECSERVICENAME,
		   (__bridge id)kSecAttrAccessible: (__bridge id)kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
		   (__bridge id)kSecValueData: data };

	SecItemAdd((__bridge CFDictionaryRef)d, nil);
}

/*	JDClearLoginInformation
 *
 *		Clear the login information for this service
 */

void SCClearSecureData()
{
	NSDictionary *d;

	d = @{ (__bridge id)kSecClass: (__bridge id)kSecClassGenericPassword,
		   (__bridge id)kSecAttrService: SECSERVICENAME,
		   (__bridge id)kSecAttrAccessible: (__bridge id)kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
		   (__bridge id)kSecReturnAttributes: (__bridge id)kCFBooleanTrue };

	SecItemDelete((__bridge CFDictionaryRef)d);
}