/*
 * Unitto is a calculator for Android
 * Copyright (c) 2022-2026 Elshan Agaev
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

package com.sadellie.unitto.core.data.converter

import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.isEqualTo
import com.sadellie.unitto.core.common.isLessThan
import com.sadellie.unitto.core.common.setMaxScale
import com.sadellie.unitto.core.data.converter.collections.accelerationCollection
import com.sadellie.unitto.core.data.converter.collections.angleCollection
import com.sadellie.unitto.core.data.converter.collections.areaCollection
import com.sadellie.unitto.core.data.converter.collections.currencyCollection
import com.sadellie.unitto.core.data.converter.collections.dataCollection
import com.sadellie.unitto.core.data.converter.collections.dataTransferCollection
import com.sadellie.unitto.core.data.converter.collections.electrostaticCapacitance
import com.sadellie.unitto.core.data.converter.collections.energyCollection
import com.sadellie.unitto.core.data.converter.collections.flowRateCollection
import com.sadellie.unitto.core.data.converter.collections.fluxCollection
import com.sadellie.unitto.core.data.converter.collections.forceCollection
import com.sadellie.unitto.core.data.converter.collections.fuelConsumptionCollection
import com.sadellie.unitto.core.data.converter.collections.lengthCollection
import com.sadellie.unitto.core.data.converter.collections.luminanceCollection
import com.sadellie.unitto.core.data.converter.collections.massCollection
import com.sadellie.unitto.core.data.converter.collections.numberBaseCollection
import com.sadellie.unitto.core.data.converter.collections.powerCollection
import com.sadellie.unitto.core.data.converter.collections.prefixCollection
import com.sadellie.unitto.core.data.converter.collections.pressureCollection
import com.sadellie.unitto.core.data.converter.collections.speedCollection
import com.sadellie.unitto.core.data.converter.collections.temperatureCollection
import com.sadellie.unitto.core.data.converter.collections.timeCollection
import com.sadellie.unitto.core.data.converter.collections.torqueCollection
import com.sadellie.unitto.core.data.converter.collections.volumeCollection
import com.sadellie.unitto.core.model.converter.UnitGroup
import com.sadellie.unitto.core.model.converter.unit.BasicUnit
import io.github.sadellie.evaluatto.Expression

/** Every unit the converter knows, in Unitto's order. */
object Units {
  val all: List<BasicUnit> by lazy {
    lengthCollection +
      currencyCollection +
      massCollection +
      speedCollection +
      temperatureCollection +
      areaCollection +
      timeCollection +
      volumeCollection +
      dataCollection +
      pressureCollection +
      accelerationCollection +
      energyCollection +
      powerCollection +
      angleCollection +
      dataTransferCollection +
      fluxCollection +
      numberBaseCollection +
      electrostaticCapacitance +
      prefixCollection +
      forceCollection +
      torqueCollection +
      flowRateCollection +
      luminanceCollection +
      fuelConsumptionCollection
  }

  fun byId(id: String): BasicUnit? = all.firstOrNull { it.id == id }

  fun inGroup(group: UnitGroup): List<BasicUnit> = all.filter { it.group == group }
}

/**
 * Converts a value between two units of one group. Unitto's repository, without its database:
 * the conversions are Unitto's own, and a currency's rate comes from [rate], which answers from
 * the rates kept on the phone or not at all.
 *
 * [value] is what was typed, and may be an expression ("12+3"), as in Unitto.
 */
class Converter(private val rate: (fromId: String, toId: String) -> KBigDecimal?) {

  fun convert(unitFrom: BasicUnit, unitTo: BasicUnit, value: String): ConverterResult =
    when {
      unitFrom.group == UnitGroup.NUMBER_BASE && unitTo.group == UnitGroup.NUMBER_BASE ->
        ConverterResult.NumberBase(
          (unitFrom as BasicUnit.NumberBase).convert(unitTo as BasicUnit.NumberBase, value)
        )

      unitFrom.group == UnitGroup.CURRENCY && unitTo.group == UnitGroup.CURRENCY -> {
        val input = calculateInput(value)
        val pairRate = rate(unitFrom.id, unitTo.id)
        if (pairRate == null) ConverterResult.Error.CurrencyError
        else ConverterResult.Default(value = input.multiply(pairRate).setMaxScale(), calculation = input)
      }

      // foot and inches output
      unitTo.id == UnitID.foot ->
        convertFoot(
          footUnit = unitTo as BasicUnit.Default,
          inchUnit = Units.byId(UnitID.inch) as BasicUnit.Default,
          feetInput = (unitFrom as BasicUnit.Default).convert(unitTo, calculateInput(value)),
        )

      // pound and ounces output
      unitTo.id == UnitID.pound ->
        convertPound(
          poundUnit = unitTo as BasicUnit.Default,
          ounceUnit = Units.byId(UnitID.ounce) as BasicUnit.Default,
          poundsInput = (unitFrom as BasicUnit.Default).convert(unitTo, calculateInput(value)),
        )

      else -> {
        val input = calculateInput(value)
        ConverterResult.Default((unitFrom as BasicUnit.Default).convert(unitTo as BasicUnit.Default, input), input)
      }
    }

