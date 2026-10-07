/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2025 Elshan Agaev
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

package com.sadellie.unitto.core.data.converter.collections

import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.data.converter.UnitID
import com.sadellie.unitto.core.model.converter.UnitGroup
import com.sadellie.unitto.core.model.converter.unit.BasicUnit
import com.sadellie.unitto.core.model.converter.unit.NormalUnit
import com.wanderwildwood.soroban.R

internal val dataCollection: List<BasicUnit> by lazy {
  listOf(
    NormalUnit(
      UnitID.bit,
      KBigDecimal("1"),
      UnitGroup.DATA,
      R.string.unit_bit,
      R.string.unit_bit_short,
    ),
    NormalUnit(
      UnitID.kibibit,
      KBigDecimal("1024"),
      UnitGroup.DATA,
      R.string.unit_kibibit,
      R.string.unit_kibibit_short,
    ),
    NormalUnit(
      UnitID.kilobit,
      KBigDecimal("1000"),
      UnitGroup.DATA,
      R.string.unit_kilobit,
      R.string.unit_kilobit_short,
    ),
    NormalUnit(
      UnitID.megabit,
      KBigDecimal("1000000"),
      UnitGroup.DATA,
      R.string.unit_megabit,
      R.string.unit_megabit_short,
    ),
    NormalUnit(
      UnitID.mebibit,
      KBigDecimal("1048576"),
      UnitGroup.DATA,
      R.string.unit_mebibit,
      R.string.unit_mebibit_short,
    ),
    NormalUnit(
      UnitID.gigabit,
      KBigDecimal("1000000000"),
      UnitGroup.DATA,
      R.string.unit_gigabit,
      R.string.unit_gigabit_short,
    ),
    NormalUnit(
      UnitID.gibibit,
      KBigDecimal("1073741824"),
      UnitGroup.DATA,
      R.string.unit_gibibit,
      R.string.unit_gibibit_short,
    ),
    NormalUnit(
      UnitID.terabit,
      KBigDecimal("1000000000000"),
      UnitGroup.DATA,
      R.string.unit_terabit,
      R.string.unit_terabit_short,
    ),
    NormalUnit(
      UnitID.petabit,
      KBigDecimal("1000000000000000"),
      UnitGroup.DATA,
      R.string.unit_petabit,
      R.string.unit_petabit_short,
    ),
    NormalUnit(
      UnitID.exabit,
      KBigDecimal("1000000000000000000"),
      UnitGroup.DATA,
      R.string.unit_exabit,
      R.string.unit_exabit_short,
    ),
    NormalUnit(
      UnitID.byte,
      KBigDecimal("8"),
      UnitGroup.DATA,
      R.string.unit_byte,
      R.string.unit_byte_short,
    ),
    NormalUnit(
      UnitID.kibibyte,
      KBigDecimal("8192"),
      UnitGroup.DATA,
      R.string.unit_kibibyte,
      R.string.unit_kibibyte_short,
    ),
    NormalUnit(
      UnitID.kilobyte,
      KBigDecimal("8000"),
      UnitGroup.DATA,
      R.string.unit_kilobyte,
      R.string.unit_kilobyte_short,
    ),
    NormalUnit(
      UnitID.megabyte,
      KBigDecimal("8000000"),
      UnitGroup.DATA,
      R.string.unit_megabyte,
      R.string.unit_megabyte_short,
    ),
    NormalUnit(
      UnitID.mebibyte,
      KBigDecimal("8388608"),
      UnitGroup.DATA,
      R.string.unit_mebibyte,
      R.string.unit_mebibyte_short,
    ),
    NormalUnit(
      UnitID.gigabyte,
      KBigDecimal("8000000000"),
      UnitGroup.DATA,
      R.string.unit_gigabyte,
      R.string.unit_gigabyte_short,
    ),
    NormalUnit(
      UnitID.gibibyte,
      KBigDecimal("8589934592"),
      UnitGroup.DATA,
      R.string.unit_gibibyte,
      R.string.unit_gibibyte_short,
    ),
    NormalUnit(
      UnitID.terabyte,
      KBigDecimal("8000000000000"),
      UnitGroup.DATA,
      R.string.unit_terabyte,
      R.string.unit_terabyte_short,
    ),
    NormalUnit(
      UnitID.petabyte,
      KBigDecimal("8000000000000000"),
      UnitGroup.DATA,
      R.string.unit_petabyte,
      R.string.unit_petabyte_short,
    ),
    NormalUnit(
      UnitID.exabyte,
      KBigDecimal("8000000000000000000"),
      UnitGroup.DATA,
      R.string.unit_exabyte,
      R.string.unit_exabyte_short,
    ),
  )
}
