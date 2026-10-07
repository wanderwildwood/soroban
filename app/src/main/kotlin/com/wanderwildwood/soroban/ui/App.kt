package com.wanderwildwood.soroban.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mudita.mmd.components.tabs.PrimaryTabRowMMD
import com.mudita.mmd.components.tabs.TabMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.soroban.Prefs
import com.wanderwildwood.soroban.R
import com.wanderwildwood.soroban.calc.CalculatorViewModel
import com.wanderwildwood.soroban.convert.ConverterViewModel

/** What is open over the three pages, if anything. */
private enum class Over { NONE, SETTINGS, HISTORY, GROUPS, FROM, TO }

/**
 * The whole app: three pages under one top bar — calculate, convert, dates — with the
 * settings, the tape and the unit lists opening over them. The page last open is the one the
 * app opens on.
 */
@Composable
fun SorobanApp() {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val prefs = Prefs.get(context)
    val calc: CalculatorViewModel = viewModel(factory = CalculatorViewModel.factory(app))
    val convert: ConverterViewModel = viewModel(factory = ConverterViewModel.factory(app))
    var page by rememberSaveable { mutableIntStateOf(prefs.page) }
    var over by rememberSaveable { mutableStateOf(Over.NONE) }
    var about by rememberSaveable { mutableStateOf(false) }
    val close = { over = Over.NONE }
    val onAbout = { about = true }

    when (over) {
        Over.SETTINGS -> SettingsScreen(prefs, onBack = close, onAbout = onAbout)
        Over.HISTORY -> {
            val lines by calc.lines.collectAsStateWithLifecycle()
            HistoryScreen(
                lines = lines,
                prefs = prefs,
                onUse = {
                    calc.replaceInput(it.result)
                    close()
                },
                onClear = { calc.clearHistory() },
                onBack = close,
            )
        }
        Over.GROUPS -> {
            val group by convert.group.collectAsStateWithLifecycle()
            GroupPicker(current = group, onPick = {
                convert.selectGroup(it)
                close()
            }, onBack = close)
        }
        Over.FROM -> UnitPicker(convert, choosingTo = false, onBack = close)
        Over.TO -> UnitPicker(convert, choosingTo = true, onBack = close)
        Over.NONE -> Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBarMMD(
                    title = { TextMMD(text = stringResource(R.string.app_name)) },
                    actions = {
                        BarButton(Icons.Settings, stringResource(R.string.cd_settings)) { over = Over.SETTINGS }
                        BarButton(Icons.Info, stringResource(R.string.cd_about), onAbout)
                    },
                )
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                PrimaryTabRowMMD(selectedTabIndex = page) {
                    listOf(R.string.tab_calculate, R.string.tab_convert, R.string.tab_dates).forEachIndexed { i, label ->
                        TabMMD(
                            selected = page == i,
                            onClick = {
                                page = i
                                prefs.page = i
                            },
                            text = {
                                TextMMD(
                                    text = stringResource(label),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (page == i) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }
                Box(Modifier.weight(1f)) {
                    when (page) {
                        0 -> CalculatorScreen(calc, onHistory = { over = Over.HISTORY })
                        1 -> ConverterScreen(
                            convert,
                            onGroups = { over = Over.GROUPS },
                            onPickFrom = { over = Over.FROM },
                            onPickTo = { over = Over.TO },
                        )
                        else -> DatesScreen()
                    }
                }
            }
        }
    }

    if (about) AboutDialog(onDismiss = { about = false })
}
