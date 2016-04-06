//
//  SCRSAEncoder.h
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

/*	SCRSAEncoder
 *
 *		This uses the encoder key (returned by SCRSAManager) to encode
 *	data for transmission.
 */

@interface SCRSAEncoder : NSObject

/*
 *	Create a new encoder with the encoder key provided. At a higher level
 *	we would create a dictionary of these objects associated with a
 *	given user ID, so we can encrypt across multiple devices for a given
 *	user.
 */

- (id)initWithEncoderKey:(NSString *)publicKey;

/*
 *	Encode data with this public key. Note the process is computationally
 *	expensive and must be run in a background thread
 */

- (NSData *)encodeData:(NSData *)data;

@end
