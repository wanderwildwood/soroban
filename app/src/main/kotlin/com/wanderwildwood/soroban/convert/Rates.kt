package com.wanderwildwood.soroban.convert

import android.content.Context
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.KMathContext
import com.sadellie.unitto.core.common.KRoundingMode
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import org.json.JSONObject

/**
 * A day's exchange rates: how much of each currency one US dollar buys, as published for [date].
 * Any pair is worked out through the dollar, so one fetch covers every currency.
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
 * The exchange rates, kept on the phone so currency converts with no signal.
 *
 * They come from Fawaz Ahmed's free currency API, the source Unitto uses: one file a day, read
 * from jsDelivr's CDN, or from the API's own mirror on Cloudflare Pages when that fails. Nothing
 * is sent but the request for the file. It is fetched only while currency is open, and at most
 * once a day: [isStale] is false for the rest of the day a fetch succeeded.
 */
class Rates(private val dir: File, private val addresses: List<String> = ADDRESSES) {

    private val file get() = File(dir, "rates-usd.json")
    private val fetchedFile get() = File(dir, "rates-fetched")

    /** The rates last fetched, or null if none have ever been. */
    fun cached(): RateTable? = runCatching { RateTable.parse(file.readText()) }.getOrNull()

    /** True unless a fetch has already succeeded today. */
    fun isStale(today: LocalDate = LocalDate.now()): Boolean =
        runCatching { LocalDate.parse(fetchedFile.readText().trim()) != today }.getOrDefault(true)

    /**
     * Fetches today's file and keeps it. Throws if neither address answers with something that
     * reads as rates; the file already kept is then left as it was.
     */
    fun fetch(today: LocalDate = LocalDate.now()): RateTable {
        var failure: Exception? = null
        for (address in addresses) {
            try {
                val text = get(address)
                val table = RateTable.parse(text)
                if (table.size < MIN_CURRENCIES) error("only ${table.size} rates in the file")
                dir.mkdirs()
                val tmp = File(dir, "rates-usd.json.tmp")
                tmp.writeText(text)
                if (!tmp.renameTo(file)) error("could not keep the rates")
                fetchedFile.writeText(today.toString())
                return table
            } catch (e: Exception) {
                failure = e
            }
        }
        throw failure ?: IllegalStateException("no address to fetch from")
    }

    private fun get(address: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) error("HTTP $code")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val TIMEOUT_MS = 15_000
        private const val MIN_CURRENCIES = 30

        val ADDRESSES = listOf(
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json",
            "https://latest.currency-api.pages.dev/v1/currencies/usd.json",
        )

        fun get(context: Context) = Rates(File(context.applicationContext.filesDir, "rates"))
    }
}
