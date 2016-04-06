//
//  SCRSAKey.cpp
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

#include <stdio.h>
#include <stdlib.h>
#include <string>
#include "SCRSAEncryption.h"

#define TEST	1

/************************************************************************/
/*																		*/
/*	Public/Private keys													*/
/*																		*/
/************************************************************************/

/*	SCRSAKey::SCRSAKey
 *
 *		Decode an RSA key pair (exponent,modulus) from a string. We don't
 *	do anything fancy here; we simply store as a comma-separated pair.
 */

SCRSAKey::SCRSAKey(std::string str)
{
	const char *s = str.c_str();
	int i = 0;
	while ((s[i] != 0) && (s[i] != ',')) ++i;
	int j = i + 1;
	while ((s[j] != 0) && (s[j] != ',')) ++j;

	std::string exp(s,i);
	std::string sze(s+i+1,j-1);
	std::string mod(s+j+1);

	e = SCBigInteger(exp);
	n = (uint32_t)atol(sze.c_str());
	m = SCBigInteger(mod);
}

/*	SCRSAKey::ToString
 *
 *		Convert to a string which can be transmitted or stored. This
 *	basically converts the RSA string into two integer values
 */

std::string SCRSAKey::ToString() const
{
	char buffer[32];
	sprintf(buffer,"%d",n);

	return e.ToString() + "," + buffer + "," + m.Modulus().ToString();
}

