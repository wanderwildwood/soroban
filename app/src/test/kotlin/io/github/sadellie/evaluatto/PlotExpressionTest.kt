package io.github.sadellie.evaluatto

import com.sadellie.unitto.core.common.KRoundingMode
import kotlin.math.PI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlotExpressionTest {

    private fun f(src: String, radians: Boolean = true) = PlotExpression(src, radians)

    @Test
    fun agreesWithTheExactEvaluatorAtSamplePoints() {
        val sums = listOf("X^2−4", "2X+3", "sin(X)×X", "√(X)+ln(X)", "(X+1)(X−1)", "exp(X)÷10", "log(X)", "πX", "X#3", "3!+X", "cos(X)^2+sin(X)^2", "X+5%")
        val xs = listOf(0.5, 1.0, 2.0, 3.7, 10.0)
        for (s in sums) for (x in xs) {
            val exact = Expression(s.replace("X", "($x)"), radianMode = true, roundingMode = KRoundingMode.HALF_EVEN).calculate().toString().toDouble()
            assertEquals("$s at $x", exact, f(s).at(x), 1e-9 * maxOf(1.0, kotlin.math.abs(exact)))
        }
    }

    @Test
    fun degreesAreHonoured() {
        assertEquals(1.0, f("sin(X)", radians = false).at(90.0), 1e-12)
        assertEquals(1.0, f("sin(X)", radians = true).at(PI / 2), 1e-12)
        assertEquals(45.0, f("tan⁻¹(X)", radians = false).at(1.0), 1e-9)
    }

    @Test
    fun noValueIsNaNNotAnError() {
        assertTrue(f("1÷X").at(0.0).isNaN())
        assertTrue(f("√(X)").at(-1.0).isNaN())
        assertTrue(f("ln(X)").at(0.0).isNaN())
        assertTrue(f("tan(X)", radians = false).at(90.0).isNaN())
        assertTrue(f("X!").at(2.5).isNaN())
    }

    @Test
    fun implicitMultiplicationAroundX() {
        assertEquals(6.0, f("2X").at(3.0), 0.0)
        assertEquals(12.0, f("X(X+1)").at(3.0), 0.0)
        assertEquals(3 * PI, f("Xπ").at(3.0), 1e-12)
        assertEquals(9.0, f("XX").at(3.0), 0.0)
    }

    @Test
    fun aConstantHasNoX() {
        assertTrue(f("2+3").isConstant)
        assertEquals(5.0, f("2+3").at(100.0), 0.0)
    }

    @Test(expected = ExpressionException::class)
    fun aBrokenSumIsFoundOnce() {
        f("X+").check()
    }
}
