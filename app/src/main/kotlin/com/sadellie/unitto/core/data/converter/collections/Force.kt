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

val forceCollection: List<BasicUnit> by lazy {
  listOf(
    NormalUnit(
      UnitID.attonewton,
      KBigDecimal("1"),
      UnitGroup.FORCE,
      R.string.unit_attonewton,
      R.string.unit_attonewton_short,
    ),
    NormalUnit(
      UnitID.dyne,
      KBigDecimal("10000000000000"),
      UnitGroup.FORCE,
      R.string.unit_dyne,
      R.string.unit_dyne_short,
    ),
    NormalUnit(
      UnitID.millinewton,
      KBigDecimal("1000000000000000"),
      UnitGroup.FORCE,
      R.string.unit_millinewton,
      R.string.unit_millinewton_short,
    ),
    NormalUnit(
      UnitID.joule_per_centimeter,
      KBigDecimal("10000000000000000"),
      UnitGroup.FORCE,
      R.string.unit_joule_per_centimeter,
      R.string.unit_joule_per_centimeter_short,
    ),
    NormalUnit(
      UnitID.newton,
      KBigDecimal("1000000000000000000"),
      UnitGroup.FORCE,
      R.string.unit_newton,
      R.string.unit_newton_short,
    ),
    NormalUnit(
      UnitID.joule_per_meter,
      KBigDecimal("1000000000000000000"),
      UnitGroup.FORCE,
      R.string.unit_joule_per_meter,
      R.string.unit_joule_per_meter_short,
    ),
    NormalUnit(
      UnitID.kilonewton,
      KBigDecimal("1000000000000000000000"),
      UnitGroup.FORCE,
      R.string.unit_kilonewton,
      R.string.unit_kilonewton_short,
    ),
    NormalUnit(
      UnitID.gram_force,
      KBigDecimal("9806650000000000"),
      UnitGroup.FORCE,
      R.string.unit_gram_force,
      R.string.unit_gram_force_short,
    ),
    NormalUnit(
      UnitID.kilogram_force,
      KBigDecimal("9806650000000000000"),
      UnitGroup.FORCE,
      R.string.unit_kilogram_force,
      R.string.unit_kilogram_force_short,
    ),
    NormalUnit(
      UnitID.ton_force,
      KBigDecimal("9806650000000000000000"),
      UnitGroup.FORCE,
      R.string.unit_ton_force,
      R.string.unit_ton_force_short,
    ),
    NormalUnit(
      UnitID.ounce_force,
      KBigDecimal("278013850953423000"),
      UnitGroup.FORCE,
      R.string.unit_ounce_force,
      R.string.unit_ounce_force_short,
    ),
    NormalUnit(
      UnitID.pound_force,
      KBigDecimal("4448221615255000000"),
      UnitGroup.FORCE,
      R.string.unit_pound_force,
      R.string.unit_pound_force_short,
    ),
    NormalUnit(
      UnitID.kilopound_force,
      KBigDecimal("4448221615255000000000"),
      UnitGroup.FORCE,
      R.string.unit_kilopound_force,
      R.string.unit_kilopound_force_short,
    ),
    NormalUnit(
      UnitID.pond,
      KBigDecimal("9806650000000000"),
      UnitGroup.FORCE,
      R.string.unit_pond,
      R.string.unit_pond_short,
    ),
    NormalUnit(
      UnitID.kilopond,
      KBigDecimal("9806650000000000000"),
      UnitGroup.FORCE,
      R.string.unit_kilopond,
      R.string.unit_kilopond_short,
    ),
  )
}
