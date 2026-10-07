package com.wanderwildwood.soroban

import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.OutputFormat
import com.sadellie.unitto.core.common.toFormattedString
import com.sadellie.unitto.core.data.converter.Converter
import com.sadellie.unitto.core.data.converter.ConverterResult
import com.sadellie.unitto.core.data.converter.UnitID
import com.sadellie.unitto.core.data.converter.Units
import com.wanderwildwood.soroban.convert.RateTable
import com.wanderwildwood.soroban.convert.Rates
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RatesTest {

    // The shape the API publishes, with a value that is not a number, as Unitto's test has.
    private val sample = """
        {"date": "2026-10-06", "usd": {"eur": 0.86, "gbp": 0.75, "jpy": 150, "usd": 1, "bad": "x"}}
    """.trimIndent()

    @Test
    fun parsesDateAndSkipsWhatIsNotANumber() {
        val t = RateTable.parse(sample)
        assertEquals(LocalDate.of(2026, 10, 6), t.date)
        assertEquals(4, t.size)
        assertNull(t.rate("bad", "usd"))
    }

    @Test
    fun crossRatesGoThroughTheDollar() {
        val t = RateTable.parse(sample)
        assertEquals("0.86", t.rate("usd", "eur")!!.toFormattedString(5, OutputFormat.PLAIN))
        // 1 EUR = 0.75 / 0.86 GBP
        assertEquals("0.87209", t.rate("eur", "gbp")!!.toFormattedString(5, OutputFormat.PLAIN))
        assertEquals("1", t.rate("jpy", "jpy")!!.toFormattedString(5, OutputFormat.PLAIN))
        assertNull(t.rate("usd", "xyz"))
    }

    @Test
    fun currencyConvertsWithKeptRatesAndSaysSoWithout() {
        val t = RateTable.parse(sample)
        val withRates = Converter { a, b -> t.rate(a, b) }
        val r = withRates.convert(Units.byId(UnitID.currency_usd)!!, Units.byId(UnitID.currency_eur)!!, "100")
        assertTrue(r is ConverterResult.Default)
        assertEquals("86", (r as ConverterResult.Default).value.toFormattedString(5, OutputFormat.PLAIN))

        val without = Converter { _, _ -> null }
        assertEquals(
            ConverterResult.Error.CurrencyError,
            without.convert(Units.byId(UnitID.currency_usd)!!, Units.byId(UnitID.currency_eur)!!, "100"),
        )
    }

    @Test
    fun aFailedFetchKeepsTheRatesAlreadyThere() {
        val dir = Files.createTempDirectory("rates").toFile()
        File(dir, "rates-usd.json").writeText(sample)
        // Port 1 on this machine refuses at once: both addresses fail.
        val rates = Rates(dir, listOf("http://127.0.0.1:1/a.json", "http://127.0.0.1:1/b.json"))
        try {
            rates.fetch()
            fail("a fetch from nowhere succeeded")
        } catch (e: Exception) {
            // expected
        }
        val kept = rates.cached()
        assertNotNull(kept)
        assertEquals(LocalDate.of(2026, 10, 6), kept!!.date)
        assertTrue("never fetched, so stale", rates.isStale(LocalDate.of(2026, 10, 7)))
        dir.deleteRecursively()
    }

    @Test
    fun noRatesAtAllIsNull() {
        val dir = Files.createTempDirectory("rates").toFile()
        assertNull(Rates(dir).cached())
        dir.deleteRecursively()
    }

    @Test
    fun theDollarRateIsOneEvenWhenTheFileLeavesItOut() {
        val t = RateTable.parse("""{"date": "2026-10-06", "usd": {"eur": 0.5}}""")
        assertEquals(KBigDecimal("2").toFormattedString(3, OutputFormat.PLAIN), t.rate("eur", "usd")!!.toFormattedString(3, OutputFormat.PLAIN))
    }
}
