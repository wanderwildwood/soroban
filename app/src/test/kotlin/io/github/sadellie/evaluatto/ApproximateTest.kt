package io.github.sadellie.evaluatto

import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.OutputFormat
import com.sadellie.unitto.core.common.integerDigits
import com.sadellie.unitto.core.common.toApproximateString
import com.wanderwildwood.soroban.calc.Answer
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApproximateTest {
  private fun run(input: String) = Expression(input, radianMode = true, KRoundingMode.HALF_EVEN).let { it.calculate() to it.approximate }

  @Test
  fun powerOfTwo_isExactAndNotMarked() {
    val (value, approximate) = run("2^1000")
    assertEquals(BigInteger.TWO.pow(1000).toString(), value.toPlainString())
    assertFalse(approximate)
    assertEquals(302, value.integerDigits())
  }

  @Test
  fun wholePowers_stayExact() {
    assertEquals("1024", run("2^10").first.toPlainString())
    assertEquals(BigInteger.valueOf(7).pow(300).toString(), run("7^300").first.toPlainString())
    assertFalse(run("7^300").second)
    assertEquals("1.5", run("1.5^1").first.stripTrailingZeros().toPlainString())
    assertEquals("2.25", run("1.5^2").first.stripTrailingZeros().toPlainString())
  }

  @Test
  fun powerWithAFractionalExponent_isNotMarkedWhenSmall() {
    assertFalse(run("2^0.5").second)
    assertFalse(run("10^20").second)
  }

  @Test
  fun resultsRoundedPastTheirKnownDigits_areMarked() {
    // Past the whole-number exponents worked out digit for digit.
    assertTrue(run("2^10000").second)
    // e^300 has 131 whole digits; only the first 100 are known.
    assertTrue(run("exp(300)").second)
    assertFalse(run("exp(10)").second)
  }

  @Test
  fun approximateString_isFewFiguresAndAPowerOfTen() {
    assertEquals("1.07150860719E+301", KBigDecimal(BigInteger.TWO.pow(1000).toString()).toApproximateString())
    assertEquals("-1.5E+120", KBigDecimal("-1.5").multiply(KBigDecimal(BigInteger.TEN.pow(120).toString())).toApproximateString())
    assertEquals("1E+101", KBigDecimal(BigInteger.TEN.pow(101).toString()).toApproximateString())
    assertEquals("1.5E-7", KBigDecimal("0.00000015").toApproximateString())
  }

  @Test
  fun answer_writesExactResultsInFullAndApproximateOnesShort() {
    val exact = Answer(run("2^100").first, approximate = false)
    assertEquals(BigInteger.TWO.pow(100).toString(), exact.text(8, OutputFormat.PLAIN))
    val huge = Answer(run("2^10000").first, approximate = true)
    assertTrue(huge.text(8, OutputFormat.PLAIN).matches(Regex("""\d\.\d{1,11}E\+3010""")))
  }
}
