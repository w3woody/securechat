//
//  SCBigInteger.cpp
//  SecureChat
//
//  Created by William Woody on 2/17/16.
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

#include "SCBigInteger.h"
#include <stdlib.h>
#include <new>
#include <Security/Security.h>

/************************************************************************/
/*																		*/
/*	Internal Support													*/
/*																		*/
/************************************************************************/

static int BitCount(uint32_t ct)
{
	int ret = 0;
	while (ct) {
		++ret;
		ct >>= 1;
	}
	return ret;
}

/************************************************************************/
/*																		*/
/*	Big Integer Construction/Destruction								*/
/*																		*/
/************************************************************************/

/*	SCBigInteger::SCBigInteger
 *
 *		Construction from integer value
 */

SCBigInteger::SCBigInteger(int32_t primVal)
{
	dataAlloc = 32;
	dataArray = (uint32_t *)malloc(sizeof(uint32_t) * dataAlloc);

	isNeg = (primVal < 0);
	isNan = false;
	dataArray[0] = (primVal < 0) ? -primVal : primVal;

	dataSize = primVal ? 1 : 0;
}

SCBigInteger::SCBigInteger(uint32_t primVal, bool neg)
{
	dataAlloc = 32;
	dataArray = (uint32_t *)malloc(sizeof(uint32_t) * dataAlloc);

	isNeg = primVal ? neg : false;
	isNan = false;
	dataArray[0] = primVal;

	dataSize = primVal ? 1 : 0;
}

/*	SCBigInteger::SCBigInteger
 *
 *		Construct from string
 */

SCBigInteger::SCBigInteger(std::string val)
{
	size_t size = (8 + val.length()) / 9;	// estimate words (crude)
	if (size < 32) size = 32;

	dataAlloc = (uint32_t)size;
	dataArray = (uint32_t *)malloc(sizeof(uint32_t) * dataAlloc);
	dataArray[0] = 0;
	dataSize = 0;

	isNeg = false;
	isNan = false;

	const char *c = val.c_str();
	if (*c == '-') {
		isNeg = true;
		++c;
	}

	while (*c) {
		MulAdd(10, *c++ - '0');
	}
}

/*	SCBigInteger::SCBigInteger
 *
 *		Create integer based on raw data array
 */

SCBigInteger::SCBigInteger(const uint32_t *data, const uint32_t nwords, bool neg)
{
	dataAlloc = 32;
	if (dataAlloc < nwords) dataAlloc = nwords;
	dataArray = (uint32_t *)malloc(sizeof(uint32_t) * dataAlloc);

	isNeg = neg;
	isNan = false;

	dataSize = nwords;
	memcpy(dataArray, data, sizeof(uint32_t) * nwords);

	while (dataSize > 0) {
		if (dataArray[dataSize-1] == 0) --dataSize;
		else break;
	}
	if (dataSize == 0) isNeg = false;
}

/*	SCBigInteger::SCBigInteger
 *
 *		Copy constructor
 */

SCBigInteger::SCBigInteger(const SCBigInteger &bi)
{
	dataAlloc = bi.dataAlloc;
	dataSize = bi.dataSize;
	isNan = bi.isNan;
	isNeg = bi.isNeg;

	dataArray = (uint32_t *)malloc(sizeof(uint32_t) * dataAlloc);
	if (dataSize) {
		memmove(dataArray, bi.dataArray, sizeof(uint32_t) * dataSize);
	}
}

/*	SCBigInteger::operator =
 *
 *		Copy operator
 */

SCBigInteger &SCBigInteger::operator = (const SCBigInteger &bi)
{
	dataSize = bi.dataSize;
	isNan = bi.isNan;
	isNeg = bi.isNeg;

	Realloc(dataSize);		// Resize memory if needed

	if (dataSize) {
		memmove(dataArray, bi.dataArray, sizeof(uint32_t) * dataSize);
	}

	return *this;
}

/*	SCBigInteger::~SCBigInteger
 *
 *		Delete. This zeros out the block of memory first. We zero out the
 *	memory in order to increase security; when we are done with intermediate
 *	values we don't want to preserve them.
 */

