//
//  SCBigInteger.h
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

#ifndef SCBigInteger_h
#define SCBigInteger_h

#include <stdio.h>
#include <stdint.h>
#include <string>

/************************************************************************/
/*																		*/
/*	Big Integer Routines												*/
/*																		*/
/************************************************************************/

/*	SCBigInteger
 *
 *		Big integer class.
 */

// http://developer.classpath.org/doc/java/math/BigInteger-source.html

class SCBigInteger
{
	public:
								SCBigInteger(int32_t primVal = 0);
								SCBigInteger(uint32_t primVal, bool neg);
								SCBigInteger(std::string val);
								SCBigInteger(const uint32_t *data, const uint32_t nwords, bool neg = false);
								~SCBigInteger();

								SCBigInteger(const SCBigInteger &bi);
		SCBigInteger			&operator = (const SCBigInteger &bi);

		/*
		 *	Access to the raw data array of the value
		 */

		const uint32_t			*GetData() const
									{
										return dataArray;
									}
		const uint32_t			GetDataSize() const
									{
										return dataSize;
									}
		bool					IsNegative() const
									{
										return isNeg;
									}

		static SCBigInteger		ProbablePrime(int nbits);
		static SCBigInteger		Random(int nbits);
		bool					IsProbablePrime(int certainty = 100) const;

		bool					operator == (const SCBigInteger &bi) const;
		bool					operator != (const SCBigInteger &bi) const
									{
										return !(operator ==(bi));
									}
		bool					operator > (const SCBigInteger &bi) const
									{
										return CompareTo(bi) > 0;
									}
		bool					operator <= (const SCBigInteger &bi) const
									{
										return CompareTo(bi) <= 0;
									}
		bool					operator >= (const SCBigInteger &bi) const
									{
										return CompareTo(bi) >= 0;
									}
		bool					operator < (const SCBigInteger &bi) const
									{
										return CompareTo(bi) < 0;
									}

		SCBigInteger			operator + (const SCBigInteger &bi) const;
		SCBigInteger			operator - (const SCBigInteger &bi) const;
		SCBigInteger			operator * (const SCBigInteger &bi) const;
		SCBigInteger			operator / (const SCBigInteger &bi) const;
		SCBigInteger			operator % (const SCBigInteger &bi) const;

		SCBigInteger			GCD(const SCBigInteger &bi) const;
		SCBigInteger			ModInverse(const SCBigInteger &bi) const;
		SCBigInteger			ModPow(const SCBigInteger &e, const SCBigInteger &m) const;

		// Test
		SCBigInteger			ModPowOld(const SCBigInteger &e, const SCBigInteger &m) const;

		// At some point (usually a division) an error took place.
		bool					IsNan()
									{
										return isNan;
									}

		bool					IsEven() const;
		bool					IsOdd() const
									{
										return !IsEven();
									}

		std::string				ToString() const;

	private:
		/*
		 *	Internal support
		 */
		
		void					Realloc(uint32_t size);

		void					MulAdd(uint32_t mul, uint32_t add);
		void					MulAdd(const SCBigInteger &i, uint32_t mul, uint32_t shift);
		uint32_t				DivRemain(uint32_t div);
		void					DivRemain(const SCBigInteger &i, SCBigInteger &m);
		int						CompareTo(const SCBigInteger &bi) const;
		int						CompareAbs(const SCBigInteger &bi) const;

		void					ShiftLeft(uint32_t bits);
		void					ShiftRight(uint32_t bits);
		void					SetBit(uint32_t bit);
		bool					BitTest(uint32_t index) const;

		void					AddInternal(const SCBigInteger &bi);
		void					SubInternal(const SCBigInteger &bi);

		uint32_t				GetBitLength() const;
		uint32_t				GetLowestSetBit() const;
		bool					PassRabinMiller(int iter) const;

		SCBigInteger			ModPowOdd(const SCBigInteger &e, const SCBigInteger &m) const;

		/*
		 *	Values are stored in the array with the least significant byte
		 *	at index 0
		 */

		uint32_t				dataAlloc;			// size of dataArray
		uint32_t				*dataArray;			// unsigned number value

		uint32_t				dataSize;			// size of representation in words
		bool					isNeg;				// true if negative
		bool					isNan;

		/*
		 *	SCRSAKey implements various efficient algorithms which accelerate
		 *	ModPow, in order to accelerate the transform method
		 */

	friend class SCMontMath;
};

/************************************************************************/
/*																		*/
/*	Montgomery Exponent Support											*/
/*																		*/
/************************************************************************/

/*	SCMontMath
 *
 *		Stores a modulus along with certain cached values from algorithms
 *	14.94 and 14.32 for Montgomery exponentiation in chapter 14 of the
 *	Handbook of Applied Cryptography. This allows us to track certain
 *	intermediate values which are constants associated with a given
 *	modulus, and allows us to perform rapid exponentiation. This is also
 *	used by the SCRSAKey class, which needs to perform fast
 *	exponeniation on the same modulus for a large number of input values.
 */

class SCMontMath
{
	public:
							SCMontMath()
								{
								}
							SCMontMath(const SCBigInteger &mod)
								{
									m = mod;
									mrinit = false;
								}
							SCMontMath(const SCMontMath &k)
								{
									m = k.m;
									mrinit = false;
								}
							~SCMontMath()
								{
								}

		SCMontMath			&operator = (const SCMontMath &k)
								{
									m = k.m;
									mrinit = false;
									return *this;
								}

		SCMontMath			&operator = (const SCBigInteger &mod)
								{
									m = mod;
									mrinit = false;
									return *this;
								}

		const SCBigInteger	&Modulus() const
								{
									return m;
								}

		/*
		 *	Perform v.ModPow(e,m) in an efficient fashion, caching 
		 *	intermediate values as needed
		 */

		SCBigInteger		ExpMod(const SCBigInteger &val, const SCBigInteger &exp);
	private:
		SCBigInteger		m;

		bool				mrinit;

		uint32_t			minv;
		SCBigInteger		rmod;
		SCBigInteger		r2mod;

		void				MontInit();

		SCBigInteger		MontMult(const SCBigInteger &x,
									 const SCBigInteger &y);
};



#endif /* SCBigInteger_hpp */