  /** A length of time written out in days, hours, minutes and so on, beside the plain number. */
  fun timeBreakdown(unitFrom: BasicUnit, value: String): ConverterResult.Time =
    convertTimeAndFormatToHumanReadable(unitFrom as BasicUnit.Default, calculateInput(value))

  private fun convertTimeAndFormatToHumanReadable(
    unitFrom: BasicUnit.Default,
    value: KBigDecimal,
  ): ConverterResult.Time {
    if (value.isEqualTo(KBigDecimal.ZERO)) return ConverterResult.Time()

    val input = value.multiply(unitFrom.factor)

    val negative = input < KBigDecimal.ZERO
    val inputAbs = input.abs()

    if (inputAbs.isLessThan(nanosecondBasicUnit)) {
      // no conversion needed, will be definitely formatted as attoseconds
      return ConverterResult.Time(
        negative = negative,
        day = KBigDecimal.ZERO,
        hour = KBigDecimal.ZERO,
        minute = KBigDecimal.ZERO,
        second = KBigDecimal.ZERO,
        millisecond = KBigDecimal.ZERO,
        microsecond = KBigDecimal.ZERO,
        nanosecond = KBigDecimal.ZERO,
        attosecond = inputAbs,
      )
    }

    // DAY
    var division = inputAbs.divideAndRemainder(dayBasicUnit)
    val day = division.component1().setScale(0, KRoundingMode.HALF_EVEN)
    var remainingSeconds = division.component2().setScale(0, KRoundingMode.HALF_EVEN)

    division = remainingSeconds.divideAndRemainder(hourBasicUnit)
    val hour = division.component1()
    remainingSeconds = division.component2()

    division = remainingSeconds.divideAndRemainder(minuteBasicUnit)
    val minute = division.component1()
    remainingSeconds = division.component2()

    division = remainingSeconds.divideAndRemainder(secondBasicUnit)
    val second = division.component1()
    remainingSeconds = division.component2()

    division = remainingSeconds.divideAndRemainder(millisecondBasicUnit)
    val millisecond = division.component1()
    remainingSeconds = division.component2()

    division = remainingSeconds.divideAndRemainder(microsecondBasicUnit)
    val microsecond = division.component1()
    remainingSeconds = division.component2()

    division = remainingSeconds.divideAndRemainder(nanosecondBasicUnit)
    val nanosecond = division.component1()
    remainingSeconds = division.component2()

    val attosecond = remainingSeconds

    return ConverterResult.Time(
      negative = negative,
      day = day,
      hour = hour,
      minute = minute,
      second = second,
      millisecond = millisecond,
      microsecond = microsecond,
      nanosecond = nanosecond,
      attosecond = attosecond,
    )
  }

  private fun convertFoot(
    footUnit: BasicUnit.Default,
    inchUnit: BasicUnit.Default,
    feetInput: KBigDecimal,
  ): ConverterResult.FootInch {
    val (integral, fractional) = feetInput.divideAndRemainder(KBigDecimal.ONE)
    val fractionInInches = footUnit.convert(inchUnit, fractional)

    return ConverterResult.FootInch(integral, fractionInInches)
  }

  private fun convertPound(
    poundUnit: BasicUnit.Default,
    ounceUnit: BasicUnit.Default,
    poundsInput: KBigDecimal,
  ): ConverterResult.PoundOunce {
    val (integral, fractional) = poundsInput.divideAndRemainder(KBigDecimal.ONE)
    val fractionInOunces = poundUnit.convert(ounceUnit, fractional)

    return ConverterResult.PoundOunce(integral, fractionInOunces)
  }

  private fun calculateInput(value: String): KBigDecimal = Expression(value).calculate()
}

private val dayBasicUnit by lazy { KBigDecimal("86400000000000000000000") }
private val hourBasicUnit by lazy { KBigDecimal("3600000000000000000000") }
private val minuteBasicUnit by lazy { KBigDecimal("60000000000000000000") }
private val secondBasicUnit by lazy { KBigDecimal("1000000000000000000") }
private val millisecondBasicUnit by lazy { KBigDecimal("1000000000000000") }
private val microsecondBasicUnit by lazy { KBigDecimal("1000000000000") }
private val nanosecondBasicUnit by lazy { KBigDecimal("1000000000") }
