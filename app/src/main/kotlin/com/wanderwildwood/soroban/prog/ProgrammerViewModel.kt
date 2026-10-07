package com.wanderwildwood.soroban.prog

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wanderwildwood.soroban.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Everything the programmer's screen shows. */
data class ProgState(
    val tokens: List<PToken> = emptyList(),
    val base: Base = Base.DEC,
    val size: WordSize = WordSize.QWORD,
    /** True straight after =, while the answer stands in the sum's place. */
    val equalled: Boolean = false,
) {
    /** The sum's value so far, or null with nothing typed. */
    val value: Long? get() = runCatching { Word.evaluate(tokens, size) }.getOrNull()

    /** Why the sum has no value, when it has none but something is typed. */
    val error: Boolean get() = tokens.isNotEmpty() && runCatching { Word.evaluate(tokens, size) }.isFailure
}

/**
 * The programmer's calculator: whole numbers at a word size, typed in one base and shown in all
 * four. The sum is held as numbers and operators rather than as text, so changing the base
 * rewrites what is on the screen without changing what it means.
 */
class ProgrammerViewModel(private val prefs: Prefs, private val saved: SavedStateHandle) : ViewModel() {

    private val _state = MutableStateFlow(
        ProgState(
            tokens = decode(saved.get<String>(KEY).orEmpty()),
            base = prefs.progBase?.let { b -> Base.entries.firstOrNull { it.name == b } } ?: Base.DEC,
            size = prefs.progSize?.let { s -> WordSize.entries.firstOrNull { it.name == s } } ?: WordSize.QWORD,
            equalled = saved.get<Boolean>(EQUALLED) ?: false,
        )
    )
    val state: StateFlow<ProgState> = _state.asStateFlow()

    private fun set(next: ProgState) {
        _state.value = next
        saved[KEY] = encode(next.tokens)
        saved[EQUALLED] = next.equalled
    }

    fun digit(d: Char) {
        val s = _state.value
        val n = Character.digit(d, s.base.radix)
        if (n < 0) return
        val tokens = if (s.equalled) emptyList() else s.tokens
        val last = tokens.lastOrNull()
        val next = when (last) {
            is PToken.Num -> Word.appendDigit(last.value, n, s.base, s.size)?.let { tokens.dropLast(1) + PToken.Num(it) } ?: return
            PToken.Close -> return
            else -> tokens + PToken.Num(n.toLong())
        }
        set(s.copy(tokens = next, equalled = false))
    }

    fun op(op: Op) {
        val s = _state.value
        val t = s.tokens
        val next = when (val last = t.lastOrNull()) {
            null, PToken.Open, PToken.Not, PToken.Neg ->
                if (op == Op.SUB && last != PToken.Neg) t + PToken.Neg else return
            is PToken.Bin -> t.dropLast(1) + PToken.Bin(op)
            else -> t + PToken.Bin(op)
        }
        set(s.copy(tokens = next, equalled = false))
    }

    /** NOT of the number just typed, or of what comes next. */
    fun not() {
        val s = _state.value
        val t = s.tokens
        val next = when (val last = t.lastOrNull()) {
            is PToken.Num -> t.dropLast(1) + PToken.Num(Word.not(last.value, s.size))
            PToken.Close -> listOf(PToken.Num(Word.not(s.value ?: return, s.size)))
            else -> t + PToken.Not
        }
        set(s.copy(tokens = next, equalled = false))
    }

    fun open() {
        val s = _state.value
        val t = if (s.equalled) emptyList() else s.tokens
        if (t.lastOrNull() is PToken.Num || t.lastOrNull() == PToken.Close) return
        set(s.copy(tokens = t + PToken.Open, equalled = false))
    }

    fun close() {
        val s = _state.value
        val t = s.tokens
        val open = t.count { it == PToken.Open } - t.count { it == PToken.Close }
        if (open <= 0) return
        if (t.lastOrNull() !is PToken.Num && t.lastOrNull() != PToken.Close) return
        set(s.copy(tokens = t + PToken.Close, equalled = false))
    }