SCBigInteger::~SCBigInteger()
{
	memset(dataArray,0,sizeof(uint32_t) * dataAlloc);
	free(dataArray);
	dataArray = NULL;
	dataSize = 0;
	dataArray = 0;
	isNan = false;
	isNeg = false;
}

/************************************************************************/
/*																		*/
/*	Internal Support													*/
/*																		*/
/************************************************************************/

/*	SCBigInteger::Realloc
 *
 *		Realloc array if needed. This only grows but does not shrink
 */

void SCBigInteger::Realloc(uint32_t size)
{
	if (size > dataAlloc) {
		uint32_t *tmp = (uint32_t *)realloc(dataArray, size * sizeof(uint32_t));
		if (tmp == NULL) throw std::bad_alloc();

		dataAlloc = size;
		dataArray = tmp;
	}
}

/*	SCBigInteger::MulAdd
 *
 *		A basic method to multiply and add within the integer value
 *	itself.
 */

void SCBigInteger::MulAdd(uint32_t mul, uint32_t add)
{
	uint64_t scratch;
	uint32_t i;

	scratch = add;
	for (i = 0; i < dataSize; ++i) {
		scratch += ((uint64_t)mul) * dataArray[i];
		dataArray[i] = (uint32_t)scratch;
		scratch >>= 32;
	}

	if (scratch) {
		// Size of data block has grown. Make sure we fit
		Realloc(dataSize+1);
		dataArray[dataSize] = (uint32_t)scratch;
		++dataSize;
	}
}

/*	SCBigInteger::DivRemain
 *
 *		Divide by supplied integer, return remainder
 */

uint32_t SCBigInteger::DivRemain(uint32_t div)
{
	uint64_t scratch;
	uint32_t i;

	i = dataSize;
	scratch = 0;
	while (i-- > 0) {
		scratch = (scratch << 32) | dataArray[i];

		uint32_t rem = (uint32_t)(scratch % div);
		dataArray[i] = (uint32_t)(scratch / div);

		scratch = rem;
	}

	/*
	 *	Reset data size
	 */

	while (dataSize > 0) {
		if (dataArray[dataSize-1] == 0) --dataSize;
		else break;
	}
	if (dataSize == 0) isNeg = false;

	/*
	 *	Return remainder
	 */

	return (uint32_t)scratch;
}

/*	SCBigInteger::CompareAbs
 *
 *		Return 1 if greater, 0 if equal, -1 if less, and 2 if nan. Compares
 *	only the absolute values without looking at the negation flag.
 */

int SCBigInteger::CompareAbs(const SCBigInteger &i) const
{
	/*
	 *	Compare the unsigned array
	 */

	int comp = 0;
	if (dataSize > i.dataSize) {
		comp = 1;
	} else if (dataSize < i.dataSize) {
		comp = -1;
	} else {
		/*
		 *	MSB is in the highest index. So we need to compare from right
		 *	to left
		 */

		uint32_t x = dataSize;
		while (x-- > 0) {
			uint32_t l = dataArray[x];
			uint32_t r = i.dataArray[x];
			if (l > r) {
				comp = 1;
				break;
			} else if (l < r) {
				comp = -1;
				break;
			}
		}
	}

	return comp;
}


/*	SCBigInteger::CompareTo
 *
 *		Return 1 if greater, 0 if equal, -1 if less, and 2 if nan.
 */

int SCBigInteger::CompareTo(const SCBigInteger &i) const
{
	if (isNan) return 2;
	if (i.isNan) return 2;			// nans are never equal. They're errors

	if (isNeg != i.isNeg) {
		return i.isNeg ? 1 : -1;	// > if supplied is negative
	}

	/*
	 *	Compare the unsigned array
	 */

	int comp = CompareAbs(i);
	if (isNeg) comp = -comp;
	return comp;
}

/*
 *	Internal left shift. Note that this really means shifting from
 *	the least significant bit towards the most significant bit.
 */

