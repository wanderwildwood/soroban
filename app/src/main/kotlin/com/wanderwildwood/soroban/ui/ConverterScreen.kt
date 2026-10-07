package com.wanderwildwood.soroban.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
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
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.isEqualTo
import com.sadellie.unitto.core.common.trimZeros
import com.sadellie.unitto.core.data.converter.ConverterResult
import com.sadellie.unitto.core.model.converter.UnitGroup
import com.sadellie.unitto.core.model.converter.unit.BasicUnit
import com.wanderwildwood.soroban.Numbers
import com.wanderwildwood.soroban.R
import com.wanderwildwood.soroban.Settings
import com.wanderwildwood.soroban.convert.ConverterViewModel
import com.wanderwildwood.soroban.convert.RatesView
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The converter: the group, what is typed in the unit it is in, and what it comes to in the
 * other, over a keypad. Either unit opens the list of its group's units; the group opens the
 * list of groups.
 */
@Composable
fun ConverterScreen(
    vm: ConverterViewModel,
    onGroups: () -> Unit,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val group by vm.group.collectAsStateWithLifecycle()
    val pair by vm.pair.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    val rates by vm.ratesView.collectAsStateWithLifecycle()
    var actions by rememberSaveable { mutableStateOf<String?>(null) }

    val typed = vm.input.text.toString()
    val numberBase = group == UnitGroup.NUMBER_BASE
    val typedShown = if (numberBase) typed.uppercase() else Numbers.show(typed, settings.symbols)
    val answer = answerText(result, settings, money = group == UnitGroup.CURRENCY)

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(48.dp).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).fillMaxHeight().clickable(onClick = onGroups),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextMMD(text = stringResource(group.res), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.ChevronRight, contentDescription = stringResource(R.string.cd_groups), modifier = Modifier.size(22.dp))
            }
            BarButton(Icons.SwapVert, stringResource(R.string.cd_swap)) { vm.swap() }
        }
        HorizontalDividerMMD()

        Side(
            unit = pair.first,
            value = typedShown.ifEmpty { "0" },
            onUnit = onPickFrom,
            onHold = { actions = Numbers.copyText(typed, settings.symbols).let { if (numberBase) typed.uppercase() else it } },
        )
        HorizontalDividerMMD()
        Side(
            unit = pair.second,
            value = answer ?: "",
            onUnit = onPickTo,
            onHold = { actions = answer?.let(::plain) ?: "" },
        )
        Note(group = group, rates = rates, result = result, vm = vm)
        HorizontalDividerMMD()

        Keypad(
            if (numberBase) baseKeys(vm, pair.first) else numberKeys(vm, settings.symbols.fractional),
            Modifier.weight(1f),
        )
    }

    actions?.let { text ->
        ValueActions(
            text = text,
            onPaste = { pasted ->
                val tokens = if (numberBase) pasted.trim().uppercase().filter { Character.digit(it, pair.first.factor.intValueExact()) >= 0 }.ifEmpty { null }
                else Numbers.parse(pasted, settings.symbols)
                if (tokens != null) vm.replaceInput(tokens)
                else Toast.makeText(context, context.getString(R.string.paste_nothing), Toast.LENGTH_SHORT).show()
            },
            onDismiss = { actions = null },
        )
    }
}

/** An answer as text for another app, without the unit-pair layout around it. */
private fun plain(s: String) = s.trim()

/**
 * What a converted value is written as: rounded, separated, and for feet or pounds split.
 * Money is rounded to cents, or for a coin worth less, to its first figure that is not 0,
 * whatever the decimal places set: 44.58 euros, not 44.5836775.
 */
@Composable
fun answerText(result: ConverterResult?, settings: Settings, money: Boolean = false): String? = when (result) {
    null, ConverterResult.Loading -> null
    is ConverterResult.Default -> Numbers.show(result.value, if (money) settings.copy(precision = minOf(settings.precision, 2)) else settings)
    is ConverterResult.NumberBase -> result.value.uppercase()
    is ConverterResult.FootInch -> stringResource(
        R.string.convert_feet_inches,
        Numbers.show(result.foot, settings.copy(precision = 0)),
        Numbers.show(result.inch, settings),
    )
    is ConverterResult.PoundOunce -> stringResource(
        R.string.convert_pounds_ounces,
        Numbers.show(result.pound, settings.copy(precision = 0)),
        Numbers.show(result.ounce, settings),
    )
    is ConverterResult.Time -> null
    ConverterResult.Error.CurrencyError -> stringResource(R.string.convert_no_rate)
    ConverterResult.Error.DivideByZeroError -> stringResource(R.string.calc_divide_by_zero)
}

/** One side of the conversion: its unit, which opens the list, and its value, large. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Side(unit: BasicUnit, value: String, onUnit: () -> Unit, onHold: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().height(30.dp).clickable(onClick = onUnit),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextMMD(
                text = stringResource(unit.displayName),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(6.dp))
            TextMMD(text = stringResource(unit.shortName), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Icon(Icons.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .combinedClickable(onClick = onUnit, onLongClick = onHold, onLongClickLabel = stringResource(R.string.cd_sum_actions)),
            contentAlignment = Alignment.CenterEnd,
        ) {
            TextMMD(
                text = value,
                // An instrument's reading, as on the calculator.
                fontSize = if (value.length > 16) 24.sp else 34.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/**
 * One line under the answer, when there is something to say: for currency, whose rates these
 * are and from which day, or that there are none; for time, the answer in days, hours and
 * minutes.
 */
