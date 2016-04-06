//
//  SCBlowfish.h
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

#ifndef SCBlowfish_h
#define SCBlowfish_h

#include <stdio.h>
#include <stdint.h>

/************************************************************************/
/*																		*/
/*	Blowfish Implementation												*/
/*																		*/
/************************************************************************/

/*	SCBlowfish
 *
 *		A block cipher documented here:
 *
 *	https://www.schneier.com/cryptography/archives/1994/09/description_of_a_new.html
 */

class SCBlowfish
{
	public:
		/*
		 *	Note: only the first 448 bits (56 bytes) of the key are
		 *	used. If the key is shorter, it is rolled to fill 56
		 *	bytes. Bytes beyond the 56th byte are ignored
		 */

						SCBlowfish(uint16_t len, uint8_t *key);
						~SCBlowfish();

		/*
		 *	Encrypt and decrypt a single block
		 */

		void			EncryptBlock(uint32_t x[2]);
		void			DecryptBlock(uint32_t x[2]);

		/*
		 *	Note:
		 *
		 *		For these to work the data must be a multiple of 64 byte
		 *	blocks in size. The size is given in blocks. Also note that
		 *	the result of the previous block is XORed into the next block,
		 *	so this is not the same as simply calling EncryptBlock and
		 *	DecryptBlock on each individual block.
		 */

		void			EncryptData(uint32_t *data, size_t nblocks);
		void			DecryptData(uint32_t *data, size_t nblocks);

	private:
		uint32_t		f(uint32_t);
		uint32_t		s[4][256];
		uint32_t		p[18];
};

#endif /* SCBlowfish_hpp */