void SCBigInteger::ShiftLeft(uint32_t bits)
{
	if (bits == 0) return;
	if (dataSize == 0) return;

	uint64_t scratch;
	uint32_t off = bits % 32;
	uint32_t words = bits / 32;
	uint32_t src,dst;

	// Make sure big enough
	Realloc(dataSize + words + 1);

	src = dataSize;
	dataSize += words + 1;
	dst = dataSize;
	dataArray[--dst] = 0;

	while (src-- > 0) {
		dst--;
		scratch = dataArray[src];
		scratch <<= off;
		dataArray[dst+1] |= (uint32_t)(scratch >> 32);
		dataArray[dst] = (uint32_t)scratch;
	}
	while (dst-- > 0) {
		dataArray[dst] = 0;
	}

	while (dataSize > 0) {
		if (dataArray[dataSize-1] == 0) --dataSize;
		else break;
	}
	if (dataSize == 0) isNeg = false;
}

/*
 *	Internal right shift. This really means shifting from the most
 *	significant bit towards the least significant bit.
 */

void SCBigInteger::ShiftRight(uint32_t bits)
{
	if (bits == 0) return;
	if (dataSize == 0) return;

	uint64_t scratch;
	uint32_t off = bits % 32;
	uint32_t words = bits / 32;
	uint32_t src,dst;

	src = words;
	dst = 0;
	while (src < dataSize) {
		scratch = dataArray[src];
		scratch <<= (32 - off);
		if (dst > 0) {
			dataArray[dst-1] |= (uint32_t)scratch;
		}
		dataArray[dst] = (uint32_t)(scratch >> 32);

		++src;
		++dst;
	}

	dataSize -= words;
	while (dataSize > 0) {
		if (dataArray[dataSize-1] == 0) --dataSize;
		else break;
	}
	if (dataSize == 0) isNeg = false;
}

/*
 *	Add internal; only manipulates the data
 */

void SCBigInteger::AddInternal(const SCBigInteger &bi)
{
	uint64_t scratch = 0;
	uint32_t pos = 0;

	// Make sure we have enough space.
	if (dataAlloc < bi.dataSize+1) {
		Realloc(bi.dataSize + 1);
	}

	while ((pos < dataSize) || (pos < bi.dataSize)) {
		if (pos < dataSize) {
			scratch += dataArray[pos];
		}
		if (pos < bi.dataSize) {
			scratch += bi.dataArray[pos];
		}

		if (dataSize <= pos) {
			dataSize = pos+1;
		}
		dataArray[pos++] = (uint32_t)scratch;
		scratch >>= 32;
	}

	if (scratch) {
		if (dataSize <= pos) {
			dataSize = pos+1;
		}
		dataArray[pos++] = (uint32_t)scratch;
	}
}

/*
 *	Subtract internal. Requires that the value passed is smaller than
 *	the current value
 */

void SCBigInteger::SubInternal(const SCBigInteger &bi)
{
	int64_t scratch = 0;
	uint32_t pos = 0;

	while (pos < dataSize) {
		scratch += dataArray[pos];
		if (pos < bi.dataSize) {
			scratch -= bi.dataArray[pos];
		}

		dataArray[pos++] = (uint32_t)scratch;
		scratch >>= 32;
	}

	while (dataSize > 0) {
		if (dataArray[dataSize-1] == 0) --dataSize;
		else break;
	}
	if (dataSize == 0) isNeg = false;
}


/*	SCBigInteger::SetBit
 *
 *		Set the specified bit
 */

void SCBigInteger::SetBit(uint32_t bit)
{
	uint32_t len = (bit + 32) >> 5;		// necessary space to fit.
	Realloc(len);

	/*
	 *	note that we have garbage at the bits above the data size count.
	 */

	uint32_t bmask = (uint32_t)(1L << (bit & 31));
	uint32_t index = (uint32_t)(bit >> 5);

	if (index >= dataSize) {
		dataArray[dataSize++] = 0;
	}

	dataArray[index] |= bmask;
}

/*	SCBigInteger::BitTest
 *
 *		Test bit
 */

bool SCBigInteger::BitTest(uint32_t bit) const
{
	uint32_t bmask = (uint32_t)(1L << (bit & 31));
	uint32_t index = (uint32_t)(bit >> 5);

	if (index >= dataSize) return false;
	return 0 != (dataArray[index] & bmask);
}

