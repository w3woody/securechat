//
//  SCChecksum.cpp
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

#include "SCChecksum.h"

/************************************************************************/
/*																		*/
/*	CRC32 checksum														*/
/*																		*/
/************************************************************************/

#define POLYNOMIAL		0x07			/* x^8 + x^2 + x^1 + x^0 */

static int CRC8Init;
static uint8_t CRC8Table[256];

/*	SCInitializeCRC8
 *
 *		Intiialize CRC8 tables
 */

void SCInitializeCRC8(void)
{
	CRC8Init = 1;

	for (int i = 0; i < 256; i++) {
		uint8_t rem = i;  /* remainder from polynomial division */
		for (int j = 0; j < 8; j++) {
			if (rem & 0x80) {
				rem <<= 1;
				rem ^= POLYNOMIAL;
			} else {
				rem <<= 1;
			}
		}
		CRC8Table[i] = rem;
	}
}

/*	SCCalcCRC8
 *
 *		Calculate CRC8. Note that multiple calls can concatenate the
 *	results; the previous CRC32 is passed in. (If there isn't a previous
 *	CRC32 result from another block, pass 0)
 */

uint8_t SCCalcCRC8(uint8_t crc, const uint8_t *buf, size_t len)
{
	/*
	 *	Sanity check: verify initialized. If we have to initialize then
	 *	this code is not re-entrant
	 */

	if (!CRC8Init) {
		SCInitializeCRC8();
	}

	for (size_t i = 0; i < len; ++i) {
		crc = CRC8Table[crc ^ *buf++];
	}
	return crc;
}
