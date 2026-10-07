package com.wanderwildwood.soroban.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import com.sadellie.unitto.core.common.Token
import com.wanderwildwood.soroban.Numbers
import com.wanderwildwood.soroban.R
import com.wanderwildwood.soroban.calc.CalculationResult
import com.wanderwildwood.soroban.calc.CalculatorViewModel

/**
 * The calculator: the sum and its answer over a keypad of big plain keys.
 *
 * The keypad has two pages on the same grid. The first is an ordinary calculator; "fx" turns
 * to the second, the scientific functions. A function, once pressed, goes into the sum and
 * turns the page back, the way a shift key lets go, so "sin 30" is fx, sin, 3, 0. The keys
 * stay the size they are rather than shrinking to fit both pages on one screen.
 */
@Composable
fun CalculatorScreen(vm: CalculatorViewModel, onHistory: () -> Unit) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    val equalled by vm.equalled.collectAsStateWithLifecycle()
    val lines by vm.lines.collectAsStateWithLifecycle()
    var functions by rememberSaveable { mutableStateOf(false) }
    var actions by rememberSaveable { mutableStateOf(false) }

    val text = vm.input.text.toString()
    val shown = Numbers.show(text, settings.symbols)

    Column(Modifier.fillMaxSize()) {
        Display(
            tape = lines.firstOrNull()?.let {
                Numbers.show(it.expression, settings.symbols) + " = " + Numbers.show(it.result, settings.symbols)
            },
            sum = shown.ifEmpty { "0" },
            below = when (val r = result) {
                is CalculationResult.Success -> r.text.takeIf { it.isNotEmpty() }?.let { if (equalled) Numbers.show(it, settings.symbols) else "= " + Numbers.show(it, settings.symbols) }
                CalculationResult.DivideByZeroError -> stringResource(R.string.calc_divide_by_zero)
                CalculationResult.Error -> stringResource(R.string.calc_error)
                CalculationResult.Empty -> null
            },
            onTape = onHistory,
            onHold = { actions = true },
        )

        val add: (String) -> Unit = { vm.addTokens(it) }
        val keys = if (!functions) {
            numberPage(vm, add, settings.symbols.fractional, onFunctions = { functions = true })
        } else {
            functionPage(
                vm = vm,
                radians = settings.radians,
                insert = { token ->
                    vm.addTokens(token)
                    functions = false
                },
                onNumbers = { functions = false },
            )
        }
        HorizontalDividerMMD()
        Keypad(keys, Modifier.weight(1f))
    }

    if (actions) {
        ValueActions(
            text = Numbers.copyText(text, settings.symbols),
            onPaste = { pasted ->
                Numbers.parse(pasted, settings.symbols)?.let { vm.replaceInput(it) }
                    ?: Toast.makeText(context, context.getString(R.string.paste_nothing), Toast.LENGTH_SHORT).show()
            },
            onDismiss = { actions = false },
        )
    }
}

@Composable
private fun numberPage(vm: CalculatorViewModel, add: (String) -> Unit, point: String, onFunctions: () -> Unit): List<List<Key?>> {
    val backspace = stringResource(R.string.cd_backspace)
    val clear = stringResource(R.string.key_clear)
    return listOf(
        listOf(
            Key(clear, vm::clearInput, small = true),
            Key(Token.Operator.LEFT_BRACKET, { add(Token.Operator.LEFT_BRACKET) }),
            Key(Token.Operator.RIGHT_BRACKET, { add(Token.Operator.RIGHT_BRACKET) }),
            Key("", vm::deleteTokens, icon = Icons.Backspace, description = backspace),
        ),
        listOf(
            Key(Token.Operator.PERCENT, { add(Token.Operator.PERCENT) }),
            Key(Token.Operator.SQRT, { add(Token.Operator.SQRT) }),
            Key("xʸ", { add(Token.Operator.POWER) }, small = true),
            Key(Token.Operator.DIVIDE, { add(Token.Operator.DIVIDE) }),
        ),
        listOf(digit("7", add), digit("8", add), digit("9", add), Key(Token.Operator.MULTIPLY, { add(Token.Operator.MULTIPLY) })),
        listOf(digit("4", add), digit("5", add), digit("6", add), Key(Token.Operator.MINUS, { add(Token.Operator.MINUS) })),
        listOf(digit("1", add), digit("2", add), digit("3", add), Key(Token.Operator.PLUS, { add(Token.Operator.PLUS) })),
        listOf(
            Key(stringResource(R.string.key_functions), onFunctions, small = true),
            digit("0", add),
            // The key says the decimal mark as the numbers are written: "." or ",".
            Key(point, { add(Token.Digit.DOT) }),
            Key("=", { vm.onEqualClick() }, filled = true),
        ),
    )
}