/*	SCBigInteger::DivRemain
 *
 *		Divide and remainder calculation for big integer values. Performs
 *	work inline. This returns *this/i in m, with the remainder inside.
 */

void SCBigInteger::DivRemain(const SCBigInteger &i, SCBigInteger &m)
{
	SCBigInteger d = i;
	/*
	 *	This is a brute force algorithm of shift and subtract
	 */

	uint32_t bit = 0;
	m.dataSize = 0;
	while (CompareAbs(d) >= 0) {
		++bit;
		d.ShiftLeft(1);
	}

	while (bit > 0) {
		d.ShiftRight(1);
		--bit;

		if (CompareAbs(d) >= 0) {
			SubInternal(d);
			m.SetBit(bit);
		}
	}

	/* The remainder's signum remains the same as the numerator */
	/* The return value's signum is the same as if we multiplied */
	m.isNeg = isNeg != i.isNeg;
	if (m.dataSize == 0) m.isNeg = false;		// if zero, force 0
	if (dataSize == 0) isNeg = false;
}

/*	SCBigInteger::MulAdd
 *
 *		Multiply/add support for our multiply routine
 */

void SCBigInteger::MulAdd(const SCBigInteger &add, uint32_t mul, uint32_t shift)
{
	// Set msize larger than any potential product here. Guarantee space.
	uint32_t msize = shift + add.dataSize + 1;
	if (msize < dataSize + 1) msize = dataSize + 1;
	Realloc(msize);

	uint64_t scratch = 0;
	uint32_t pos = shift;

	// Iterate through all digits.
	for (uint32_t i = 0; pos < msize; ++i, ++pos) {
		if (pos < dataSize) {
			scratch += dataArray[pos];
		}
		if (i < add.dataSize) {
			scratch += ((uint64_t)mul) * add.dataArray[i];
		}
		dataArray[pos] = (uint32_t)scratch;
		scratch >>= 32;
	}

	while (msize > 0) {
		if (dataArray[msize-1] == 0) --msize;
		else break;
	}
	dataSize = msize;
}

/************************************************************************/
/*																		*/
/*	Big Integer Routines												*/
/*																		*/
/************************************************************************/

/*	SCBigInteger::Random
 *
 *		Random generator. Note this is the one place where we rely on
 *	the underlying API in order to generate a key. This theoretically could
 *	create weakness; if that is of concern feel free to replace this with
 *	your own algorithm.
 *
 *		The strength of the key itself is not really compromised by the
 *	weakness of the random number generator, though the predictability of
 *	the generated random number does introduce weakness. Meaning if you
 *	replace this with an unseeded random number generator, you will generate
 *	the same keys, and someone will be able to crack your messages with
 *	about zero effort.
 */

SCBigInteger SCBigInteger::Random(int nbits)
{
	SCBigInteger ret;

	if (nbits < 1) nbits = 1;			// zero bits? Really?

	int nwords = (nbits + 31) >> 5;		// # words of storage we need
	ret.Realloc(nwords);

	SecRandomCopyBytes(kSecRandomDefault, nwords * 4, (uint8_t *)ret.dataArray);

	// Now trim the top
	if (nbits & 31) {
		uint32_t mswmask = (uint32_t)(1L << (nbits & 31)) - 1;
		ret.dataArray[nwords-1] &= mswmask;
	}

	// And count the bits. Note that there is a (VERY VERY VERY SMALL) chance
	// the msw is zero, so we need to handle that case
	while (nwords > 0) {
		if (ret.dataArray[nwords-1] == 0) --nwords;
		else break;
	}
	ret.dataSize = nwords;

	return ret;
}

/************************************************************************/
/*																		*/
/*	Prime Support														*/
/*																		*/
/************************************************************************/

/*	SCBigInteger::ProbablePrime
 *
 *		Keep drawing random numbers until we find a probable prime of the
 *	length specified
 */

SCBigInteger SCBigInteger::ProbablePrime(int nbits)
{
	SCBigInteger tmp = Random(nbits);
	for (;;) {
		// Set the nth bit. Random returned a word wide enough so we just
		// need to flip the correct bit
		uint32_t word = (nbits-1) >> 5;
		uint32_t mask = (1 << ((nbits-1) & 31));
		tmp.dataArray[word] |= mask;
		tmp.dataSize = word+1;
		tmp.dataArray[0] |= 1;		// make odd

		if (tmp.IsProbablePrime()) break;

		tmp = Random(nbits);
	}
	return tmp;
}

