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

internal val lengthCollection: List<BasicUnit> by lazy {
  listOf(
    NormalUnit(
      UnitID.attometer,
      KBigDecimal("1"),
      UnitGroup.LENGTH,
      R.string.unit_attometer,
      R.string.unit_attometer_short,
    ),
    NormalUnit(
      UnitID.nanometer,
      KBigDecimal("1000000000"),
      UnitGroup.LENGTH,
      R.string.unit_nanometer,
      R.string.unit_nanometer_short,
    ),
    NormalUnit(
      UnitID.micrometer,
      KBigDecimal("1000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_micrometer,
      R.string.unit_micrometer_short,
    ),
    NormalUnit(
      UnitID.millimeter,
      KBigDecimal("1000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_millimeter,
      R.string.unit_millimeter_short,
    ),
    NormalUnit(
      UnitID.centimeter,
      KBigDecimal("10000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_centimeter,
      R.string.unit_centimeter_short,
    ),
    NormalUnit(
      UnitID.decimeter,
      KBigDecimal("100000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_decimeter,
      R.string.unit_decimeter_short,
    ),
    NormalUnit(
      UnitID.meter,
      KBigDecimal("1000000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_meter,
      R.string.unit_meter_short,
    ),
    NormalUnit(
      UnitID.kilometer,
      KBigDecimal("1000000000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_kilometer,
      R.string.unit_kilometer_short,
    ),
    NormalUnit(
      UnitID.nautical_mile,
      KBigDecimal("1852000000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_nautical_mile,
      R.string.unit_nautical_mile_short,
    ),
    NormalUnit(
      UnitID.inch,
      KBigDecimal("25400000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_inch,
      R.string.unit_inch_short,
    ),
    NormalUnit(
      UnitID.foot,
      KBigDecimal("304800000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_foot,
      R.string.unit_foot_short,
    ),
    NormalUnit(
      UnitID.yard,
      KBigDecimal("914400000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_yard,
      R.string.unit_yard_short,
    ),
    NormalUnit(
      UnitID.mile,
      KBigDecimal("1609344000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_mile,
      R.string.unit_mile_short,
    ),
    NormalUnit(
      UnitID.light_year,
      KBigDecimal("9460730472000000000000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_light_year,
      R.string.unit_light_year_short,
    ),
    NormalUnit(
      UnitID.parsec,
      KBigDecimal("30856775814913600000000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_parsec,
      R.string.unit_parsec_short,
    ),
    NormalUnit(
      UnitID.kiloparsec,
      KBigDecimal("30856775814913600000000000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_kiloparsec,
      R.string.unit_kiloparsec_short,
    ),
    NormalUnit(
      UnitID.megaparsec,
      KBigDecimal("30856775814913600000000000000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_megaparsec,
      R.string.unit_megaparsec_short,
    ),
    NormalUnit(
      UnitID.mercury_equatorial_radius,
      KBigDecimal("2439700000000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_mercury_equatorial_radius,
      R.string.unit_mercury_equatorial_radius_short,
    ),
    NormalUnit(
      UnitID.venus_equatorial_radius,
      KBigDecimal("6051800000000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_venus_equatorial_radius,
      R.string.unit_venus_equatorial_radius_short,
    ),
    NormalUnit(
      UnitID.earth_equatorial_radius,
      KBigDecimal("6371000000000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_earth_equatorial_radius,
      R.string.unit_earth_equatorial_radius_short,
    ),
    NormalUnit(
      UnitID.mars_equatorial_radius,
      KBigDecimal("3389500000000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_mars_equatorial_radius,
      R.string.unit_mars_equatorial_radius_short,
    ),
    NormalUnit(
      UnitID.jupiter_equatorial_radius,
      KBigDecimal("69911000000000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_jupiter_equatorial_radius,
      R.string.unit_jupiter_equatorial_radius_short,
    ),
    NormalUnit(
      UnitID.saturn_equatorial_radius,
      KBigDecimal("58232000000000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_saturn_equatorial_radius,
      R.string.unit_saturn_equatorial_radius_short,
    ),
    NormalUnit(
      UnitID.uranus_equatorial_radius,
      KBigDecimal("25362000000000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_uranus_equatorial_radius,
      R.string.unit_uranus_equatorial_radius_short,
    ),
    NormalUnit(
      UnitID.neptune_equatorial_radius,
      KBigDecimal("24622000000000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_neptune_equatorial_radius,
      R.string.unit_neptune_equatorial_radius_short,
    ),
    NormalUnit(
      UnitID.sun_equatorial_radius,
      KBigDecimal("695508000000000000000000000"),
      UnitGroup.LENGTH,
      R.string.unit_sun_equatorial_radius,
      R.string.unit_sun_equatorial_radius_short,
    ),
  )
}
