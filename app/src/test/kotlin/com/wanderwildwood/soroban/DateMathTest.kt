package com.wanderwildwood.soroban

import com.wanderwildwood.soroban.dates.Between
import com.wanderwildwood.soroban.dates.DateMath
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DateMathTest {

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Test
    fun betweenCountsCalendarMonths() {
        assertEquals(Between(0, 1, 1, 29), DateMath.between(d(2026, 1, 31), d(2026, 3, 1)))
        assertEquals(Between(0, 0, 28, 28), DateMath.between(d(2026, 1, 31), d(2026, 2, 28)))
        assertEquals(Between(1, 0, 1, 366), DateMath.between(d(2024, 2, 29), d(2025, 3, 1)))
    }

    @Test
    fun betweenIgnoresOrderAndCountsTheDaysBetween() {
        val forward = DateMath.between(d(2026, 5, 1), d(2026, 5, 2))
        val backward = DateMath.between(d(2026, 5, 2), d(2026, 5, 1))
        assertEquals(1L, forward.totalDays)
        assertEquals(forward, backward)
        assertEquals(Between(0, 0, 0, 0), DateMath.between(d(2026, 5, 1), d(2026, 5, 1)))
    }

    @Test
    fun weeksAndDays() {
        val b = DateMath.between(d(2026, 1, 1), d(2026, 12, 25))
        assertEquals(358L, b.totalDays)
        assertEquals(51L, b.weeks)
        assertEquals(1L, b.weekDays)
    }

    @Test
    fun shiftLandsOnTheLastDayOfAShortMonth() {
        assertEquals(d(2026, 2, 28), DateMath.shift(d(2026, 1, 31), 0, 1, 0, back = false))
        assertEquals(d(2028, 2, 29), DateMath.shift(d(2028, 1, 31), 0, 1, 0, back = false))
        assertEquals(d(2025, 2, 28), DateMath.shift(d(2024, 2, 29), 1, 0, 0, back = false))
    }

    @Test
    fun shiftBackTakesAway() {
        assertEquals(d(2025, 8, 28), DateMath.shift(d(2026, 10, 7), 1, 1, 10, back = true))
        assertEquals(d(2026, 10, 17), DateMath.shift(d(2026, 10, 7), 0, 0, 10, back = false))
    }
}