/*	SCBigInteger::IsProbablePrime
 *
 *		Determine if this is a likely prime. We use the Rabin-Miller
 *	algorithm for this test. Note that we do not test against an array
 *	of small known primes. This can result in errors if this is used
 *	to test against small integer values such as 2 or 3.
 *
 *		Algorithm comes from Chapter 11, section 5 of Applied Cryptography
 */

bool SCBigInteger::IsProbablePrime(int certainty) const
{
	if (isNeg) return false;
	if (dataSize == 0) return false;
	if ((dataSize == 1) && (dataArray[0] == 2)) return true;	// 2
	if (IsEven()) return false;

	/*
	 *	Step 1: iterate through the first handful of small primes
	 */

	static const uint32_t smallprimes[] = {
		3,5,7,11,13,17,19,23,29,
		31,37,41,43,47,53,59,61,67,71,
		73,79,83,89,97,101,103,107,109,113,
		127,131,137,139,149,151,157,163,167,173,
		179,181,191,193,197,199,211,223,227,229,
		233,239,241,251,257,263,269,271,277,281,
		283,293,307,311,313,317,331,337,347,349,
		353,359,367,373,379,383,389,397,401,409,
		419,421,431,433,439,443,449,457,461,463,
		467,479,487,491,499,503,509,521,523,541,
		547,557,563,569,571,577,587,593,599,601,
		607,613,617,619,631,641,643,647,653,659,
		661,673,677,683,691,701,709,719,727,733,
		739,743,751,757,761,769,773,787,797,809,
		811,821,823,827,829,839,853,857,859,863,
		877,881,883,887,907,911,919,929,937,941,
		947,953,967,971,977,983,991,997,1009,1013,
		1019,1021,1031,1033,1039,1049,1051,1061,1063,1069,
		1087,1091,1093,1097,1103,1109,1117,1123,1129,1151,
		1153,1163,1171,1181,1187,1193,1201,1213,1217,1223,
		1229,1231,1237,1249,1259,1277,1279,1283,1289,1291,
		1297,1301,1303,1307,1319,1321,1327,1361,1367,1373,
		1381,1399,1409,1423,1427,1429,1433,1439,1447,1451,
		1453,1459,1471,1481,1483,1487,1489,1493,1499,1511,
		1523,1531,1543,1549,1553,1559,1567,1571,1579,1583,
		1597,1601,1607,1609,1613,1619,1621,1627,1637,1657,
		1663,1667,1669,1693,1697,1699,1709,1721,1723,1733,
		1741,1747,1753,1759,1777,1783,1787,1789,1801,1811,
		1823,1831,1847,1861,1867,1871,1873,1877,1879,1889,
		1901,1907,1913,1931,1933,1949,1951,1973,1979,1987,
		1993,1997,1999,0
	};
	for (int i = 0; smallprimes[i]; ++i) {
		SCBigInteger tmp = *this;
		if (0 == tmp.DivRemain(smallprimes[i])) return false;
	}

	/*
	 *	Step 2: perform the Rabin-Miller test the recommended number of
	 *	times given the bit size
	 */

	certainty /= 2;
	int minRounds;
	uint32_t nbits = GetBitLength();
	if (nbits < 256) {
		minRounds = 27;
	} else if (nbits < 512) {
		minRounds = 15;
	} else if (nbits < 768) {
		minRounds = 8;
	} else if (nbits < 1024) {
		minRounds = 4;
	} else {
		minRounds = 2;
	}

	if (certainty < minRounds) certainty = minRounds;
	return PassRabinMiller(certainty);
}

uint32_t SCBigInteger::GetBitLength() const
{
	return (dataSize - 1) * 32 + BitCount(dataArray[dataSize-1]);
}

