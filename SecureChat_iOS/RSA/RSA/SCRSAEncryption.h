//
//  SCRSAEncryption.h
//  SecureChat
//
//  Created by William Woody on 2/21/16.
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

#ifndef SCRSAEncryption_h
#define SCRSAEncryption_h

#include <stdio.h>
#include "SCBigInteger.h"

/************************************************************************/
/*																		*/
/*	Public/Private keys													*/
/*																		*/
/************************************************************************/

/*	SCRSAKey
 *
 *		Represents the public key and private key for encryption. Note that
 *	in section 19.3 of "Advanced Cryptography", the public and private key
 *	both use the same modulus. In both cases we encrypt or decrypt by
 *	calculating val = msg.modPow(exp,mod). So it is easier for us to simply
 *	represent both keys as the exponent and modulus.
 */

class SCRSAKey
{
	public:
							SCRSAKey(std::string str);
							SCRSAKey()
								{
								}
							SCRSAKey(uint32_t nbits, const SCBigInteger &exp, const SCBigInteger &mod)
								{
									n = nbits;
									e = exp;
									m = mod;
								}
							SCRSAKey(const SCRSAKey &k)
								{
									n = k.n;
									e = k.e;
									m = k.m;
								}
							~SCRSAKey()
								{
								}

		SCRSAKey			&operator = (const SCRSAKey &k)
								{
									n = k.n;
									e = k.e;
									m = k.m;
									return *this;
								}

		std::string			ToString()  const;

		uint32_t			Size() const
								{
									return n;
								}

		const SCBigInteger	&Exponent() const
								{
									return e;
								}
		const SCBigInteger	&Modulus() const
								{
									return m.Modulus();
								}

		/*
		 *	Perform v.ModPow(e,m) in an efficient fashion, caching 
		 *	intermediate values as needed
		 */

		SCBigInteger		Transform(const SCBigInteger &v)
								{
									return m.ExpMod(v, e);
								}
	private:
		uint32_t			n;		// # bits
		SCBigInteger		e;		// exp
		SCMontMath			m;		// mod
};

/************************************************************************/
/*																		*/
/*	OAEP-style padding													*/
/*																		*/
/************************************************************************/

/*	SCRSAPadding
 *
 *		This class stores the intermediate values and handles the conversion
 *	to our custom OAEP-style padding.
 */

class SCRSAPadding
{
	public:
							SCRSAPadding(uint32_t n);
							~SCRSAPadding()
								{
								}

		/*
		 *	Calculated based on n; gives the sizes in bytes
		 */

		size_t				GetMessageSize()
								{
									return msgSize;
								}
		size_t				GetEncodeSize()
								{
									return encSize;
								}

		/*
		 *	Encoder/decoder. Note the input and output buffers must be at least 
		 *	the size given for messages or for encodings.
		 */

		bool				Encode(const uint8_t *msg, uint8_t *enc);
		bool				Decode(const uint8_t *enc, uint8_t *msg);

	private:
		size_t				msgSize;
		size_t				encSize;
};

/************************************************************************/
/*																		*/
/*	RSA Routines														*/
/*																		*/
/************************************************************************/

/*	SCRSAKeyGeneratePair
 *
 *		Generate a key pair, a public and a private key
 */

extern void SCRSAKeyGeneratePair(uint32_t n, SCRSAKey &pub, SCRSAKey &priv);


#endif /* SCRSAEncryption_h */
