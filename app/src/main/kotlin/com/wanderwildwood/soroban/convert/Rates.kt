package com.wanderwildwood.soroban.convert

import android.content.Context
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.KMathContext
import com.sadellie.unitto.core.common.KRoundingMode
import com.wanderwildwood.soroban.R
import java.io.File
import java.time.LocalDate
import org.json.JSONObject

/**
 * A day's exchange rates: how much of each currency one US dollar buys, as published for [date].
 * Any pair is worked out through the dollar, so one file covers every currency.
 */
class RateTable(val date: LocalDate, private val perDollar: Map<String, Double>) {

    fun rate(fromId: String, toId: String): KBigDecimal? {
        val from = perDollar[fromId] ?: return null
        val to = perDollar[toId] ?: return null
        if (from == 0.0) return null
        return KBigDecimal.valueOf(to).divide(KBigDecimal.valueOf(from), KMathContext(PRECISION, KRoundingMode.HALF_EVEN))
    }

    val size: Int get() = perDollar.size

    companion object {
        private const val PRECISION = 20

        /**
         * Reads the published file: `{"date": "2026-10-07", "usd": {"eur": 0.86, ...}}`. Entries
         * that are not numbers are skipped, as Unitto skips them.
         */
        fun parse(json: String): RateTable {
            val root = JSONObject(json)
            val date = LocalDate.parse(root.getString("date"))
            val usd = root.getJSONObject(BASE)
            val map = HashMap<String, Double>(usd.length())
            usd.keys().forEach { key ->
                val v = usd.opt(key)
                if (v is Number) map[key] = v.toDouble()
            }
            // The base is not always listed against itself.
            map[BASE] = 1.0
            return RateTable(date, map)
        }

        const val BASE = "usd"
    }
}

/**
 * The exchange rates, carried in the app itself (`res/raw/rates_usd.json`), so currency converts
 * with no network at all: the app has no internet permission.
 *
 * They are one day's file from Fawaz Ahmed's free currency API (CC0), the source Unitto uses,
 * taken when this version of the app was made. The converter shows their date, because they are
 * as new as the app and no newer.
 */
object Rates {

    /** The rates that came with the app, or null if the file does not read as rates. */
    fun bundled(context: Context): RateTable? = runCatching {
        context.resources.openRawResource(R.raw.rates_usd).bufferedReader().use { RateTable.parse(it.readText()) }
    }.getOrNull()

    /**
     * Earlier versions fetched the rates and kept the file in `files/rates`. Nothing reads it any
     * more, so it is not left lying on the phone.
     */
    fun forgetFetched(context: Context) {
        File(context.applicationContext.filesDir, "rates").deleteRecursively()
    }
}
