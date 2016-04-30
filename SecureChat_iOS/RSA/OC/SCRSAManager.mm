//
//  SCRSAEncryption.mm
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

#import "SCRSAManager.h"
#import "SCKeychain.h"

#include "SCRSAEncryption.h"
#include "SCSecureHash.h"
#include "SCBlowfish.h"
#include "SCSecuredData.h"
#include "SCUUIDGenerator.h"

#include <string.h>

/************************************************************************/
/*																		*/
/*	Internals															*/
/*																		*/
/************************************************************************/

@interface SCRSAManager ()
{
	SCRSAKey *privateRSAKey;
	uint8_t passHash[32];
}

@property (copy) NSString *deviceIdentifier;
@property (copy) NSString *publicRSAKey;

@property (copy) NSString *username;
@property (copy) NSString *passwordHash;
@property (copy) NSString *server;

@end


/************************************************************************/
/*																		*/
/*	Startup/Shutdown													*/
/*																		*/
/************************************************************************/

@implementation SCRSAManager

+ (SCRSAManager *)shared;
{
	static SCRSAManager *manager;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		manager = [[SCRSAManager alloc] init];
	});
	return manager;
}

- (id)init
{
	if (nil != (self = [super init])) {
		privateRSAKey = nil;
		memset(passHash,0,sizeof(passHash));
		self.deviceIdentifier = nil;
	}
	return self;
}

- (void)dealloc
{
	if (privateRSAKey) delete privateRSAKey;
}

/************************************************************************/
/*																		*/
/*	Access																*/
/*																		*/
/************************************************************************/

/*
 *	Returns true if we have a secure key loaded into memory. Used to
 *	determine if we need to log in by setting the passcode.
 */

- (BOOL)hasRSAKey
{
	return self.publicRSAKey != nil;
}

/*
 *	Returns true if we have information loaded that we can use to
 *	access the server
 */

- (BOOL)canStartServices
{
	return SCHasSecureData() && (self.server != nil);
}

/*
 *	Set the passcode. If a data buffer exists, this decrypts the buffer
 *	and attempts to unroll the data, returning NO if that fails.
 *	Otherwise this stores the encryption representation of the passcode
 *	for futher use
 */

- (BOOL)setPasscode:(NSString *)passcode
{
	SCSHA256Context ctx;
	NSData *data = [passcode dataUsingEncoding:NSUTF8StringEncoding];

	/*
	 *	Encode passcode to hash
	 */

	ctx.Start();
	ctx.Update(data.length, (uint8_t *)data.bytes);
	ctx.Finish(passHash);

	if (SCHasSecureData()) {
		/*
		 *	Obtain the data from the keychain and decrypt
		 */

		SCBlowfish bf(sizeof(passHash),passHash);

		data = SCGetSecureData();
		size_t nblocks = data.length/8;
		uint32_t *enc = (uint32_t *)malloc(sizeof(uint32_t) * 2 * nblocks);
		memmove(enc, data.bytes, nblocks * sizeof(uint32_t) * 2);

		bf.DecryptData(enc, nblocks);
		data = [[NSData alloc] initWithBytesNoCopy:enc length:nblocks * sizeof(uint32_t) * 2 freeWhenDone:YES];

		/*
		 *	Now convert.
		 */

		SCSecuredData *sdata = [SCSecuredData deserializeData:data];
		if (sdata == nil) return NO;

		/*
		 *	Carry data across
		 */

		self.publicRSAKey = sdata.publicKey;
		privateRSAKey = new SCRSAKey([sdata.privateKey UTF8String]);
		self.deviceIdentifier = sdata.uuid;
		self.server = sdata.serverURL;
		self.username = sdata.username;
		self.passwordHash = sdata.password;

		/*
		 *	And push the data back to the secure store
		 */

		[self encodeSecureData];

		return YES;

	} else {
		return YES;
	}
}

/*
 *	Updates the passcode.
 */

