package com.wanderwildwood.soroban.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.sadellie.unitto.core.common.OutputFormat
import com.wanderwildwood.soroban.NumberStyle
import com.wanderwildwood.soroban.Numbers
import com.wanderwildwood.soroban.Prefs
import com.wanderwildwood.soroban.R
import com.wanderwildwood.soroban.calc.Line

private val PLACES = listOf(0, 2, 3, 4, 6, 8, 10, 12, 15, 20)

/** How numbers are written. Four rows, and nothing else. */
@Composable
fun SettingsScreen(prefs: Prefs, onBack: () -> Unit, onAbout: () -> Unit) {
    BackHandler(onBack = onBack)
    val s by prefs.settings.collectAsStateWithLifecycle()
    var picking by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = stringResource(R.string.settings_title)) },
                navigationIcon = { BarButton(Icons.Back, stringResource(R.string.cd_back), onBack) },
                actions = { BarButton(Icons.Info, stringResource(R.string.cd_about), onAbout) },
            )
        },
    ) { padding ->
        LazyColumnMMD(Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp)) {
            item { Spacer(Modifier.height(8.dp)) }
            item(key = "places") {
                SettingRow(stringResource(R.string.settings_places), s.precision.toString()) { picking = "places" }
            }
            item(key = "style") {
                SettingRow(stringResource(R.string.settings_style), s.style.example) { picking = "style" }
            }
            item(key = "e") {
                SettingRow(
                    stringResource(R.string.settings_large),
                    stringResource(if (s.outputFormat == OutputFormat.PLAIN) R.string.settings_large_plain else R.string.settings_large_e),
                ) { picking = "e" }
            }
            item(key = "fractions") {
                SwitchRow(stringResource(R.string.settings_fractions), s.fractions) { on -> prefs.update { it.copy(fractions = on) } }
            }
        }
    }

    when (picking) {
        "places" -> OptionsDialog(
            title = stringResource(R.string.settings_places),
            options = PLACES.map { it to it.toString() },
            chosen = s.precision,
            onPick = { n -> prefs.update { it.copy(precision = n) } },
            onDismiss = { picking = null },
        )
        "style" -> OptionsDialog(
            title = stringResource(R.string.settings_style),
            options = NumberStyle.entries.map { it to it.example },
            chosen = s.style,
            onPick = { style -> prefs.update { it.copy(style = style) } },
            onDismiss = { picking = null },
        )
        "e" -> OptionsDialog(
            title = stringResource(R.string.settings_large),
            options = listOf(
                OutputFormat.PLAIN to stringResource(R.string.settings_large_plain),
                OutputFormat.ALLOW_ENGINEERING to stringResource(R.string.settings_large_e),
            ),
            chosen = s.outputFormat,
            onPick = { f -> prefs.update { it.copy(outputFormat = f) } },
            onDismiss = { picking = null },
        )
    }
}

/**
 * The tape: every sum worked out, newest first. Pressing one puts its answer back in the
 * calculator to carry on from. Clearing it is the last row, and asks in its own face.
 */
@Composable
fun HistoryScreen(
    lines: List<Line>,
    prefs: Prefs,
    onUse: (Line) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val s by prefs.settings.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = stringResource(R.string.history_title)) },
                navigationIcon = { BarButton(Icons.Back, stringResource(R.string.cd_back), onBack) },
            )
        },
    ) { padding ->
        LazyColumnMMD(Modifier.padding(padding).fillMaxSize()) {
            if (lines.isEmpty()) {
                item(key = "empty") {
                    TextMMD(
                        text = stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
            lines.forEachIndexed { i, line ->
                item(key = "line$i") {
                    Column(Modifier.fillMaxWidth().clickable { onUse(line) }) {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                            TextMMD(text = Numbers.show(line.expression, s.symbols), style = MaterialTheme.typography.bodySmall)
                            TextMMD(text = "= " + Numbers.show(line.result, s.symbols), style = MaterialTheme.typography.bodyMedium)
                        }
                        HorizontalDividerMMD()
                    }
                }
            }
            if (lines.isNotEmpty()) {
                item(key = "clear") {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        ArmedRow(
                            label = pluralStringResource(R.plurals.history_clear, lines.size, lines.size.toString()),
                            armedLabel = stringResource(R.string.history_clear_armed),
                            onConfirmed = onClear,
                        )
                    }
                }
            }
        }
    }
}
