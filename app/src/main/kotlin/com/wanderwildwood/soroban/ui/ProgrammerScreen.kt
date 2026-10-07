package com.wanderwildwood.soroban.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.soroban.R
import com.wanderwildwood.soroban.prog.Base
import com.wanderwildwood.soroban.prog.Op
import com.wanderwildwood.soroban.prog.PToken
import com.wanderwildwood.soroban.prog.ProgState
import com.wanderwildwood.soroban.prog.ProgrammerViewModel
import com.wanderwildwood.soroban.prog.Word

/**
 * The programmer's calculator: the sum in the base it is typed in, its value in HEX, DEC, OCT
 * and BIN together (press one to type in it), the word size, and a keypad on two pages, digits
 * and bit operations. "Bits" puts the value's bits in place of the keys, to flip one at a time.
 */
@Composable
fun ProgrammerScreen(vm: ProgrammerViewModel, onLeave: () -> Unit) {
    val context = LocalContext.current
    val s by vm.state.collectAsStateWithLifecycle()
    var ops by rememberSaveable { mutableStateOf(false) }
    var bits by rememberSaveable { mutableStateOf(false) }
    var actions by rememberSaveable { mutableStateOf(false) }
    val value = s.value

    Column(Modifier.fillMaxSize()) {
        ProgDisplay(s, value, onHold = { actions = true }, onBase = vm::setBase)
        HorizontalDividerMMD()
        // The modes, as one row of words: what the keys below are, and the word they work in.
        Keypad(
            listOf(
                listOf(
                    Key(s.size.name, vm::cycleSize, mode = true, description = stringResource(R.string.cd_word_size, s.size.name)),
                    Key(stringResource(R.string.key_bits), { bits = !bits }, mode = true, bold = bits),
                    Key(stringResource(if (ops) R.string.key_digits else R.string.key_ops), { ops = !ops; bits = false }, mode = true),
                    Key(stringResource(R.string.key_numbers), onLeave, mode = true, description = stringResource(R.string.cd_leave_programmer)),
                ),
            ),
            Modifier.height(44.dp),
        )
        HorizontalDividerMMD()
        if (bits) {
            BitGrid(value ?: 0L, s, vm::toggleBit, Modifier.weight(1f))
        } else {
            Keypad(if (ops) opsPage(vm, s) else digitPage(vm, s), Modifier.weight(1f))
        }
    }

    if (actions) {
        val text = value?.let { Word.format(it, s.base, s.size) }.orEmpty()
        ValueActions(
            text = text,
            onPaste = { pasted ->
                if (!vm.paste(pasted)) Toast.makeText(context, context.getString(R.string.paste_nothing), Toast.LENGTH_SHORT).show()
            },
            onDismiss = { actions = false },
        )
    }
}

/** The sum as typed, then the value in each of the four bases, the one typed in bold. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProgDisplay(s: ProgState, value: Long?, onHold: () -> Unit, onBase: (Base) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Box(
            Modifier.fillMaxWidth().height(44.dp)
                .combinedClickable(onClick = {}, onLongClick = onHold, onLongClickLabel = stringResource(R.string.cd_sum_actions)),
            contentAlignment = Alignment.CenterEnd,
        ) {
            val sum = show(s)
            TextMMD(
                text = when {
                    s.error -> stringResource(R.string.calc_error)
                    sum.isEmpty() -> "0"
                    else -> tailOf(sum, 26)
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Base.entries.forEach { base ->
            val text = value?.let { if (base == Base.BIN) Word.nibbles(it, s.size) else Word.format(it, base, s.size).replace("-", "−") } ?: "0"
            Row(
                Modifier.fillMaxWidth()
                    .height(if (base == Base.BIN) 42.dp else 24.dp)
                    .clickable { onBase(base) }
                    .semantics { contentDescription = base.name },
                verticalAlignment = Alignment.Top,
            ) {
                TextMMD(
                    text = base.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (base == s.base) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.width(44.dp),
                )
                TextMMD(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (base == s.base) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = if (base == Base.BIN) FontFamily.Monospace else null,
                    textAlign = TextAlign.End,
                    maxLines = if (base == Base.BIN) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** The sum written out in the base being typed in. */