- (BOOL)updatePasscode:(NSString *)passcode withOldPasscode:(NSString *)oldPasscode;
{
	uint8_t oldHash[32];
	uint8_t newHash[32];

	if (!SCHasSecureData()) return NO;

	/*
	 *	Encode the old, new passcodes
	 */

	NSData *data;

	SCSHA256Context ctx;

	data = [oldPasscode dataUsingEncoding:NSUTF8StringEncoding];
	ctx.Start();
	ctx.Update(data.length, (uint8_t *)data.bytes);
	ctx.Finish(oldHash);

	data = [passcode dataUsingEncoding:NSUTF8StringEncoding];
	ctx.Start();
	ctx.Update(data.length, (uint8_t *)data.bytes);
	ctx.Finish(newHash);

	/*
	 *	Now obtain the data from the keychain and decrypt. If we fail,
	 *	then throw information. Note we also update our own state; this
	 *	is a deliberate design decision to screw with the user if they
	 *	type in the wrong code.
	 *
	 *	Note we cannot use the method above, because the method above
	 *	will replace the hash, and we don't necessarily want to penalize
	 *	the user here.
	 */

	SCBlowfish bf(sizeof(oldHash),oldHash);

	data = SCGetSecureData();
	size_t nblocks = data.length/8;
	uint32_t *enc = (uint32_t *)malloc(sizeof(uint32_t) * 2 * nblocks);
	memmove(enc, data.bytes, nblocks * sizeof(uint32_t) * 2);

	bf.DecryptData(enc, nblocks);
	data = [[NSData alloc] initWithBytesNoCopy:enc length:nblocks * sizeof(uint32_t) * 2 freeWhenDone:YES];

	SCSecuredData *sdata = [SCSecuredData deserializeData:data];
	if (sdata == nil) return NO;

	/*
	 *	Carry data across
	 */

	self.publicRSAKey = sdata.publicKey;
	privateRSAKey = new SCRSAKey([sdata.privateKey UTF8String]);
	self.deviceIdentifier = sdata.uuid;
	self.server = sdata.serverURL;
	self.username = sdata.username;
	self.passwordHash = sdata.password;

	/*
	 *	Encode with new hash
	 */

	memcpy(passHash, newHash, sizeof(newHash));
	[self encodeSecureData];

	return YES;
}

/************************************************************************/
/*																		*/
/*	Obtain Contents														*/
/*																		*/
/************************************************************************/

- (NSString *)deviceUUID
{
	return self.deviceIdentifier;
}

- (NSString *)publicKey
{
	return self.publicRSAKey;
}

/************************************************************************/
/*																		*/
/*	Decryption Support													*/
/*																		*/
/************************************************************************/

/*
 *	Decode data
 */

- (NSData *)decodeData:(NSData *)data
{
	if (privateRSAKey == nil) {
		return nil;
	}

	/*
	 *	Perform decryption process. This preallocates a chunk of 
	 *	memory which is the necessary size
	 */

	SCRSAPadding padding(privateRSAKey->Size());
	size_t msgSize = padding.GetMessageSize();
	size_t blockSize = padding.GetEncodeSize();
	size_t encSize = blockSize / sizeof(BIWORD);	// in 32-bit words

	size_t blockLength = data.length;
	const uint8_t *blockData = (uint8_t *)data.bytes;
	blockLength /= padding.GetEncodeSize();
	if (blockLength < 1) {
		return nil;
	}

	uint8_t *decode = (uint8_t *)malloc(blockLength * msgSize);
	BIWORD *encBuffer = (BIWORD *)malloc(blockSize);
	uint8_t *msgBuffer = (uint8_t *)malloc(msgSize);

	/*
	 *	Decode all the buffers
	 */

	for (uint32_t i = 0; i < blockLength; ++i) {
		/*
		 *	Move the next block into the enc buffer
		 */

		memmove(encBuffer, blockData + i * blockSize, blockSize);

		// Spin the bits in each word from network order.
		// (I.E.: we have a stream of bytes but we want them
		// flipped to deal with the host order for long words)
		for (uint32_t i = 0; i < encSize; ++i) {
			encBuffer[i] = ntohs(encBuffer[i]);
//			encBuffer[i] = ntohl(encBuffer[i]);
		}
		// Spin the bytes. That's because our padding puts the MSB
		// at word 0, and our SCBigInteger implementation wants the
		// MSB at word n-1
		for (size_t i = 0, j = encSize-1; i < j; ++i, --j) {
			BIWORD tmp = encBuffer[i];
			encBuffer[i] = encBuffer[j];
			encBuffer[j] = tmp;
		}

		// RSA transform
		SCBigInteger bi(encBuffer,(size_t)encSize);
		SCBigInteger ei = privateRSAKey->Transform(bi);
		memcpy(encBuffer,ei.GetData(),blockSize);

		// Spin the bytes
		for (size_t i = 0, j = encSize-1; i < j; ++i, --j) {
			BIWORD tmp = encBuffer[i];
			encBuffer[i] = encBuffer[j];
			encBuffer[j] = tmp;
		}
		
		// Spin the bits back to network order and write the data
		for (uint32_t i = 0; i < encSize; ++i) {
//			encBuffer[i] = htonl(encBuffer[i]);
			encBuffer[i] = htons(encBuffer[i]);
		}

		// Decode the padded buffer
		if (!padding.Decode((uint8_t *)encBuffer, msgBuffer)) {
			/*
			 *	Failure decoding buffer; checksum violation. We
			 *	simply bail
			 */

			NSLog(@"Checksum failure");
			free(encBuffer);
			free(msgBuffer);
			free(decode);
			return nil;
		}

		// Move into next block
		memmove(decode + i * msgSize, msgBuffer, msgSize);
	}

	/*
	 *	Now get the actual size (which is the first 4 bytes of the
	 *	decoded block), move the bytes down and return
	 */

	uint32_t len = 0;
	for (int i = 0; i < 4; ++i) {
		len = (len << 8) | decode[i];
	}
	if (len > data.length - 4) len = (uint32_t)(data.length - 4);

	NSData *ret = [[NSData alloc] initWithBytes:decode + 4 length:len];

	free(encBuffer);
	free(msgBuffer);
	free(decode);

	return ret;
}

