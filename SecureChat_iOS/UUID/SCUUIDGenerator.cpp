//
//  SCUUIDGenerator.cpp
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


#include "SCUUIDGenerator.h"

#include <stdint.h>
#include <Security/Security.h>

/************************************************************************/
/*																		*/
/*	UUID Implementation													*/
/*																		*/
/************************************************************************/

/************************************************************************\

We generate a Version 4 UUID, documented here:

https://en.wikipedia.org/wiki/Universally_unique_identifier#Version_4_.28random.29

\************************************************************************/

/*	SCUUIDGenerator
 *
 *		Generate a random 64-bit value.
 */

std::string SCUUIDGenerator()
{
	uint8_t buffer[16];			// 128-bit buffer

	/*
	 *	Generate a 128-bit long random value
	 */

	SecRandomCopyBytes(kSecRandomDefault, sizeof(buffer), buffer);

	/*
	 *	Now slam certain values to make this a version 4 UUID
	 */

	buffer[6] = (buffer[6] & 0x0F) | 0x40;		// set version = 4
	buffer[8] = (buffer[8] & 0x3F) | 0x80;		// Slam top to bits to 10b

	/*
	 *	Format in standard format
	 */

	std::string str;
	for (int i = 0; i < 16; ++i) {
		char tmp[4];

		if ((i == 4) || (i == 6) || (i == 8) || (i == 10)) {
			str.push_back('-');
		}

		sprintf(tmp,"%02x",buffer[i]);
		str.append(tmp);
	}

	return str;
}