@Composable
private fun Note(group: UnitGroup, rates: RatesView, result: ConverterResult?, vm: ConverterViewModel) {
    val text: String?
    var retry = false
    if (group == UnitGroup.CURRENCY) {
        val date = rates.table?.date?.let { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(it) }
        text = when {
            rates.fetching && date == null -> stringResource(R.string.rates_fetching)
            rates.fetching -> stringResource(R.string.rates_fetching_have, date!!)
            rates.failed && date == null -> stringResource(R.string.rates_none).also { retry = true }
            rates.failed -> stringResource(R.string.rates_failed, date!!).also { retry = true }
            date != null -> stringResource(R.string.rates_of, date)
            else -> null
        }
    } else if (group == UnitGroup.TIME) {
        text = vm.input.text.toString().takeIf { it.isNotEmpty() }
            ?.let { runCatching { vm.timeBreakdown() }.getOrNull() }
            ?.let { timeText(it) }
    } else {
        text = null
    }
    // The line is always there, empty or not, so the keypad never moves under the thumb.
    Box(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .let { if (retry) it.clickable { vm.retryRates() } else it }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        TextMMD(text = text ?: "", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A length of time as days, hours, minutes and on down, leaving out the parts that are 0. */
@Composable
private fun timeText(t: ConverterResult.Time): String? {
    val parts = listOf(
        t.day to R.string.time_days,
        t.hour to R.string.time_hours,
        t.minute to R.string.time_minutes,
        t.second to R.string.time_seconds,
        t.millisecond to R.string.time_milliseconds,
        t.microsecond to R.string.time_microseconds,
        t.nanosecond to R.string.time_nanoseconds,
        t.attosecond to R.string.time_attoseconds,
    ).filter { !it.first.isEqualTo(KBigDecimal.ZERO) }
    // "1 h" beside "1 hour" says nothing new; "1 d 2 h 30 min" does.
    if (parts.size < 2) return null
    val words = parts.map { (n, res) -> stringResource(res, n.trimZeros().toPlainString()) }
    return (if (t.negative) Token.Operator.MINUS else "") + words.joinToString(" ")
}

@Composable
private fun numberKeys(vm: ConverterViewModel, point: String): List<List<Key?>> {
    val add: (String) -> Unit = { vm.key(it) }
    fun d(t: String) = Key(t, { add(t) })
    return listOf(
        listOf(
            Key(stringResource(R.string.key_clear), vm::clear, small = true),
            Key(Token.Operator.LEFT_BRACKET, { add(Token.Operator.LEFT_BRACKET) }),
            Key(Token.Operator.RIGHT_BRACKET, { add(Token.Operator.RIGHT_BRACKET) }),
            Key("", vm::delete, icon = Icons.Backspace, description = stringResource(R.string.cd_backspace)),
        ),
        listOf(d("7"), d("8"), d("9"), d(Token.Operator.DIVIDE)),
        listOf(d("4"), d("5"), d("6"), d(Token.Operator.MULTIPLY)),
        listOf(d("1"), d("2"), d("3"), d(Token.Operator.MINUS)),
        listOf(Key(point, { add(Token.Digit.DOT) }), d("0"), Key("xʸ", { add(Token.Operator.POWER) }, small = true), d(Token.Operator.PLUS)),
    )
}

/** Digits for a number base: only those the base has are shown; the rest are left blank. */
@Composable
private fun baseKeys(vm: ConverterViewModel, from: BasicUnit): List<List<Key?>> {
    val base = from.factor.intValueExact()
    fun d(t: String) = Key(t, { vm.key(t) }, enabled = Character.digit(t[0], base) >= 0)
    return listOf(
        listOf(d("D"), d("E"), d("F"), Key("", vm::delete, icon = Icons.Backspace, description = stringResource(R.string.cd_backspace))),
        listOf(d("A"), d("B"), d("C"), Key(stringResource(R.string.key_clear_word), vm::clear, small = true)),
        listOf(d("7"), d("8"), d("9"), null),
        listOf(d("4"), d("5"), d("6"), null),
        listOf(d("1"), d("2"), d("3"), d("0")),
    )
}

/** Held down, a value offers to be copied or shared, or replaced from the clipboard. */
@Composable
fun ValueActions(text: String, onPaste: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(text = text.ifEmpty { "0" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(14.dp))
        if (text.isNotEmpty()) {
            ActionButton(stringResource(R.string.action_copy)) {
                clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), text))
                onDismiss()
            }
            ActionButton(stringResource(R.string.action_share)) {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                runCatching { context.startActivity(Intent.createChooser(send, null)) }
                    .onFailure { Toast.makeText(context, context.getString(R.string.share_nothing), Toast.LENGTH_SHORT).show() }
                onDismiss()
            }
        }
        ActionButton(stringResource(R.string.action_paste)) {
            val pasted = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
            if (pasted.isNullOrBlank()) Toast.makeText(context, context.getString(R.string.paste_empty), Toast.LENGTH_SHORT).show()
            else onPaste(pasted)
            onDismiss()
        }
        ActionButton(stringResource(R.string.cancel), onDismiss)
    }
}