uint32_t SCBigInteger::GetLowestSetBit() const
{
	uint32_t index = 0;
	while ((index < dataSize) && (dataArray[index] == 0)) ++index;
	if (index >= dataSize) return -1;		// zero

	uint32_t bit = 1;
	uint32_t msb = dataArray[index];
	uint32_t x = 0;
	while (0 == (bit & msb)) {
		bit <<= 1;
		++x;
	}

	return index * 32 + x;
}

/*	SCBigInteger::PassRabinMiller
 *
 *		Perform Rabin-Miller test, from Applied Cryptography, Chapter 11,
 *	section 5.
 */

bool SCBigInteger::PassRabinMiller(int iterations) const
{
	SCBigInteger one(1);
	SCBigInteger two(2);
	SCBigInteger pminus1 = *this - one;
	SCBigInteger m = pminus1;
	uint32_t a = m.GetLowestSetBit();
	uint32_t blen = GetBitLength();
	m.ShiftRight(a);

	if (IsEven()) return false;

	// Generate a montgomery math object for rapid ModPow operations
	SCMontMath mod(*this);

	for (int i = 0; i < iterations; ++i) {
		SCBigInteger b;

		// Find a test random value between 1 and our value
		do {
			b = SCBigInteger::Random(blen);
		} while ((b <= one) && (b >= *this));

		int j = 0;
//		SCBigInteger z = b.ModPow(m, *this);
		SCBigInteger z = mod.ExpMod(b, m);		//

		// Combine steps 4 and 6 for prime test
		while (((j != 0) || (z != one)) && (z != pminus1)) {
			if (((j > 0) && (z == one)) || (++j == a)) {
				return false;
			}
			z = mod.ExpMod(z, two);	// faster in theory than straight mult
//			z = (z * z) % *this;
		}
	}
	return true;
}

/************************************************************************/
/*																		*/
/*	Math Operators														*/
/*																		*/
/************************************************************************/

SCBigInteger SCBigInteger::operator + (const SCBigInteger &bi) const
{
	SCBigInteger ret;

	if (isNan || bi.isNan) {
		ret.isNan = true;
	} else {
		if (isNeg == bi.isNeg) {
			ret = *this;
			ret.AddInternal(bi);
			ret.isNeg = isNeg;
		} else {
			if (CompareAbs(bi) > 0) {
				ret = *this;
				ret.SubInternal(bi);
				ret.isNeg = isNeg && (ret.dataSize > 0);
			} else {
				ret = bi;
				ret.SubInternal(*this);
				ret.isNeg = bi.isNeg && (ret.dataSize > 0);
			}
		}
	}

	return ret;
}


SCBigInteger SCBigInteger::operator - (const SCBigInteger &bi) const
{
	SCBigInteger neg = bi;
	neg.isNeg = !neg.isNeg;
	return *this + neg;
}


SCBigInteger SCBigInteger::operator * (const SCBigInteger &bi) const
{
	SCBigInteger tmp = 0;

	uint32_t index;
	for (index = 0; index < dataSize; ++index) {
		tmp.MulAdd(bi, dataArray[index], index);
	}

	tmp.isNeg = isNeg != bi.isNeg;
	return tmp;
}


SCBigInteger SCBigInteger::operator / (const SCBigInteger &bi) const
{
	SCBigInteger tmp = *this;
	SCBigInteger res;
	tmp.DivRemain(bi,res);
	return res;
}


SCBigInteger SCBigInteger::operator % (const SCBigInteger &bi) const
{
	SCBigInteger tmp = *this;
	SCBigInteger res;
	tmp.DivRemain(bi,res);
	return tmp;
}

/*	gcd
 *
 *		Internal method 
 */

static uint32_t gcd(uint32_t x, uint32_t y)
{
	uint32_t tmp;

	if (y > x) {
		tmp = x;
		x = y;
		y = tmp;
	}

	while (y) {
		tmp = y;
		y = x % y;
		x = tmp;
	}
	return x;
}


SCBigInteger SCBigInteger::GCD(const SCBigInteger &bi) const
{
	SCBigInteger a = *this;
	SCBigInteger b = bi;
	SCBigInteger tmp;

	SCBigInteger *x = &a;
	SCBigInteger *y = &b;
	SCBigInteger *t;

	if (*x > *y) {
		t = x;
		x = y;
		y = t;
	}
	while (y->dataSize) {
		if ((y->dataSize == 1) && (x->dataSize == 1)) {
			// Small values; escape to faster routine
			return SCBigInteger(gcd(y->dataArray[0],x->dataArray[0]),false);
		}

		x->DivRemain(*y, tmp);
		t = x;
		x = y;
		y = t;
	}

	x->isNeg = false;
	return *x;
}

