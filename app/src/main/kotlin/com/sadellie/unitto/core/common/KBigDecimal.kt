/*
 * Unitto is a calculator for Android
 * Copyright (c) 2025 Elshan Agaev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sadellie.unitto.core.common

import ch.obermuhlner.math.big.BigDecimalMath
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

class KBigDecimal(internal val wrapped: BigDecimal) : Comparable<KBigDecimal> {
  override fun equals(other: Any?): Boolean {
    if (other !is KBigDecimal) return false
    return this.wrapped == other.wrapped
  }

  override fun toString(): String = this.wrapped.toString()

  override fun hashCode(): Int = this.wrapped.hashCode()

  constructor(string: String) : this(BigDecimal(string))

  constructor(double: Double) : this(BigDecimal(double))

  companion object {
    val ZERO: KBigDecimal = KBigDecimal(BigDecimal.ZERO)
    val ONE: KBigDecimal = KBigDecimal(BigDecimal.ONE)
    val TEN: KBigDecimal = KBigDecimal(BigDecimal.TEN)

    fun valueOf(double: Double): KBigDecimal = KBigDecimal(BigDecimal.valueOf(double))

    fun valueOf(long: Long): KBigDecimal = KBigDecimal(BigDecimal.valueOf(long))
  }

  override operator fun compareTo(other: KBigDecimal): Int =
    this.wrapped.compareTo(other.wrapped)

  fun stripTrailingZeros(): KBigDecimal = KBigDecimal(this.wrapped.stripTrailingZeros())

  fun setScale(scale: Int, roundingMode: KRoundingMode): KBigDecimal =
    KBigDecimal(this.wrapped.setScale(scale, roundingMode.wrapped))

  fun setScale(scale: Int): KBigDecimal = KBigDecimal(this.wrapped.setScale(scale))

  fun scale(): Int = this.wrapped.scale()

  fun abs(): KBigDecimal = KBigDecimal(this.wrapped.abs())

  fun remainder(divisor: KBigDecimal): KBigDecimal =
    KBigDecimal(this.wrapped.remainder(divisor.wrapped))

  fun toPlainString(): String = this.wrapped.toPlainString()

  fun toEngineeringString(): String = this.wrapped.toEngineeringString()

  fun intValueExact(): Int = this.wrapped.intValueExact()

  fun multiply(multiplier: KBigDecimal): KBigDecimal =
    KBigDecimal(this.wrapped.multiply(multiplier.wrapped))

  fun div(divisor: KBigDecimal): KBigDecimal = KBigDecimal(this.wrapped.div(divisor.wrapped))

  operator fun plus(addend: KBigDecimal): KBigDecimal =
    KBigDecimal(this.wrapped.plus(addend.wrapped))

  operator fun minus(subtrahend: KBigDecimal): KBigDecimal =
    KBigDecimal(this.wrapped.minus(subtrahend.wrapped))

  fun divide(divisor: KBigDecimal, scale: Int, roundingMode: KRoundingMode): KBigDecimal =
    KBigDecimal(this.wrapped.divide(divisor.wrapped, scale, roundingMode.wrapped))

  operator fun unaryMinus(): KBigDecimal = KBigDecimal(this.wrapped.unaryMinus())

  fun divide(divisor: KBigDecimal, mathContext: KMathContext): KBigDecimal =
    KBigDecimal(this.wrapped.divide(divisor.wrapped, mathContext.wrapped))

  fun add(addend: KBigDecimal): KBigDecimal = KBigDecimal(this.wrapped.add(addend.wrapped))

  constructor(
    string: String,
    mathContext: KMathContext,
  ) : this(BigDecimal(string, mathContext.wrapped))

  fun toBigInteger(): KBigInteger = KBigInteger(this.wrapped.toBigInteger())

  fun toInt(): Int = this.wrapped.toInt()

  constructor(bigInteger: KBigInteger) : this(BigDecimal(bigInteger.wrapped))

  constructor(int: Int) : this(BigDecimal(int))

  operator fun times(bigDecimal: KBigDecimal): KBigDecimal =
    KBigDecimal(this.wrapped.times(bigDecimal.wrapped))

  fun divideAndRemainder(divisor: KBigDecimal): Array<KBigDecimal> {
    val res = this.wrapped.divideAndRemainder(divisor.wrapped)
    return arrayOf(KBigDecimal(res[0]), KBigDecimal(res[1]))
  }

  fun pow(n: Int): KBigDecimal = KBigDecimal(this.wrapped.pow(n))

  fun scaleByPowerOfTen(n: Int): KBigDecimal = KBigDecimal(this.wrapped.scaleByPowerOfTen(n))
}

class KRoundingMode internal constructor(val wrapped: RoundingMode) {

  override fun equals(other: Any?): Boolean {
    if (other !is KRoundingMode) return false
    return this.wrapped == other.wrapped
  }

  override fun toString(): String = this.wrapped.toString()

  override fun hashCode(): Int = this.wrapped.hashCode()

  companion object {
    val HALF_EVEN: KRoundingMode = KRoundingMode(RoundingMode.HALF_EVEN)
    val DOWN: KRoundingMode = KRoundingMode(RoundingMode.DOWN)
  }
}

class KMathContext private constructor(val wrapped: MathContext) {
  override fun equals(other: Any?): Boolean {
    if (other !is KMathContext) return false
    return this.wrapped == other.wrapped
  }

  override fun toString(): String = this.wrapped.toString()

  override fun hashCode(): Int = this.wrapped.hashCode()

  constructor(
    precision: Int,
    roundingMode: KRoundingMode,
  ) : this(MathContext(precision, roundingMode.wrapped))

  val precision: Int = this.wrapped.precision
}

class KBigInteger internal constructor(internal val wrapped: BigInteger) :
  Comparable<KBigInteger> {
  override fun equals(other: Any?): Boolean {
    if (other !is KBigInteger) return false
    return this.wrapped == other.wrapped
  }

  override fun toString(): String = this.wrapped.toString()

  fun toString(radix: Int): String = this.wrapped.toString(radix)

  override fun hashCode(): Int = this.wrapped.hashCode()

  fun gcd(d: KBigInteger): KBigInteger = KBigInteger(this.wrapped.gcd(d.wrapped))

  fun divide(divisor: KBigInteger): KBigInteger =
    KBigInteger(this.wrapped.divide(divisor.wrapped))

  companion object {
    val ONE: KBigInteger = KBigInteger(BigInteger.ONE)
    val ZERO: KBigInteger = KBigInteger(BigInteger.ZERO)
    val TEN: KBigInteger = KBigInteger(BigInteger.TEN)
  }

  fun pow(n: Int): KBigInteger = KBigInteger(this.wrapped.pow(n))

  fun toKBigDecimal(): KBigDecimal = KBigDecimal(this.wrapped.toBigDecimal())

  operator fun div(other: KBigInteger): KBigInteger =
    KBigInteger(this.wrapped.div(other.wrapped))

  operator fun minus(other: KBigInteger): KBigInteger =
    KBigInteger(this.wrapped.minus(other.wrapped))

  fun multiply(other: KBigInteger): KBigInteger =
    KBigInteger(this.wrapped.multiply(other.wrapped))

  constructor(value: String) : this(BigInteger(value))

  constructor(value: String, radix: Int) : this(BigInteger(value, radix))

  override fun compareTo(other: KBigInteger): Int = this.wrapped.compareTo(other.wrapped)
}

class KBigDecimalMath {
  companion object {
    fun toRadians(bigDecimal: KBigDecimal, mathContext: KMathContext): KBigDecimal =
      KBigDecimal(BigDecimalMath.toRadians(bigDecimal.wrapped, mathContext.wrapped))

    fun sin(bigDecimal: KBigDecimal, mathContext: KMathContext): KBigDecimal =
      KBigDecimal(BigDecimalMath.sin(bigDecimal.wrapped, mathContext.wrapped))

    fun asin(bigDecimal: KBigDecimal, mathContext: KMathContext): KBigDecimal =
      KBigDecimal(BigDecimalMath.asin(bigDecimal.wrapped, mathContext.wrapped))

    fun toDegrees(bigDecimal: KBigDecimal, mathContext: KMathContext): KBigDecimal =
      KBigDecimal(BigDecimalMath.toDegrees(bigDecimal.wrapped, mathContext.wrapped))

    fun cos(bigDecimal: KBigDecimal, mathContext: KMathContext): KBigDecimal =
      KBigDecimal(BigDecimalMath.cos(bigDecimal.wrapped, mathContext.wrapped))

    fun acos(bigDecimal: KBigDecimal, mathContext: KMathContext): KBigDecimal =
      KBigDecimal(BigDecimalMath.acos(bigDecimal.wrapped, mathContext.wrapped))

    fun tan(bigDecimal: KBigDecimal, mathContext: KMathContext): KBigDecimal =
      KBigDecimal(BigDecimalMath.tan(bigDecimal.wrapped, mathContext.wrapped))

    fun atan(bigDecimal: KBigDecimal, mathContext: KMathContext): KBigDecimal =
      KBigDecimal(BigDecimalMath.atan(bigDecimal.wrapped, mathContext.wrapped))

    fun log(bigDecimal: KBigDecimal, mathContext: KMathContext): KBigDecimal =
      KBigDecimal(BigDecimalMath.log(bigDecimal.wrapped, mathContext.wrapped))

    fun log10(bigDecimal: KBigDecimal, mathContext: KMathContext): KBigDecimal =
      KBigDecimal(BigDecimalMath.log10(bigDecimal.wrapped, mathContext.wrapped))

    fun exp(bigDecimal: KBigDecimal, mathContext: KMathContext): KBigDecimal =
      KBigDecimal(BigDecimalMath.exp(bigDecimal.wrapped, mathContext.wrapped))

    fun pi(mathContext: KMathContext): KBigDecimal =
      KBigDecimal(BigDecimalMath.pi(mathContext.wrapped))

    fun e(mathContext: KMathContext): KBigDecimal =
      KBigDecimal(BigDecimalMath.e(mathContext.wrapped))

    fun sqrt(bigDecimal: KBigDecimal, mathContext: KMathContext): KBigDecimal =
      KBigDecimal(BigDecimalMath.sqrt(bigDecimal.wrapped, mathContext.wrapped))

    fun pow(expr: KBigDecimal, factor: KBigDecimal, mathContext: KMathContext): KBigDecimal =
      KBigDecimal(BigDecimalMath.pow(expr.wrapped, factor.wrapped, mathContext.wrapped))
  }
}
