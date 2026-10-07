package com.wanderwildwood.soroban.prog

/** How many bits a number has, as Windows Calculator names them. */
enum class WordSize(val bits: Int) {
    QWORD(64),
    DWORD(32),
    WORD(16),
    BYTE(8);

    val mask: Long get() = if (bits == 64) -1L else (1L shl bits) - 1

    /** [v] cut to this many bits and read as signed two's complement: what overflow wraps to. */
    fun wrap(v: Long): Long = if (bits == 64) v else {
        val low = v and mask
        if (low and (1L shl (bits - 1)) != 0L) low or mask.inv() else low
    }

    /** The bit pattern of [v] at this size, as an unsigned number for writing in HEX/OCT/BIN. */
    fun pattern(v: Long): ULong = (v and mask).toULong()

    val next: WordSize get() = entries[(ordinal + 1) % entries.size]
}

enum class Base(val radix: Int) {
    HEX(16),
    DEC(10),
    OCT(8),
    BIN(2);

    fun accepts(digit: Char): Boolean = Character.digit(digit, radix) >= 0
}

/** The operators, from tightest to loosest, as C and Windows Calculator bind them. */
enum class Op(val symbol: String, val precedence: Int) {
    MUL("×", 5), DIV("÷", 5), MOD("mod", 5),
    ADD("+", 4), SUB("−", 4),
    SHL("<<", 3), SHR(">>", 3), ROL("rol", 3), ROR("ror", 3),
    AND("AND", 2), NAND("NAND", 2),
    XOR("XOR", 1),
    OR("OR", 0), NOR("NOR", 0),
}

/** A piece of a programmer's sum. Numbers are held as values, so a change of base rewrites them. */
sealed interface PToken {
    data class Num(val value: Long) : PToken
    data class Bin(val op: Op) : PToken
    /** Bitwise NOT, before its operand. */
    data object Not : PToken
    /** Minus before a number, as in −5. */
    data object Neg : PToken
    data object Open : PToken
    data object Close : PToken
}

class ProgrammerException(message: String) : Exception(message)

/**
 * Integer arithmetic at a word size, wrapping on overflow the way Windows Calculator's
 * programmer mode does: every step is cut to the word and read as signed two's complement.
 * Shifts right are arithmetic, keeping the sign; rotates turn within the word.
 */
object Word {

    fun apply(op: Op, a: Long, b: Long, size: WordSize): Long {
        val bits = size.bits
        val r = when (op) {
            Op.ADD -> a + b
            Op.SUB -> a - b
            Op.MUL -> a * b
            Op.DIV -> if (b == 0L) throw ProgrammerException("divide by zero") else if (a == Long.MIN_VALUE && b == -1L) a else a / b
            Op.MOD -> if (b == 0L) throw ProgrammerException("divide by zero") else if (b == -1L) 0L else a % b
            Op.AND -> a and b
            Op.OR -> a or b
            Op.XOR -> a xor b
            Op.NAND -> (a and b).inv()
            Op.NOR -> (a or b).inv()
            // A shift by the word or more empties it (or fills it with the sign).
            Op.SHL -> if (b < 0) throw ProgrammerException("negative shift") else if (b >= bits) 0L else a shl b.toInt()
            Op.SHR -> if (b < 0) throw ProgrammerException("negative shift") else size.wrap(a) shr minOf(b, bits - 1L).toInt()
            Op.ROL -> rotate(a, b, size, left = true)
            Op.ROR -> rotate(a, b, size, left = false)
        }
        return size.wrap(r)
    }

    private fun rotate(a: Long, by: Long, size: WordSize, left: Boolean): Long {
        val bits = size.bits
        val n = (((by % bits) + bits) % bits).toInt()
        if (n == 0) return size.wrap(a)
        val p = a and size.mask
        val turned = if (left) (p shl n) or (p ushr (bits - n)) else (p ushr n) or (p shl (bits - n))
        return size.wrap(turned)
    }