/*	ModInverse
 *
 *		Find the modinverse using the extended euclid algorithm. Uses the
 *	sample code in section 11.3: Number Theory of the book Applied
 *	Cryptography by Bruce Schneier. Note that the implementation is given
 *	for readability instead of speed.
 */

SCBigInteger SCBigInteger::ModInverse(const SCBigInteger &bi) const
{
	SCBigInteger nan;
	SCBigInteger one(1);
	SCBigInteger zero(0);

	SCBigInteger tmp;
	SCBigInteger u = bi;
	SCBigInteger v = *this;		// pre-swap.
	uint32_t k;

	nan.isNan = true;
	if (isNeg || (dataSize == 0)) return nan;
	if (bi.isNeg || (bi.dataSize == 0)) return nan;	// errors.

	v = v % u;
//	u = u.mod(v);				// Note we pre-swap and limit the size above.
//	if (u.compareTo(v) < 0) {	// So this test and swap is unnecessary.
//		tmp = u;
//		u = v;
//		v = tmp;
//	}

	for (k = 0; u.IsEven() && v.IsEven(); ++k) {
		u.ShiftRight(1);
		v.ShiftRight(1);
	}
	
	SCBigInteger u1 = 1;
	SCBigInteger u2 = 0;
	SCBigInteger u3 = u;
	SCBigInteger t1 = v;
	SCBigInteger t2 = u - one;
	SCBigInteger t3 = v;
	
	do {
		do {
			if (u3.IsEven()) {
				if (u1.IsOdd() || u2.IsOdd()) {
					u1 = u1 + v;
					u2 = u2 + u;
				}
				u1.ShiftRight(1);
				u2.ShiftRight(1);
				u3.ShiftRight(1);
			}
			if (t3.IsEven() || (u3 < t3)) {
				tmp = u1;
				u1 = t1;
				t1 = tmp;
				tmp = u2;
				u2 = t2;
				t2 = tmp;
				tmp = u3;
				u3 = t3;
				t3 = tmp;
			}
		} while (u3.IsEven());
		
		while ((u1 < t1) && (u2 < t2)) {
			u1 = u1 + v;
			u2 = u2 + u;
		}
		
		u1 = u1 - t1;
		u2 = u2 - t2;
		u3 = u3 - t3;
	} while (t3 > zero);
	
	while ((u1 >= v) && (u2 >= u)) {
		u1 = u1 - v;
		u2 = u2 - u;
	}
	
	u1.ShiftLeft(k);
	u2.ShiftLeft(k);
	u3.ShiftLeft(k);
	
	// u3 == gcd
	if (u3 == one) {
		return u - u2;
	} else {
		return nan;
	}
}


/************************************************************************/
/*																		*/
/*	ModPow																*/
/*																		*/
/*		Because so much of cryptography relies on this function, we do	*/
/*	what we can to maximize the speed of this operation.				*/
/*																		*/
/************************************************************************/

/*	SCBigInteger::ModPowOdd
 *
 *		Implements the Montgomery exponentiation documented as algorithm
 *	14.94 of the Handbook of Applied Cryptography by creating an instance
 *	of the SCMontMath internal class
 */

SCBigInteger SCBigInteger::ModPowOdd(const SCBigInteger &e, const SCBigInteger &mod) const
{
	SCMontMath mm = mod;
	return mm.ExpMod(*this,e);
}

/*	SCBigInteger::ModPowOld
 *
 *		This is a rather simple implementation of ModPow which is correct
 *	but slow as a dog. To be removed.
 */

