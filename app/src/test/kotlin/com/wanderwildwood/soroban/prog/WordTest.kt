package com.wanderwildwood.soroban.prog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordTest {

    private fun eval(size: WordSize, vararg t: PToken) = Word.evaluate(t.toList(), size)
    private fun n(v: Long) = PToken.Num(v)
    private fun b(o: Op) = PToken.Bin(o)

    @Test
    fun overflowWrapsAtEveryWordSize() {
        assertEquals(-128L, eval(WordSize.BYTE, n(127), b(Op.ADD), n(1)))
        assertEquals(-32768L, eval(WordSize.WORD, n(32767), b(Op.ADD), n(1)))
        assertEquals(Int.MIN_VALUE.toLong(), eval(WordSize.DWORD, n(Int.MAX_VALUE.toLong()), b(Op.ADD), n(1)))
        assertEquals(Long.MIN_VALUE, eval(WordSize.QWORD, n(Long.MAX_VALUE), b(Op.ADD), n(1)))
        assertEquals(0L, eval(WordSize.BYTE, n(16), b(Op.MUL), n(16)))
    }

    @Test
    fun signEdges() {
        assertEquals(-128L, Word.negate(-128, WordSize.BYTE))
        assertEquals(Long.MIN_VALUE, eval(WordSize.QWORD, n(Long.MIN_VALUE), b(Op.DIV), n(-1)))
        assertEquals("80", Word.format(-128, Base.HEX, WordSize.BYTE))
        assertEquals("FF", Word.format(-1, Base.HEX, WordSize.BYTE))
        assertEquals("FFFFFFFFFFFFFFFF", Word.format(-1, Base.HEX, WordSize.QWORD))
        assertEquals("-1", Word.format(255, Base.DEC, WordSize.BYTE))
        assertEquals("177777", Word.format(-1, Base.OCT, WordSize.WORD))
    }

    @Test
    fun bitwiseAtEachSize() {
        for (size in WordSize.entries) {
            assertEquals(size.wrap(0b1000), eval(size, n(0b1100), b(Op.AND), n(0b1010)))
            assertEquals(size.wrap(0b1110), eval(size, n(0b1100), b(Op.OR), n(0b1010)))
            assertEquals(size.wrap(0b0110), eval(size, n(0b1100), b(Op.XOR), n(0b1010)))
            assertEquals(size.wrap(0b1000.inv().toLong()), eval(size, n(0b1100), b(Op.NAND), n(0b1010)))
            assertEquals(size.wrap(0b1110.inv().toLong()), eval(size, n(0b1100), b(Op.NOR), n(0b1010)))
            assertEquals(-1L, eval(size, PToken.Not, n(0)))
            assertEquals(size.mask, size.pattern(-1).toLong())
        }
        assertEquals("F0", Word.format(eval(WordSize.BYTE, PToken.Not, n(0x0F))!!, Base.HEX, WordSize.BYTE))
    }

    @Test
    fun shiftsAreArithmeticAndRotatesTurn() {
        assertEquals(-64L, eval(WordSize.BYTE, n(-128), b(Op.SHR), n(1)))
        assertEquals(-1L, eval(WordSize.BYTE, n(-128), b(Op.SHR), n(20)))
        assertEquals(0L, eval(WordSize.BYTE, n(1), b(Op.SHL), n(8)))
        assertEquals(-128L, eval(WordSize.BYTE, n(1), b(Op.SHL), n(7)))
        assertEquals(1L, eval(WordSize.BYTE, n(-128), b(Op.ROL), n(1)))
        assertEquals(-128L, eval(WordSize.BYTE, n(1), b(Op.ROR), n(1)))
        assertEquals("56781234", Word.format(eval(WordSize.DWORD, n(0x34567812), b(Op.ROL), n(8))!!, Base.HEX, WordSize.DWORD))
        assertEquals(1L, eval(WordSize.QWORD, n(Long.MIN_VALUE), b(Op.ROL), n(1)))
        assertEquals(Long.MIN_VALUE, eval(WordSize.QWORD, n(1), b(Op.ROR), n(65)))
    }

    @Test
    fun precedenceAsInC() {
        // 1 + 2 << 1 is (1 + 2) << 1
        assertEquals(6L, eval(WordSize.QWORD, n(1), b(Op.ADD), n(2), b(Op.SHL), n(1)))
        // 6 OR 1 AND 3 is 6 OR (1 AND 3)
        assertEquals(7L, eval(WordSize.QWORD, n(6), b(Op.OR), n(1), b(Op.AND), n(3)))
        // 2 × (3 + 4)
        assertEquals(14L, eval(WordSize.QWORD, n(2), b(Op.MUL), PToken.Open, n(3), b(Op.ADD), n(4), PToken.Close))
        // 7 − 2 − 1 is left to right
        assertEquals(4L, eval(WordSize.QWORD, n(7), b(Op.SUB), n(2), b(Op.SUB), n(1)))
        assertEquals(-3L, eval(WordSize.QWORD, PToken.Neg, n(3)))
    }

    @Test
    fun aSumStillBeingTypedHasItsValueSoFar() {
        assertEquals(5L, eval(WordSize.QWORD, n(5), b(Op.ADD)))
        assertEquals(7L, eval(WordSize.QWORD, PToken.Open, n(3), b(Op.ADD), n(4)))
        assertNull(eval(WordSize.QWORD))
    }

    @Test(expected = ProgrammerException::class)
    fun divisionByZeroSaysSo() {
        eval(WordSize.QWORD, n(1), b(Op.DIV), n(0))
    }

    @Test
    fun digitsStopWhereTheWordIsFull() {
        var v = 0L
        "FF".forEach { v = Word.appendDigit(v, Character.digit(it, 16), Base.HEX, WordSize.BYTE)!! }
        assertEquals(-1L, v)
        assertNull(Word.appendDigit(v, 1, Base.HEX, WordSize.BYTE))
        assertNull(Word.appendDigit(127, 0, Base.DEC, WordSize.BYTE))
        assertEquals(127L, Word.appendDigit(12, 7, Base.DEC, WordSize.BYTE))
        assertNull(Word.appendDigit(Long.MAX_VALUE / 10 + 1, 0, Base.DEC, WordSize.QWORD))
        var q = 0L
        repeat(16) { q = Word.appendDigit(q, 15, Base.HEX, WordSize.QWORD)!! }
        assertEquals(-1L, q)
        assertNull(Word.appendDigit(q, 0, Base.HEX, WordSize.QWORD))
        var bin = 0L
        repeat(8) { bin = Word.appendDigit(bin, 1, Base.BIN, WordSize.BYTE)!! }
        assertNull(Word.appendDigit(bin, 1, Base.BIN, WordSize.BYTE))
    }

    @Test
    fun togglingBits() {
        assertEquals(-128L, Word.toggle(0, 7, WordSize.BYTE))
        assertEquals(0L, Word.toggle(0, 8, WordSize.BYTE))
        assertEquals(Long.MIN_VALUE, Word.toggle(0, 63, WordSize.QWORD))
    }

    @Test
    fun nibbles() {
        assertEquals("1010 0101", Word.nibbles(0xA5, WordSize.QWORD))
        assertEquals("0001", Word.nibbles(1, WordSize.BYTE))
        assertEquals("1111 1111", Word.nibbles(-1, WordSize.BYTE))
    }

    @Test
    fun sumsSurviveBeingSavedAndRestored() {
        val t = listOf(PToken.Not, PToken.Open, n(-5), b(Op.ROL), n(3), PToken.Close, PToken.Neg)
        assertEquals(t, ProgrammerViewModel.decode(ProgrammerViewModel.encode(t)))
    }
}
