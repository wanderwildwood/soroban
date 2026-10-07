package com.wanderwildwood.soroban

import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.toFormattedString
import com.sadellie.unitto.core.ui.textfield.formatExpression

/**
 * Turning the calculator's own tokens into what is written on the screen, and text from
 * elsewhere back into tokens. The tokens use "." for decimals and no separators, whatever the
 * phone's language; only the screen and the clipboard see the chosen style.
 */
object Numbers {

    /** A sum or a number as it is shown: separators as chosen, and "mod" for modulo. */
    fun show(tokens: String, symbols: FormatterSymbols): String =
        tokens.formatExpression(symbols).replace(Token.Operator.MODULO, " mod ")

    /** A converted value, rounded to the chosen places, as it is shown. */
    fun show(value: KBigDecimal, settings: Settings): String =
        show(value.toFormattedString(settings.precision, settings.outputFormat).replace("-", Token.Operator.MINUS), settings.symbols)

    /** A number or sum as text for another app: exactly what the screen shows. */
    fun copyText(tokens: String, symbols: FormatterSymbols): String = show(tokens, symbols).trim()

    /**
     * Text pasted or shared in, as tokens: "1,234.5" (or "1.234,5", whichever style is set),
     * "-3", "2*3" and "10 / 4" all read; anything that is not part of a sum is dropped.
     * Returns null when nothing usable is left.
     */
    fun parse(text: String, symbols: FormatterSymbols): String? {
        var s = text.trim()
        if (s.isEmpty()) return null
        // Grouping first, so "1.234,5" loses its point before the comma becomes one.
        if (symbols.grouping.isNotBlank()) s = s.replace(symbols.grouping, "")
        s = s.replace(" ", "").replace(" ", "").replace(" ", "")
        if (symbols.fractional != Token.PERIOD) s = s.replace(symbols.fractional, Token.PERIOD)
        s = s.replace("mod", Token.Operator.MODULO)
        Token.sexyToUgly.forEach { (token, ugly) -> ugly.forEach { s = s.replace(it, token) } }
        val kept = StringBuilder()
        var i = 0
        while (i < s.length) {
            val match = Token.expressionTokens.filter { s.startsWith(it, i) }.maxByOrNull { it.length }
            if (match != null) {
                kept.append(match)
                i += match.length
            } else {
                i++
            }
        }
        return kept.toString().ifEmpty { null }
    }
}