SCBigInteger SCBigInteger::ModPowOld(const SCBigInteger &e, const SCBigInteger &mod) const
{
	SCBigInteger zero = 0;
	SCBigInteger r = 1;
	SCBigInteger t = *this;
	SCBigInteger n = e;

	bool negate = false;
	if (n < zero) {
		n.isNeg = false;
		negate = true;
	}

	while (n != zero) {
		if (n.IsOdd()) {
			r = (r * t) % mod;
		}
		n.ShiftRight(1);
		t = (t * t) % mod;
	}

	if (negate) {
		return r.ModInverse(mod);
	} else {
		return r;
	}
}

/*	SCBigInteger::ModPow
 *
 *		For encryption this routine gets called so many friggin' times that
 *	we need a *FAST* ModPow. So we combine Montgomery reduction with 
 *	a few other tricks to speed up this process.
 *
 *
 */

SCBigInteger SCBigInteger::ModPow(const SCBigInteger &e, const SCBigInteger &mod) const
{
	/*
	 *	Deal with trivial cases
	 */

	if ((mod.dataSize == 0) || (mod.isNeg)) {
		// Error
		SCBigInteger nan;
		nan.isNan = true;
		return nan;
	}

	if ((mod.dataSize == 1) && (mod.dataArray[0] == 1)) {
		return SCBigInteger(0);
	}

	if (dataSize == 0) {
		return SCBigInteger(0);
	}

	if ((dataSize == 1) && (dataArray[0] == 1) && !isNeg) {
		return SCBigInteger(1);
	}

	if ((e.dataSize == 1) && (e.dataArray[0] == 1)) {
		if (e.isNeg) {
			return ModInverse(mod);
		} else {
			return *this % mod;
		}
	}

	/*
	 *	Set up exponent, handle negate
	 */

	bool negate = false;
	SCBigInteger exponent = e;
	if (exponent.isNeg) {
		negate = true;
		exponent.isNeg = false;
	}
	SCBigInteger result;

	/*
	 *	The heart of the modulus operation
	 */

	if (mod.IsOdd()) {
		/*
		 *	If the value was odd, we can use Montgomery exponentiation
		 *	for a much faster exponent operation
		 */

		result = ModPowOdd(exponent, mod);
	} else {
		/*
		 *	TODO: Rewrite in terms of something else. For now, punt and
		 *	brute force
		 */

		result = ModPowOld(exponent, mod);
	}

	/*
	 *	If the exponent was negative, inverse
	 */

	if (negate) {
		return result.ModInverse(mod);
	} else {
		return result;
	}
}

/************************************************************************/
/*																		*/
/*	Return string														*/
/*																		*/
/************************************************************************/

/*	SCBigInteger::IsEven
 *
 *		Return true if even or zero
 */

bool SCBigInteger::IsEven() const
{
	if (dataSize == 0) return true;
	return 0 == (1 & dataArray[0]);
}

/*	SCBigInteger::ToString
 *
 *		Return string representation of big integer
 */

std::string SCBigInteger::ToString() const
{
	std::string tmp;

	if (isNan) {
		tmp.push_back('n');
		tmp.push_back('a');
		tmp.push_back('n');
	} else if (dataSize == 0) {
		tmp.push_back('0');
	} else {
		/*
		 *	Set up scratch and extract digits
		 */

		SCBigInteger i = *this;

		while (i.dataSize) {
			uint32_t digit = i.DivRemain(10);
			tmp.push_back('0' + digit);
		}

		if (isNeg) tmp.push_back('-');
	}

	/*
	 *	Reverse string
	 */

	size_t s = 0;
	size_t e = tmp.length() - 1;
	while (s < e) {
		char c = tmp[s];
		tmp[s] = tmp[e];
		tmp[e] = c;
		++s;
		--e;
	}

	return tmp;
}

/************************************************************************/
/*																		*/
/*	Compare Operations													*/
/*																		*/
/************************************************************************/

/*	SCBigInteger::operator ==
 *
 *		Return true if values are equal
 */

bool SCBigInteger::operator == (const SCBigInteger &i) const
{
	if (isNan) return false;
	if (i.isNan) return false;		// nans are never equal. They're errors

	if (isNeg != i.isNeg) return false;
	if (dataSize != i.dataSize) return false;

	uint32_t nwords = (i.dataSize + 31) >> 5;

	return memcmp(i.dataArray, dataArray, nwords * 4) == 0;
}

