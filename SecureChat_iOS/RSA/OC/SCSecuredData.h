//
//  SCSecuredData.h
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

/*
 *	Represents the data that is securely stored in the Apple iOS keychain.
 *	This data can be serialized as an NSData object (which happens to be
 *	an even 64-bit word in size, for Blowfish encrption and decryption)
 */

@interface SCSecuredData : NSObject

- (NSData *)serializeData;
+ (SCSecuredData *)deserializeData:(NSData *)data;

/*
 *	Fields representing the data stored as this secured data object
 */

@property (nonatomic, copy) NSString *uuid;
@property (nonatomic, copy) NSString *publicKey;
@property (nonatomic, copy) NSString *privateKey;

@property (nonatomic, copy) NSString *username;
@property (nonatomic, copy) NSString *password;
@property (nonatomic, copy) NSString *serverURL;


@end
