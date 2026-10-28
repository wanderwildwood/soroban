package com.wanderwildwood.soroban.ui

import android.app.Application
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.soroban.Prefs
import com.wanderwildwood.soroban.R
import com.wanderwildwood.soroban.calc.CalculatorViewModel
import com.wanderwildwood.soroban.convert.ConverterViewModel
import com.wanderwildwood.soroban.graph.GraphViewModel
import com.wanderwildwood.soroban.prog.ProgrammerViewModel

/** What is open over the three pages, if anything. */
private enum class Over { NONE, SETTINGS, HISTORY, GROUPS, FROM, TO, EDIT, POINTS, TABLE }

/**
 * The whole app: four pages under one top bar — calculate, graph, convert, dates — with the
 * settings, the tape and the unit lists opening over them. The page last open is the one the
 * app opens on.
 */
@Composable
fun SorobanApp(
    /** Whether the screen may turn sideways with the phone now: on the graph, when Settings allow it. */
    onTurn: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val prefs = Prefs.get(context)
    val calc: CalculatorViewModel = viewModel(factory = CalculatorViewModel.factory(app))
    val convert: ConverterViewModel = viewModel(factory = ConverterViewModel.factory(app))
    val graph: GraphViewModel = viewModel(factory = GraphViewModel.factory(app))
    val programmer: ProgrammerViewModel = viewModel(factory = ProgrammerViewModel.factory(app))
    var editing by rememberSaveable { mutableIntStateOf(0) }
    var page by rememberSaveable { mutableIntStateOf(prefs.page) }
    var over by rememberSaveable { mutableStateOf(Over.NONE) }
    var about by rememberSaveable { mutableStateOf(false) }
    val close = { over = Over.NONE }
    val settings by prefs.settings.collectAsStateWithLifecycle()
    val onGraph = over == Over.NONE && page == 1
    LaunchedEffect(onGraph, settings.turnWithPhone) { onTurn(onGraph && settings.turnWithPhone) }
    // Turned sideways on the graph: the plot and its controls take the screen under a bar of
    // their own, without the tabs; the arrow goes back to the calculator, upright.
    val wide = onGraph && LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
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
        Over.EDIT -> FunctionEditor(graph, editing, onBack = close)
        Over.POINTS -> PointsScreen(graph, onBack = close)
        Over.TABLE -> TableScreen(graph, prefs, onBack = close)
        Over.NONE -> if (wide) Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBarMMD(
                    title = { TextMMD(text = stringResource(R.string.tab_graph)) },
                    navigationIcon = {
                        BarButton(Icons.Back, stringResource(R.string.cd_back)) {
                            page = 0
                            prefs.page = 0
                        }
                    },
                    actions = {
                        TextMMD(
                            text = stringResource(R.string.graph_reset),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clickable { graph.reset(1.0) }
                                .wrapContentHeight(Alignment.CenterVertically)
                                .padding(horizontal = 16.dp),
                        )
                    },
                )
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                GraphScreen(
                    graph,
                    onEdit = {
                        editing = it
                        over = Over.EDIT
                    },
                    onPoints = { over = Over.POINTS },
                    onTable = { over = Over.TABLE },
                    wide = true,
                )
            }
        } else Scaffold(
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
                StoneTabs(
                    labels = listOf(R.string.tab_calculate, R.string.tab_graph, R.string.tab_convert, R.string.tab_dates).map { stringResource(it) },
                    selected = page,
                    onSelect = { i ->
                        page = i
                        prefs.page = i
                    },
                )
                Box(Modifier.weight(1f)) {
                    when (page) {
                        0 -> CalculatorScreen(calc, programmer, prefs, onHistory = { over = Over.HISTORY })
                        1 -> GraphScreen(
                            graph,
                            onEdit = {
                                editing = it
                                over = Over.EDIT
                            },
                            onPoints = { over = Over.POINTS },
                            onTable = { over = Over.TABLE },
                        )
                        2 -> ConverterScreen(
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