private fun show(s: ProgState): String = s.tokens.joinToString("") {
    when (it) {
        is PToken.Num -> Word.format(it.value, s.base, s.size).replace("-", "−")
        is PToken.Bin -> " ${it.op.symbol} "
        PToken.Not -> "NOT "
        PToken.Neg -> "−"
        PToken.Open -> "("
        PToken.Close -> ")"
    }
}

private fun tailOf(s: String, n: Int) = if (s.length <= n) s else "…" + s.takeLast(n - 1)

@Composable
private fun digitPage(vm: ProgrammerViewModel, s: ProgState): List<List<Key?>> {
    fun d(c: Char) = Key(c.toString(), { vm.digit(c) }, enabled = s.base.accepts(c))
    fun o(op: Op) = Key(op.symbol, { vm.op(op) })
    return listOf(
        listOf(d('A'), d('B'), d('C'), d('D'), d('E')),
        listOf(
            d('F'),
            Key("(", vm::open),
            Key(")", vm::close),
            Key(stringResource(R.string.key_clear_word), vm::clear, small = true),
            Key("", vm::backspace, icon = Icons.Backspace, description = stringResource(R.string.cd_backspace)),
        ),
        listOf(d('7'), d('8'), d('9'), o(Op.MUL), o(Op.DIV)),
        listOf(d('4'), d('5'), d('6'), o(Op.SUB), o(Op.ADD)),
        listOf(d('1'), d('2'), d('3'), d('0'), Key("=", vm::equals, filled = true)),
    )
}

@Composable
private fun opsPage(vm: ProgrammerViewModel, s: ProgState): List<List<Key?>> {
    fun d(c: Char) = Key(c.toString(), { vm.digit(c) }, enabled = s.base.accepts(c))
    fun o(op: Op, label: String = op.symbol) = Key(label, { vm.op(op) }, small = true)
    return listOf(
        listOf(o(Op.AND), o(Op.OR), o(Op.XOR), Key("NOT", vm::not, small = true), o(Op.NAND)),
        listOf(o(Op.NOR), o(Op.SHL), o(Op.SHR), o(Op.ROL), o(Op.ROR)),
        listOf(d('7'), d('8'), d('9'), o(Op.MOD), Key("", vm::backspace, icon = Icons.Backspace, description = stringResource(R.string.cd_backspace))),
        listOf(d('4'), d('5'), d('6'), Key("(", vm::open), Key(")", vm::close)),
        listOf(d('1'), d('2'), d('3'), d('0'), Key("=", vm::equals, filled = true)),
    )
}

/**
 * The value's 64 bits, eight to a row, highest first, each row split into its two nibbles. A
 * set bit is black with a white 1; a clear one is a 0 in a box. Bits above the word size are
 * left blank. Pressing a bit flips it.
 */
@Composable
private fun BitGrid(value: Long, s: ProgState, onToggle: (Int) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        for (row in 0 until 8) {
            val high = 63 - row * 8
            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                TextMMD(
                    text = high.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(26.dp).padding(end = 4.dp),
                )
                for (i in 0 until 8) {
                    val index = high - i
                    if (i == 4) Spacer(Modifier.width(10.dp))
                    val live = index < s.size.bits
                    val on = live && (value ushr index) and 1L == 1L
                    Box(
                        Modifier.weight(1f).fillMaxHeight().padding(2.dp)
                            .let { if (!live) it else if (on) it.background(Color.Black) else it.rule(1, 4) }
                            .let { if (live) it.clickable { onToggle(index) } else it }
                            .semantics { contentDescription = "bit $index" },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (live) {
                            TextMMD(
                                text = if (on) "1" else "0",
                                color = if (on) Color.White else Color.Black,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}
