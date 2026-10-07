package com.wanderwildwood.soroban.dates

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/** The time between two days, both ways of counting it. */
data class Between(
    val years: Int,
    val months: Int,
    val days: Int,
    /** Every day from the first to the second, the first not counted: 1 May to 2 May is 1. */
    val totalDays: Long,
) {
    val weeks: Long get() = totalDays / 7
    val weekDays: Long get() = totalDays % 7
}

object DateMath {

    /**
     * From [a] to [b] in years, months and days, and in days. Order does not matter: the
     * earlier of the two is always the start, and the answer is never negative.
     *
     * Months are counted as the calendar has them, so 31 January to 28 February is 28 days
     * but "0 months, 28 days", and 31 January to 1 March is "1 month, 1 day".
     */
    fun between(a: LocalDate, b: LocalDate): Between {
        val (start, end) = if (a <= b) a to b else b to a
        val period = Period.between(start, end)
        return Between(period.years, period.months, period.days, ChronoUnit.DAYS.between(start, end))
    }

    /**
     * [date] moved by years, then months, then days, forward or back. A day past the end of a
     * shorter month lands on its last day: 31 January plus one month is 28 (or 29) February.
     */
    fun shift(date: LocalDate, years: Long, months: Long, days: Long, back: Boolean): LocalDate {
        val sign = if (back) -1 else 1
        return date
            .plusYears(sign * years)
            .plusMonths(sign * months)
            .plusDays(sign * days)
    }
}
