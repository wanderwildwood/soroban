package io.github.sadellie.evaluatto

import com.sadellie.unitto.core.common.Token
import kotlin.math.PI
import kotlin.math.E
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * A function of x for the graph: Unitto's [Expression], parsed once by the same tokenizer and
 * the same grammar, and evaluated in doubles so a curve's few thousand points take
 * milliseconds rather than the seconds the exact arithmetic would.
 *
 * x is written [X] in the sum. Before tokenizing it becomes a bracketed number no one would
 * type, so the tokenizer's own repairs treat it as a bracket does: "2x" gains its ×, "x(" too.
 * The brackets keep "2x" from reading as one long number. A point where
 * the function has no value (a root of a negative, 1/0, tan 90°) is NaN, never an exception.
 */
class PlotExpression(source: String, private val radianMode: Boolean) {

    private val tokens: List<String> = source.replace(X, "(" + SENTINEL + ")").tokenize()

    /** True when the sum has no x in it, a constant line. */
    val isConstant: Boolean = SENTINEL !in tokens

    private var x = 0.0
    private var cursor = 0

    /** f(x), or NaN where it has no value. Throws only for a sum that cannot be read at all. */
    fun at(value: Double): Double {
        x = value
        cursor = 0
        val result = parseExpression()
        if (cursor != tokens.size) throw ExpressionException.BadExpression()
        return if (result.isFinite()) result else Double.NaN
    }

    /** Checks the sum reads, at x = 1, so a broken one is found once rather than per point. */
    fun check() {
        at(1.0)
    }

    private fun peek() = tokens.getOrNull(cursor) ?: ""

    private fun moveIfMatched(token: String): Boolean {
        if (peek() == token) {
            cursor++
            return true
        }
        return false
    }

    private fun parseExpression(): Double {
        if (tokens.isEmpty()) throw ExpressionException.BadExpression()
        var value = parseTerm()
        while (peek() == Token.Operator.PLUS || peek() == Token.Operator.MINUS) {
            when {
                moveIfMatched(Token.Operator.PLUS) -> value += parseTerm()
                moveIfMatched(Token.Operator.MINUS) -> value -= parseTerm()
            }
        }
        return value
    }

    private fun parseTerm(): Double {
        var value = parseFactor()
        while (peek() == Token.Operator.MULTIPLY || peek() == Token.Operator.DIVIDE) {
            when {
                moveIfMatched(Token.Operator.MULTIPLY) -> value *= parseFactor()
                moveIfMatched(Token.Operator.DIVIDE) -> {
                    val divisor = parseFactor()
                    value = if (divisor == 0.0) Double.NaN else value / divisor
                }
            }
        }
        return value
    }

    private fun parseFactor(negative: Boolean = false): Double {
        var value: Double? = null

        fun parseFuncParentheses(): Double =
            if (moveIfMatched(Token.Operator.LEFT_BRACKET)) {
                val inner = parseExpression()
                if (!moveIfMatched(Token.Operator.RIGHT_BRACKET)) throw ExpressionException.BadExpression()
                inner
            } else {
                parseFactor()
            }

        if (moveIfMatched(Token.Operator.PLUS)) return parseFactor()
        if (moveIfMatched(Token.Operator.MINUS)) return -parseFactor(true)

        if (moveIfMatched(Token.Operator.LEFT_BRACKET)) {
            value = parseExpression()
            if (!moveIfMatched(Token.Operator.RIGHT_BRACKET)) throw ExpressionException.BadExpression()
        }

        val possibleNumber = peek()
        if (possibleNumber.isNotEmpty() && possibleNumber.first().toString() in Token.Digit.allWithDot) {
            value = if (possibleNumber == SENTINEL) x else possibleNumber.toDouble()
            cursor++
        }

        if (moveIfMatched(Token.Const.PI)) value = PI
        if (moveIfMatched(Token.Const.E)) value = E
        if (moveIfMatched(Token.Operator.SQRT)) value = parseFuncParentheses().let { if (it < 0) Double.NaN else sqrt(it) }
        if (moveIfMatched(Token.Func.SIN)) value = sin(toRadians(parseFuncParentheses()))
        if (moveIfMatched(Token.Func.COS)) value = cos(toRadians(parseFuncParentheses()))
        if (moveIfMatched(Token.Func.TAN)) value = tanOrNaN(toRadians(parseFuncParentheses()))
        if (moveIfMatched(Token.Func.ARSIN)) value = fromRadians(asin(parseFuncParentheses()))
        if (moveIfMatched(Token.Func.ARCOS)) value = fromRadians(acos(parseFuncParentheses()))
        if (moveIfMatched(Token.Func.ACTAN)) value = fromRadians(atan(parseFuncParentheses()))
        if (moveIfMatched(Token.Func.LN)) value = parseFuncParentheses().let { if (it <= 0) Double.NaN else ln(it) }
        if (moveIfMatched(Token.Func.LOG)) value = parseFuncParentheses().let { if (it <= 0) Double.NaN else log10(it) }
        if (moveIfMatched(Token.Func.EXP)) value = exp(parseFuncParentheses())

        if (moveIfMatched(Token.Operator.POWER)) {
            val power = parseFactor()
            val base = value ?: throw ExpressionException.BadExpression()
            value = if (base == 0.0 && power == 0.0) Double.NaN else base.pow(power)
        }
        if (moveIfMatched(Token.Operator.MODULO)) {
            val base = value ?: throw ExpressionException.BadExpression()
            val divisor = parseFactor()
            value = if (divisor == 0.0) Double.NaN else base.rem(divisor)
        }
        if (moveIfMatched(Token.Operator.FACTORIAL)) {
            val base = value ?: throw ExpressionException.BadExpression()
            value = if (negative) Double.NaN else factorial(base)
        }

        return value ?: throw ExpressionException.BadExpression()
    }

    private fun toRadians(angle: Double) = if (radianMode) angle else angle * PI / 180.0

    private fun fromRadians(angle: Double) = if (radianMode) angle else angle * 180.0 / PI

    /**
     * tan where its value is real. In degrees, tan 90° in doubles is 1.6E16 rather than
     * undefined, since π/2 cannot be written exactly; a cosine that small is taken as 0.
     */
    private fun tanOrNaN(angle: Double): Double {
        val c = cos(angle)
        return if (kotlin.math.abs(c) < 1e-12) Double.NaN else tan(angle)
    }

    /** n! for whole n from 0 to 170, as Unitto allows only whole numbers; NaN otherwise. */
    private fun factorial(n: Double): Double {
        if (n < 0 || n != kotlin.math.floor(n) || n > 170) return Double.NaN
        var result = 1.0
        var i = 2
        while (i <= n) {
            result *= i
            i++
        }
        return result
    }

    companion object {
        /** How x is written in a sum, as the calculator's tokens have it. */
        const val X = "X"

        /** A number no sum will hold, standing for x while the tokenizer reads the sum. */
        private const val SENTINEL = "0.000000000000000000000000000000007319"
    }
}