    fun not(a: Long, size: WordSize) = size.wrap(a.inv())

    fun negate(a: Long, size: WordSize) = size.wrap(-a)

    /**
     * Works out a sum. A sum still being typed — a trailing operator, brackets left open —
     * is worked out as far as it goes: open brackets are closed, and a dangling operator is
     * ignored, so the screen can show the answer so far. Throws for division by zero.
     */
    fun evaluate(tokens: List<PToken>, size: WordSize): Long? {
        val t = tokens.toMutableList()
        while (t.isNotEmpty() && (t.last() is PToken.Bin || t.last() is PToken.Not || t.last() is PToken.Neg || t.last() is PToken.Open)) t.removeAt(t.lastIndex)
        if (t.isEmpty()) return null
        val open = t.count { it is PToken.Open } - t.count { it is PToken.Close }
        repeat(maxOf(0, open)) { t += PToken.Close }
        val p = Parser(t, size)
        val v = p.expression(0)
        if (p.pos != t.size) throw ProgrammerException("unbalanced")
        return v
    }

    private class Parser(val t: List<PToken>, val size: WordSize) {
        var pos = 0

        fun expression(minPrecedence: Int): Long {
            var left = unary()
            while (true) {
                val tok = t.getOrNull(pos) as? PToken.Bin ?: break
                if (tok.op.precedence < minPrecedence) break
                pos++
                val right = expression(tok.op.precedence + 1)
                left = apply(tok.op, left, right, size)
            }
            return left
        }

        fun unary(): Long = when (val tok = t.getOrNull(pos) ?: throw ProgrammerException("missing number")) {
            PToken.Not -> { pos++; not(unary(), size) }
            PToken.Neg -> { pos++; negate(unary(), size) }
            PToken.Open -> {
                pos++
                val v = expression(0)
                if (t.getOrNull(pos) != PToken.Close) throw ProgrammerException("unbalanced")
                pos++
                v
            }
            is PToken.Num -> { pos++; size.wrap(tok.value) }
            else -> throw ProgrammerException("missing number")
        }
    }

    /** [v] written in [base]: DEC signed, the others as the word's bit pattern. */
    fun format(v: Long, base: Base, size: WordSize): String = when (base) {
        Base.DEC -> size.wrap(v).toString()
        else -> size.pattern(v).toString(base.radix).uppercase()
    }

    /** Binary in groups of four, leading empty groups left off, as the screen shows it. */
    fun nibbles(v: Long, size: WordSize): String {
        val bits = size.pattern(v).toString(2)
        val padded = bits.padStart((bits.length + 3) / 4 * 4, '0')
        return padded.chunked(4).joinToString(" ")
    }

    /**
     * Appends [digit] to [current] in [base], or returns null when the number would no longer
     * fit the word: the digit is then not taken, as Windows Calculator does.
     */
    fun appendDigit(current: Long, digit: Int, base: Base, size: WordSize): Long? {
        return if (base == Base.DEC) {
            // Typed decimals are positive; the largest is the word's largest signed number.
            val max = if (size.bits == 64) Long.MAX_VALUE else (1L shl (size.bits - 1)) - 1
            if (current > (max - digit) / 10) null else current * 10 + digit
        } else {
            val shift = Integer.numberOfTrailingZeros(base.radix)
            val pattern = size.pattern(current)
            // Room for one more digit only if the top bits it would push out are all zero.
            if (size.bits - shift < 64 && (pattern shr (size.bits - shift)) != 0UL) return null
            if (size.bits == 64 && (pattern shr (64 - shift)) != 0UL) return null
            size.wrap(((pattern shl shift) or digit.toULong()).toLong())
        }
    }

    /** Flips bit [index] of [v], within the word. */
    fun toggle(v: Long, index: Int, size: WordSize): Long =
        if (index >= size.bits) v else size.wrap(v xor (1L shl index))
}