@Composable
private fun functionPage(
    vm: CalculatorViewModel,
    radians: Boolean,
    insert: (String) -> Unit,
    onNumbers: () -> Unit,
): List<List<Key?>> {
    val backspace = stringResource(R.string.cd_backspace)
    val clear = stringResource(R.string.key_clear)
    val f = Token.Func
    return listOf(
        listOf(
            Key(f.SIN, { insert(f.SIN_BRACKET) }, small = true),
            Key(f.COS, { insert(f.COS_BRACKET) }, small = true),
            Key(f.TAN, { insert(f.TAN_BRACKET) }, small = true),
            // Says which it is now; a press changes it, and stays on this page.
            Key(
                stringResource(if (radians) R.string.key_radians else R.string.key_degrees),
                { vm.updateRadianMode(!radians) },
                small = true,
                description = stringResource(if (radians) R.string.cd_radians else R.string.cd_degrees),
            ),
        ),
        listOf(
            Key(f.ARSIN, { insert(f.ARSIN_BRACKET) }, small = true),
            Key(f.ARCOS, { insert(f.ARCOS_BRACKET) }, small = true),
            Key(f.ACTAN, { insert(f.ACTAN_BRACKET) }, small = true),
            Key("x!", { insert(Token.Operator.FACTORIAL) }, small = true),
        ),
        listOf(
            Key(f.LN, { insert(f.LN_BRACKET) }, small = true),
            Key(f.LOG, { insert(f.LOG_BRACKET) }, small = true),
            Key("eˣ", { insert(f.EXP_BRACKET) }, small = true),
            Key("10ˣ", { insert("10" + Token.Operator.POWER) }, small = true),
        ),
        listOf(
            Key(Token.Const.PI, { insert(Token.Const.PI) }),
            Key(Token.Const.E, { insert(Token.Const.E) }),
            Key("x²", { insert(Token.Operator.POWER + "2") }, small = true),
            Key("mod", { insert(Token.Operator.MODULO) }, small = true),
        ),
        listOf(
            Key(clear, vm::clearInput, small = true),
            Key(Token.Operator.LEFT_BRACKET, { insert(Token.Operator.LEFT_BRACKET) }),
            Key(Token.Operator.RIGHT_BRACKET, { insert(Token.Operator.RIGHT_BRACKET) }),
            Key("", vm::deleteTokens, icon = Icons.Backspace, description = backspace),
        ),
        listOf(
            Key(stringResource(R.string.key_numbers), onNumbers, small = true),
            Key(Token.Operator.SQRT, { insert(Token.Operator.SQRT) }),
            Key("xʸ", { insert(Token.Operator.POWER) }, small = true),
            Key("=", {
                vm.onEqualClick()
                onNumbers()
            }, filled = true),
        ),
    )
}

private fun digit(d: String, add: (String) -> Unit) = Key(d, { add(d) })

/**
 * The top of the screen: the last sum worked out, small, which opens the tape; the sum being
 * typed, large; and under it the answer so far, or after = the answer as a fraction.
 *
 * A long sum keeps its end in view, where the typing is, and loses its beginning off the left.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Display(tape: String?, sum: String, below: String?, onTape: () -> Unit, onHold: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .combinedClickable(onClick = onTape),
            contentAlignment = Alignment.CenterEnd,
        ) {
            TextMMD(
                text = tape ?: "",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(104.dp)
                .combinedClickable(onClick = {}, onLongClick = onHold, onLongClickLabel = stringResource(R.string.cd_sum_actions)),
            contentAlignment = Alignment.BottomEnd,
        ) {
            TextMMD(
                text = tail(sum, 40),
                // The sum is the instrument's reading, sized to be read at arm's length.
                fontSize = if (sum.length > 13) 30.sp else 42.sp,
                lineHeight = if (sum.length > 13) 36.sp else 48.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Box(Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.CenterEnd) {
            TextMMD(
                text = below ?: "",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The last [n] characters of [s], marked as cut where the beginning was dropped. */
private fun tail(s: String, n: Int): String = if (s.length <= n) s else "…" + s.takeLast(n - 1)

@Composable
fun ActionButton(label: String, onClick: () -> Unit) {
    OutlinedButtonMMD(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(48.dp)) {
        TextMMD(text = label, style = MaterialTheme.typography.bodySmall)
    }
}
