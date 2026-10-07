package com.wanderwildwood.soroban

import android.content.Context
import android.content.SharedPreferences
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.OutputFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How a number is written: what separates the thousands, and what marks the decimals. */
enum class NumberStyle(val grouping: String, val fractional: String) {
    COMMA_POINT(",", "."),
    SPACE_POINT(" ", "."),
    POINT_COMMA(".", ","),
    SPACE_COMMA(" ", ",");

    val symbols: FormatterSymbols get() = FormatterSymbols(grouping, fractional, indian = false)

    /** 1234567.89 written this way, for the settings row. */
    val example: String get() = "1${grouping}234${grouping}567${fractional}89"

    companion object {
        /** The phone's own way, as near as these four come to it. */
        fun forLocale(locale: Locale): NumberStyle {
            val s = DecimalFormatSymbols.getInstance(locale)
            val comma = s.decimalSeparator == ','
            val spaced = s.groupingSeparator.isWhitespace()
            return when {
                comma && spaced -> SPACE_COMMA
                comma -> POINT_COMMA
                spaced -> SPACE_POINT
                else -> COMMA_POINT
            }
        }
    }
}

data class Settings(
    /** Decimal places a result is rounded to. */
    val precision: Int = 8,
    val style: NumberStyle = NumberStyle.COMMA_POINT,
    /** [OutputFormat.PLAIN] writes every digit out; [OutputFormat.ALLOW_ENGINEERING] uses E. */
    val outputFormat: Int = OutputFormat.PLAIN,
    /** After =, a result that is a simple fraction is also shown as one. */
    val fractions: Boolean = true,
    val radians: Boolean = false,
) {
    val symbols: FormatterSymbols get() = style.symbols
}

/**
 * The settings, and the few things remembered between visits (which page was open, the units
 * last converted between), in SharedPreferences on this phone.
 */
class Prefs private constructor(private val prefs: SharedPreferences) {

    private val state = MutableStateFlow(read())
    val settings: StateFlow<Settings> = state.asStateFlow()

    private fun read() = Settings(
        precision = prefs.getInt(PRECISION, Settings().precision),
        style = prefs.getString(STYLE, null)?.let { name -> NumberStyle.entries.firstOrNull { it.name == name } }
            ?: NumberStyle.forLocale(Locale.getDefault()),
        outputFormat = prefs.getInt(OUTPUT, OutputFormat.PLAIN),
        fractions = prefs.getBoolean(FRACTIONS, true),
        radians = prefs.getBoolean(RADIANS, false),
    )

    fun update(change: (Settings) -> Settings) {
        val next = change(state.value)
        prefs.edit()
            .putInt(PRECISION, next.precision)
            .putString(STYLE, next.style.name)
            .putInt(OUTPUT, next.outputFormat)
            .putBoolean(FRACTIONS, next.fractions)
            .putBoolean(RADIANS, next.radians)
            .apply()
        state.value = next
    }

    /**
     * The tab last open: 0 calculate, 1 graph, 2 convert, 3 dates. 0.1.0 kept it under
     * another key without the graph, so its convert (1) and dates (2) move up one.
     */
    var page: Int
        get() = if (prefs.contains(TAB)) prefs.getInt(TAB, 0) else when (val old = prefs.getInt(PAGE, 0)) {
            0 -> 0
            else -> old + 1
        }
        set(value) = prefs.edit().putInt(TAB, value).apply()

    var group: String?
        get() = prefs.getString(GROUP, null)
        set(value) = prefs.edit().putString(GROUP, value).apply()

    /** Which keys the calculator shows: 0 the numbers, 1 the functions, 2 programmer. */
    var calcMode: Int
        get() = prefs.getInt(CALC_MODE, 0)
        set(value) = prefs.edit().putInt(CALC_MODE, value).apply()

    /** The graph's functions, in calculator tokens with x as "X". Up to three. */
    var functions: List<String>
        get() = prefs.getString(FUNCTIONS, null)?.split('\n')?.filter { it.isNotEmpty() }.orEmpty()
        set(value) = prefs.edit().putString(FUNCTIONS, value.joinToString("\n")).apply()

    /** The graph's window as "xMin,xMax,yMin,yMax", or null before the graph is first drawn. */
    var viewport: List<Double>?
        get() = prefs.getString(VIEWPORT, null)?.split(',')?.mapNotNull { it.toDoubleOrNull() }?.takeIf { it.size == 4 }
        set(value) = prefs.edit().putString(VIEWPORT, value?.joinToString(",")).apply()

    var tableStart: String
        get() = prefs.getString(TABLE_START, "0") ?: "0"
        set(value) = prefs.edit().putString(TABLE_START, value).apply()

    var tableStep: String
        get() = prefs.getString(TABLE_STEP, "1") ?: "1"
        set(value) = prefs.edit().putString(TABLE_STEP, value).apply()

    var progBase: String?
        get() = prefs.getString(PROG_BASE, null)
        set(value) = prefs.edit().putString(PROG_BASE, value).apply()

    var progSize: String?
        get() = prefs.getString(PROG_SIZE, null)
        set(value) = prefs.edit().putString(PROG_SIZE, value).apply()

    /** The pair of units last used in a group, as "fromId>toId". */
    fun pair(group: String): Pair<String, String>? =
        prefs.getString("$PAIR$group", null)?.split('>')?.takeIf { it.size == 2 }?.let { it[0] to it[1] }

    fun setPair(group: String, from: String, to: String) {
        val recent = (listOf(from, to) + recent(group).filter { it != from && it != to }).take(RECENT)
        prefs.edit()
            .putString("$PAIR$group", "$from>$to")
            .putString("$RECENT_KEY$group", recent.joinToString(">"))
            .apply()
    }

    /** The units last chosen in a group, newest first, so a long list opens on them. */
    fun recent(group: String): List<String> =
        prefs.getString("$RECENT_KEY$group", null)?.split('>')?.filter { it.isNotEmpty() }.orEmpty()

    companion object {
        private const val PRECISION = "precision"
        private const val STYLE = "number_style"
        private const val OUTPUT = "output_format"
        private const val FRACTIONS = "fractions"
        private const val RADIANS = "radians"
        private const val PAGE = "page"
        private const val TAB = "tab"
        private const val GROUP = "group"
        private const val PAIR = "pair_"
        private const val RECENT_KEY = "recent_"
        private const val CALC_MODE = "calc_mode"
        private const val FUNCTIONS = "graph_functions"
        private const val VIEWPORT = "graph_viewport"
        private const val TABLE_START = "table_start"
        private const val TABLE_STEP = "table_step"
        private const val PROG_BASE = "prog_base"
        private const val PROG_SIZE = "prog_size"
        private const val RECENT = 6

        @Volatile private var instance: Prefs? = null

        fun get(context: Context): Prefs = instance ?: synchronized(this) {
            instance ?: Prefs(context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE))
                .also { instance = it }
        }
    }
}
