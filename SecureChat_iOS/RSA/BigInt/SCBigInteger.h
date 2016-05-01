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

#define BIUSE32BIT		1

#ifdef BIUSE32BIT
	typedef uint32_t	BIWORD;
	typedef int32_t		SIGNED_BIWORD;
#else
	typedef uint16_t	BIWORD;
	typedef int16_t		SIGNED_BIWORD;
#endif

/*	SCBigInteger
 *
 *		Big integer class.
 */

// http://developer.classpath.org/doc/java/math/BigInteger-source.html

class SCBigInteger
{
	public:
								SCBigInteger(SIGNED_BIWORD primVal = 0);
								SCBigInteger(BIWORD primVal, bool neg);
								SCBigInteger(std::string val);
								SCBigInteger(const BIWORD *data, const size_t nwords, bool neg = false);
								~SCBigInteger();

								SCBigInteger(const SCBigInteger &bi);
		SCBigInteger			&operator = (const SCBigInteger &bi);

		/*
		 *	Access to the raw data array of the value
		 */

		const BIWORD			*GetData() const
									{
										return dataArray;
									}
		const size_t			GetDataSize() const
									{
										return dataSize;
									}
		bool					IsNegative() const
									{
										return isNeg;
									}

		static SCBigInteger		ProbablePrime(int nbits);
		static SCBigInteger		Random(size_t nbits);
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
		
		void					Realloc(size_t size);

		void					MulAdd(BIWORD mul, BIWORD add);
		void					MulAdd(const SCBigInteger &i, BIWORD mul, size_t shift);
		BIWORD					DivRemain(BIWORD div);
		void					DivRemain(const SCBigInteger &i, SCBigInteger &m);
		int						CompareTo(const SCBigInteger &bi) const;
		int						CompareAbs(const SCBigInteger &bi) const;

		void					ShiftLeft(size_t bits);
		void					ShiftRight(size_t bits);
		void					SetBit(size_t bit);
		bool					BitTest(size_t index) const;

		void					AddInternal(const SCBigInteger &bi);
		void					SubInternal(const SCBigInteger &bi);

		size_t					GetBitLength() const;
		size_t					GetLowestSetBit() const;
		bool					PassRabinMiller(int iter) const;

		SCBigInteger			ModPowOdd(const SCBigInteger &e, const SCBigInteger &m) const;

		/*
		 *	Montgomery Multiplication support
		 *
		 *	When generating an RSA key these two routines are called a lot.
		 *	So they are highly optimized versions of other routines we have
		 *	elsewhere
		 */

		void					RightShiftWord();
		void					MulAdd(const SCBigInteger &i, BIWORD mul);

		/*
		 *	Values are stored in the array with the least significant byte
		 *	at index 0
		 */

		size_t					dataAlloc;			// size of dataArray
		BIWORD					*dataArray;			// unsigned number value

		size_t					dataSize;			// size of representation in words
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

		BIWORD				minv;
		SCBigInteger		rmod;
		SCBigInteger		r2mod;

		void				MontInit();

		SCBigInteger		MontMult(const SCBigInteger &x,
									 const SCBigInteger &y);
};



#endif /* SCBigInteger_hpp */
