package com.wanderwildwood.soroban.ui

import com.sadellie.unitto.core.data.converter.collections.currencyCollection
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The coins' tickers are their real ones, in full, and the unit list finds them by ticker or name. */
class SymbolSearchTest {
  private val xml = File("src/main/res/values/units.xml").readText()
  private fun string(name: String) = Regex("""<string name="$name"[^>]*>([^<]*)</string>""").find(xml)?.groupValues?.get(1)

  private val names: Map<String, Pair<String, String>> by lazy {
    currencyCollection.associate { u ->
      u.id to ((string("unit_currency_${u.id}") ?: u.id) to (string("unit_currency_${u.id}_short") ?: u.id))
    }
  }

  @Test
  fun everyShortNameIsItsIdInCapitals() {
    val wrong = Regex("""name="unit_currency_(\w+?)_short"[^>]*>([^<]*)<""").findAll(xml)
      .filter { it.groupValues[1].uppercase() != it.groupValues[2].uppercase() }
      .map { "${it.groupValues[1]} -> ${it.groupValues[2]}" }.toList()
    assertEquals(emptyList(), wrong)
  }

  @Test
  fun theFifteenCutTickersAreWhole() {
    val want = mapOf(
      "1inch" to "1INCH", "algo" to "ALGO", "atom" to "ATOM", "avax" to "AVAX", "busd" to "BUSD",
      "doge" to "DOGE", "egld" to "EGLD", "link" to "LINK", "luna" to "LUNA", "matic" to "MATIC",
      "shib" to "SHIB", "theta" to "THETA", "usdc" to "USDC", "usdt" to "USDT", "wbtc" to "WBTC",
    )
    want.forEach { (id, ticker) -> assertEquals(ticker, names.getValue(id).second, id) }
  }

  @Test
  fun searchByTickerFindsTheCoinFirst() {
    listOf("doge", "DOGE", "matic", "wbtc", "link", "usdt", "1inch").forEach { q ->
      assertEquals(q.lowercase(), filter(currencyCollection, names, q).first().id, q)
    }
  }

  @Test
  fun searchByNameFindsTheCoin() {
    assertTrue(filter(currencyCollection, names, "bitcoin").any { it.id == "btc" })
    assertTrue(filter(currencyCollection, names, "wrapped bitcoin").any { it.id == "wbtc" })
  }
}
