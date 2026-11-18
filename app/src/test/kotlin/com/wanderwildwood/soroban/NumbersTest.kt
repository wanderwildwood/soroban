package com.wanderwildwood.soroban

import com.sadellie.unitto.core.common.Token
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumbersTest {

    private val us = NumberStyle.COMMA_POINT.symbols
    private val de = NumberStyle.POINT_COMMA.symbols
    private val fr = NumberStyle.SPACE_COMMA.symbols

    @Test
    fun showsInTheChosenStyle() {
        assertEquals("1,234,567.89", Numbers.show("1234567.89", us))
        assertEquals("1.234.567,89", Numbers.show("1234567.89", de))
        assertEquals("1 234 567,89", Numbers.show("1234567.89", fr))
        assertEquals("12×3,000", Numbers.show("12×3000", us))
    }

    @Test
    fun theWholeSumIsTheSumAndItsAnswerInTheChosenStyle() {
        assertEquals("12×3,000 = 36,000", Numbers.wholeSum("12×3000", "36000", us))
        assertEquals("1.234,5×2 = 2.469", Numbers.wholeSum("1234.5×2", "2469", de))
        assertNull(Numbers.wholeSum("", "5", us))
        assertNull(Numbers.wholeSum("2+3", "", us))
    }

    @Test
    fun moduloIsWrittenAsAWord() {
        assertEquals("10 mod 3", Numbers.show("10" + Token.Operator.MODULO + "3", us))
    }

    @Test
    fun pastedTextReadsBackInEachStyle() {
        assertEquals("1234567.89", Numbers.parse("1,234,567.89", us))
        assertEquals("1234567.89", Numbers.parse("1.234.567,89", de))
        assertEquals("1234567.89", Numbers.parse("1 234 567,89", fr))
    }

    @Test
    fun pastedOperatorsBecomeTheCalculatorsOwn() {
        assertEquals("2×3", Numbers.parse("2*3", us))
        assertEquals("10÷4", Numbers.parse("10 / 4", us))
        assertEquals(Token.Operator.MINUS + "3", Numbers.parse("-3", us))
        assertEquals("10#3", Numbers.parse("10 mod 3", us))
    }

    @Test
    fun whatIsShownIsWhatReadsBack() {
        listOf("1234567.89", "12×3000", "−42.5", "sin(30)+2^3").forEach { tokens ->
            listOf(us, de, fr).forEach { style ->
                assertEquals(tokens, Numbers.parse(Numbers.show(tokens, style), style))
            }
        }
    }

    @Test
    fun wordsInPastedTextAreDroppedWhole() {
        assertEquals("12", Numbers.parse("12 apples", us))
        assertEquals("5", Numbers.parse("5 miles", us))
        assertEquals("3", Numbers.parse("3 feet", us))
        assertEquals("5", Numbers.parse("Model 5", us))
        assertEquals("2×e", Numbers.parse("2*e", us))
        assertEquals("2π", Numbers.parse("2π", us))
        assertEquals("ln(2)+log(100)", Numbers.parse("ln(2) + log(100)", us))
        assertEquals("sin⁻¹(1)", Numbers.parse("arsin(1)", us))
        assertEquals("1.5E10", Numbers.parse("1.5E10", us))
    }

    @Test
    fun aDecimalWrittenTheOtherWayIsStillADecimal() {
        assertEquals("3.5", Numbers.parse("3.5", de))
        assertEquals("0.25", Numbers.parse("0.25", de))
        assertEquals("3.5", Numbers.parse("3,5", us))
        assertEquals("3.5", Numbers.parse("3.5", fr))
        // Groups of three, or more than one mark, are still thousands.
        assertEquals("1234", Numbers.parse("1.234", de))
        assertEquals("1234", Numbers.parse("1,234", us))
        assertEquals("1234567", Numbers.parse("1,234,567", NumberStyle.SPACE_POINT.symbols))
        assertEquals("1234.5", Numbers.parse("1.234,5", fr))
    }

    @Test
    fun textWithNoNumberIsNothing() {
        assertNull(Numbers.parse("Ada Whitlock", us))
        assertNull(Numbers.parse("   ", us))
    }

    @Test
    fun theStyleFollowsThePhone() {
        assertEquals(NumberStyle.COMMA_POINT, NumberStyle.forLocale(java.util.Locale.US))
        assertEquals(NumberStyle.POINT_COMMA, NumberStyle.forLocale(java.util.Locale.GERMANY))
        assertEquals(NumberStyle.SPACE_COMMA, NumberStyle.forLocale(java.util.Locale.FRANCE))
    }
}
