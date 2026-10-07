/*
 * Unitto is a calculator for Android
 * Copyright (c) 2025-2026 Elshan Agaev
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
import com.sadellie.unitto.core.common.setMaxScale
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitRepositoryConvertFootInch {
  private val converter = Converter { _, _ -> null }

  @Test
  fun convert_footInchOutput() {
    val expected =
      ConverterResult.FootInch(foot = KBigDecimal("6").setMaxScale(), inch = KBigDecimal("0"))
    val actual =
      converter.convert(Units.byId(UnitID.inch)!!, Units.byId(UnitID.foot)!!, "72")

    assertEquals(expected, actual)
  }
}
