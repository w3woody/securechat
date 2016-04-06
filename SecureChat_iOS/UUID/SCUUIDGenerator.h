//
//  SCUUIDGenerator.h
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


#ifndef SCUUIDGenerator_hpp
#define SCUUIDGenerator_hpp

#include <stdio.h>
#include <string>

/************************************************************************/
/*																		*/
/*	UUID Implementation													*/
/*																		*/
/************************************************************************/

/*	SCUUIDGenerator
 *
 *		This function generates a random UUID. The reason why we use
 *	our own UUID is because one type of UUID that can be generated is
 *	associated with the device ID of our device. In order to guarantee
 *	that we cannot trivially tie back an internally generated UUID back
 *	to our device, we use secure random to securely generate a UUID.
 *
 *		See https://en.wikipedia.org/wiki/Universally_unique_identifier
 *	for more information.
 *
 *		We use UUIDs in order to announce who we are remotely so we
 *	can gather messages associated with a specific device, and to tie
 *	devices to a specific account. It is important that our UUID be
 *	anonymous (that is, a Version 4 UUID) so that we cannot examine the
 *	list of UUIDs associated with an account and easily figure out which
 *	device is associated with it.
 */

extern std::string SCUUIDGenerator(void);


#endif /* SCUUIDGenerator_hpp */
