//
//  SCChecksum.h
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

#ifndef SCChecksum_h
#define SCChecksum_h

#include <stdio.h>
#include <stdint.h>

/************************************************************************/
/*																		*/
/*	Checksum Support													*/
/*																		*/
/************************************************************************/

#ifdef __cplusplus
extern "C" {
#endif

/*	SCInitializeCRC8
 *
 *		Intiialize CRC8 tables
 */

extern void SCInitializeCRC8(void);

/*	SCCalcCRC8
 *
 *		Calculate CRC8. Note that multiple calls can concatenate the
 *	results; the previous CRC32 is passed in. (If there isn't a previous
 *	CRC32 result from another block, pass 0)
 */

extern uint8_t SCCalcCRC8(uint8_t crc, const uint8_t *buf, size_t len);

#ifdef __cplusplus
} // extern "C"
#endif

#endif /* SCChecksum_hpp */
