//
//  SCSecureHash.h
//  SecureChat
//
//  Created by William Woody on 2/24/16.
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

#ifndef SCSecureHash_h
#define SCSecureHash_h

#include <stdio.h>
#include <stdint.h>

/************************************************************************/
/*																		*/
/*	SHA256 hash															*/
/*																		*/
/************************************************************************/

/*	SCSHA256Context
 *
 *		Perform SHA-256 algorithm
 */

class SCSHA256Context
{
	public:
						SCSHA256Context();
						~SCSHA256Context();

		void			Start();
		void			Update(size_t len, const uint8_t *data);
		void			Finish(uint8_t output[32]);

	private:
		void			Zero();
		void			ProcessBuffer();

		uint64_t		total;
		uint32_t		state[8];
		int				index;
		uint8_t			buffer[64];
};

#endif /* SCSecureHash_h */