/************************************************************************/
/*																		*/
/*	Key Generation														*/
/*																		*/
/************************************************************************/

- (void)setUsername:(NSString *)uname passwordHash:(NSString *)passwordHash
{
	self.username = uname;
	self.passwordHash = passwordHash;
}

- (void)setServerUrl:(NSString *)s
{
	self.server = s;
}

/*
 *	Generate a new RSA Key. Done asynchronously
 */

- (BOOL)generateRSAKeyWithSize:(uint32_t)size
{
	uint8_t zero[32];
	memset(zero,0,sizeof(zero));
	if (!memcmp(passHash, zero, sizeof(passHash))) {
		return NO;
	}

	/*
	 *	Generate the RSA Keys
	 */

	SCRSAKey pub;
	SCRSAKey priv;

	SCRSAKeyGeneratePair(size, pub, priv);

	/*
	 *	Generate UUID
	 */

	std::string uuid = SCUUIDGenerator();

	/*
	 *	Initialize internal state
	 */

	self.publicRSAKey = [NSString stringWithUTF8String:pub.ToString().c_str()];
	privateRSAKey = new SCRSAKey(priv);
	self.deviceIdentifier = [NSString stringWithUTF8String:uuid.c_str()];
	return YES;
}

/*
 *	Secure data save
 */

- (void)encodeSecureData
{
	@synchronized(self) {
		/*
		 *	Now encode in the keychain
		 */

		SCSecuredData *sdata = [[SCSecuredData alloc] init];
		sdata.uuid = self.deviceIdentifier;
		sdata.publicKey = self.publicKey;
		sdata.privateKey = [NSString stringWithUTF8String:privateRSAKey->ToString().c_str()];
		sdata.username = self.username;
		sdata.password = self.passwordHash;
		sdata.serverURL = self.server;

		/*
		 *	Now encrypt
		 */

		SCBlowfish bf(sizeof(passHash),passHash);
		NSData *clearText = [sdata serializeData];

		/*
		 *	Steps: encode and save to keychain.
		 */

		uint8_t *buffer = (uint8_t *)malloc(clearText.length);
		memmove(buffer, clearText.bytes, clearText.length);
		bf.EncryptData((uint32_t *)buffer, clearText.length/8);

		NSData *writeData = [[NSData alloc] initWithBytesNoCopy:buffer length:clearText.length freeWhenDone:YES];
		SCSaveSecureData(writeData);
	}
}

/*
 *	Clear all saved data
 */

- (void)clear
{
	SCClearSecureData();
	if (privateRSAKey) {
		delete privateRSAKey;
		privateRSAKey = nil;
	}
	memset(passHash,0,sizeof(passHash));
	self.deviceIdentifier = nil;
	self.publicRSAKey = nil;
	self.username = nil;
	self.passwordHash = nil;
	self.server = nil;
}

@end