    fun backspace() {
        val s = _state.value
        if (s.equalled) return clear()
        val t = s.tokens
        val next = when (val last = t.lastOrNull() ?: return) {
            is PToken.Num -> {
                val shorter = dropDigit(last.value, s.base, s.size)
                if (shorter == null) t.dropLast(1) else t.dropLast(1) + PToken.Num(shorter)
            }
            else -> t.dropLast(1)
        }
        set(s.copy(tokens = next))
    }

    /** [v] with its last digit in [base] taken off, or null when it had only one. */
    private fun dropDigit(v: Long, base: Base, size: WordSize): Long? {
        val written = Word.format(v, base, size).removePrefix("-")
        if (written.length <= 1) return null
        return if (base == Base.DEC) v / 10 else size.wrap((size.pattern(v) / base.radix.toULong()).toLong())
    }

    fun clear() = set(_state.value.copy(tokens = emptyList(), equalled = false))

    fun equals() {
        val s = _state.value
        val v = runCatching { Word.evaluate(s.tokens, s.size) }.getOrNull() ?: return
        set(s.copy(tokens = listOf(PToken.Num(v)), equalled = true))
    }

    fun setBase(base: Base) {
        prefs.progBase = base.name
        _state.value = _state.value.copy(base = base)
    }

    /** The next word size; every number in the sum is cut to it, as overflow would. */
    fun cycleSize() {
        val s = _state.value
        val size = s.size.next
        prefs.progSize = size.name
        set(s.copy(size = size, tokens = s.tokens.map { if (it is PToken.Num) PToken.Num(size.wrap(it.value)) else it }))
    }

    /** Flips one bit of the current value; the value then stands alone as the sum. */
    fun toggleBit(index: Int) {
        val s = _state.value
        val v = s.value ?: 0L
        set(s.copy(tokens = listOf(PToken.Num(Word.toggle(v, index, s.size))), equalled = true))
    }

    /** Puts a pasted number, read in the input base, in place of the sum. */
    fun paste(text: String): Boolean {
        val s = _state.value
        val clean = text.trim().removePrefix("0x").removePrefix("0X").replace(" ", "").replace("_", "")
        val negative = s.base == Base.DEC && (clean.startsWith("-") || clean.startsWith("−"))
        val digits = clean.removePrefix("-").removePrefix("−")
        if (digits.isEmpty()) return false
        var v = 0L
        for (c in digits) {
            val d = Character.digit(c, s.base.radix)
            if (d < 0) return false
            v = Word.appendDigit(v, d, s.base, s.size) ?: return false
        }
        set(s.copy(tokens = listOf(PToken.Num(if (negative) Word.negate(v, s.size) else v)), equalled = true))
        return true
    }

    companion object {
        private const val KEY = "PROG_TOKENS"
        private const val EQUALLED = "PROG_EQUALLED"

        fun encode(tokens: List<PToken>): String = tokens.joinToString(" ") {
            when (it) {
                is PToken.Num -> "n${it.value}"
                is PToken.Bin -> "o${it.op.name}"
                PToken.Not -> "!"
                PToken.Neg -> "~"
                PToken.Open -> "("
                PToken.Close -> ")"
            }
        }

        fun decode(s: String): List<PToken> = s.split(' ').filter { it.isNotEmpty() }.mapNotNull {
            when {
                it.startsWith("n") -> it.drop(1).toLongOrNull()?.let(PToken::Num)
                it.startsWith("o") -> Op.entries.firstOrNull { o -> o.name == it.drop(1) }?.let(PToken::Bin)
                it == "!" -> PToken.Not
                it == "~" -> PToken.Neg
                it == "(" -> PToken.Open
                it == ")" -> PToken.Close
                else -> null
            }
        }

        fun factory(app: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer { ProgrammerViewModel(Prefs.get(app), createSavedStateHandle()) }
        }
    }
}
