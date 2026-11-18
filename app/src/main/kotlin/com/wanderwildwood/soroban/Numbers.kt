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

    /** The sum and its answer on one line ("12 × 3 = 36"), or null while there is no exact answer to give. */
    fun wholeSum(tokens: String, result: String, symbols: FormatterSymbols): String? =
        if (tokens.isBlank() || result.isBlank()) null else copyText(tokens, symbols) + " = " + copyText(result, symbols)

    /**
     * Text pasted or shared in, as tokens: "1,234.5" (or "1.234,5", whichever style is set),
     * "-3", "2*3" and "10 / 4" all read; anything that is not part of a sum is dropped, words
     * included ("12 apples" is 12). A decimal written the other way ("3.5" with the comma style
     * set, "3,5" with the point) is still a decimal. Returns null when nothing usable is left.
     */
    fun parse(text: String, symbols: FormatterSymbols): String? {
        var s = text.trim()
        if (s.isEmpty()) return null
        s = otherMark(s, symbols.fractional)
        // Grouping first, so "1.234,5" loses its point before the comma becomes one.
        if (symbols.grouping.isNotBlank()) s = s.replace(symbols.grouping, "")
        s = s.replace(" ", "").replace(" ", "").replace(" ", "")
        if (symbols.fractional != Token.PERIOD) s = s.replace(symbols.fractional, Token.PERIOD)
        Token.sexyToUgly.forEach { (token, ugly) -> ugly.forEach { s = s.replace(it, token) } }
        s = knownWords(s)
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

    /**
     * The decimal mark of the other style ("." when a comma is set, "," when a point is), read
     * the way it was meant: once in a number and not followed by exactly three digits, as in
     * "3.5" or "0,25", it is a decimal point and becomes [fractional]; otherwise ("1.234.567",
     * "1,234", "1.234,5") it groups thousands and goes.
     */
    private fun otherMark(text: String, fractional: String): String {
        val mark = if (fractional == Token.COMMA) Token.PERIOD else Token.COMMA
        return Regex("[0-9]+(?:" + Regex.escape(mark) + "[0-9]+)+").replace(text) { m ->
            val parts = m.value.split(mark)
            val decimal = parts.size == 2 && parts[1].length != 3 &&
                !text.startsWith(fractional, m.range.last + 1)
            parts.joinToString(if (decimal) fractional else "")
        }
    }

    /**
     * Keeps a run of letters only when the whole run means something to the calculator: "mod",
     * a function name followed by its bracket, e or π, or the E of 1.5E10. Any other word goes
     * whole, so the e of "apples" and the mod of "Model" are not read as part of a sum.
     */
    private fun knownWords(s: String): String {
        val inverse = Token.Func.ARSIN.removePrefix(Token.Func.SIN)
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            if (!s[i].isLetter()) {
                out.append(s[i])
                i++
                continue
            }
            var end = i
            while (end < s.length && s[end].isLetter()) end++
            val word = s.substring(i, end)
            val bracket = s.startsWith(Token.Operator.LEFT_BRACKET, end) ||
                s.startsWith(inverse + Token.Operator.LEFT_BRACKET, end)
            when {
                word == "mod" -> out.append(Token.Operator.MODULO)
                word in Token.Func.all && bracket -> out.append(word)
                word == Token.DisplayOnly.ENGINEERING_E -> out.append(word)
                word.all { it.toString() in Token.Const.all } -> out.append(word)
            }
            i = end
        }
        return out.toString()
    }
}
