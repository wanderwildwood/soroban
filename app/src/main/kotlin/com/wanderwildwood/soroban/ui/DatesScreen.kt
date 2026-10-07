package com.wanderwildwood.soroban.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.mudita.mmd.components.time.DatePickerFormatterMMD
import com.mudita.mmd.components.time.DatePickerMMD
import com.mudita.mmd.components.time.rememberDatePickerMMDState
import com.wanderwildwood.soroban.R
import com.wanderwildwood.soroban.dates.DateMath
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val dateSaver = Saver<LocalDate, Long>(save = { it.toEpochDay() }, restore = { LocalDate.ofEpochDay(it) })

/**
 * Dates: how long between two days, or which day it is so many years, months and days from
 * another. Days only, with no time of day: what a calendar answers, not a stopwatch.
 */
@Composable
fun DatesScreen() {
    var between by rememberSaveable { mutableStateOf(true) }
    var from by rememberSaveable(stateSaver = dateSaver) { mutableStateOf(LocalDate.now()) }
    var to by rememberSaveable(stateSaver = dateSaver) { mutableStateOf(LocalDate.now()) }
    var start by rememberSaveable(stateSaver = dateSaver) { mutableStateOf(LocalDate.now()) }
    var back by rememberSaveable { mutableStateOf(false) }
    var years by rememberSaveable { mutableStateOf("") }
    var months by rememberSaveable { mutableStateOf("") }
    var days by rememberSaveable { mutableStateOf("") }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumnMMD(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item(key = "mode") {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                ModeButton(stringResource(R.string.dates_between), between, Modifier.weight(1f)) { between = true }
                Spacer(Modifier.width(10.dp))
                ModeButton(stringResource(R.string.dates_shift), !between, Modifier.weight(1f)) { between = false }
            }
        }
        if (between) {
            item(key = "from") { DateRow(stringResource(R.string.dates_from), from) { picking = "from" } }
            item(key = "to") { DateRow(stringResource(R.string.dates_to), to) { picking = "to" } }
            item(key = "answer") {
                val b = DateMath.between(from, to)
                Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
                    TextMMD(text = ymd(b.years, b.months, b.days), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    // In days as well, unless that is what the line above already says.
                    if (b.years > 0 || b.months > 0) {
                        TextMMD(
                            text = pluralStringResource(R.plurals.days, b.totalDays.toInt(), "%,d".format(b.totalDays)),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (b.weeks > 0) {
                        TextMMD(
                            text = pluralStringResource(R.plurals.weeks, b.weeks.toInt(), "%,d".format(b.weeks)) +
                                if (b.weekDays > 0) ", " + pluralStringResource(R.plurals.days, b.weekDays.toInt(), b.weekDays.toString()) else "",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        } else {
            item(key = "start") { DateRow(stringResource(R.string.dates_start), start) { picking = "start" } }
            item(key = "sign") {
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    ModeButton(stringResource(R.string.dates_add), !back, Modifier.weight(1f)) { back = false }
                    Spacer(Modifier.width(10.dp))
                    ModeButton(stringResource(R.string.dates_subtract), back, Modifier.weight(1f)) { back = true }
                }
            }
            item(key = "amounts") {
                Row(Modifier.fillMaxWidth()) {
                    Amount(stringResource(R.string.dates_years), years, Modifier.weight(1f)) { years = it }
                    Spacer(Modifier.width(10.dp))
                    Amount(stringResource(R.string.dates_months), months, Modifier.weight(1f)) { months = it }
                    Spacer(Modifier.width(10.dp))
                    Amount(stringResource(R.string.dates_days), days, Modifier.weight(1f)) { days = it }
                }
            }
            item(key = "result") {
                val result = runCatching {
                    DateMath.shift(start, years.toLongOrNull() ?: 0, months.toLongOrNull() ?: 0, days.toLongOrNull() ?: 0, back)
                }.getOrNull()
                Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
                    TextMMD(
                        text = result?.let { long(it) } ?: stringResource(R.string.dates_out_of_range),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    picking?.let { which ->
        val initial = when (which) {
            "from" -> from
            "to" -> to
            else -> start
        }
        DateDialog(initial = initial, onPick = {
            when (which) {
                "from" -> from = it
                "to" -> to = it
                else -> start = it
            }
        }, onDismiss = { picking = null })
    }
}

/** "2 years, 3 months, 5 days", leaving out what is 0, or "the same day". */
@Composable
private fun ymd(y: Int, m: Int, d: Int): String {
    val parts = buildList {
        if (y > 0) add(pluralStringResource(R.plurals.years, y, y.toString()))
        if (m > 0) add(pluralStringResource(R.plurals.months, m, m.toString()))
        if (d > 0) add(pluralStringResource(R.plurals.days, d, d.toString()))
    }
    return if (parts.isEmpty()) stringResource(R.string.dates_same_day) else parts.joinToString(", ")
}

private fun long(date: LocalDate): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault()).format(date)

/** One of two ways, side by side; the one chosen has the solid edge and the bold word. */
@Composable
private fun ModeButton(label: String, chosen: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(48.dp)
            .let { if (chosen) it.rule(2) else it.rule(1) }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TextMMD(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun DateRow(label: String, date: LocalDate, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(vertical = 10.dp)) {
            TextMMD(text = label, style = MaterialTheme.typography.labelSmall)
            TextMMD(text = long(date), style = MaterialTheme.typography.bodyMedium)
        }
        HorizontalDividerMMD()
    }
}

@Composable
private fun Amount(label: String, value: String, modifier: Modifier, onValue: (String) -> Unit) {
    Column(modifier) {
        TextMMD(text = label, style = MaterialTheme.typography.labelSmall)
        TextFieldMMD(
            value = value,
            onValueChange = { v -> onValue(v.filter { it.isDigit() }.take(5)) },
            singleLine = true,
            placeholder = { TextMMD(text = "0", style = MaterialTheme.typography.bodyMedium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * MMD's month grid, full screen, with no dimmed backdrop. MMD hands its formatter UTC
 * midnight; read in the phone's zone it is the day before anywhere west of Greenwich, so it is
 * read in UTC here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerMMDState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        yearRange = 1600..2400,
    )
    val formatter = object : DatePickerFormatterMMD {
        override fun formatMonthYear(monthMillis: Long?, locale: Locale): String? =
            monthMillis?.let { DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }

        override fun formatDate(dateMillis: Long?, locale: Locale, forContentDescription: Boolean): String? =
            dateMillis?.let { long(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val view = LocalView.current
        SideEffect { (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f) }
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(vertical = 16.dp)) {
                DatePickerMMD(state = state, dateFormatter = formatter, title = null, headline = null, showModeToggle = false)
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    OutlinedButtonMMD(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) {
                        TextMMD(text = stringResource(R.string.cancel), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.width(12.dp))
                    ButtonMMD(onClick = {
                        state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                        onDismiss()
                    }, modifier = Modifier.weight(1f).height(48.dp)) {
                        TextMMD(text = stringResource(R.string.ok), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
