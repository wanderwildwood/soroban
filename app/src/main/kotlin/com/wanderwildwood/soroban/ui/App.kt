package com.wanderwildwood.soroban.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.divider.HorizontalDividerMMD
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
fun SorobanApp() {
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
                Tabs(
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

/**
 * The pages, in a row of equal parts, the one open in bold over a solid bar. MMD's own tab row
 * draws the same, but pads each label by 16dp a side, and at four tabs on a 480px panel that
 * broke "Calculate" over two lines.
 */
@Composable
private fun Tabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(48.dp)) {
            labels.forEachIndexed { i, label ->
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    TextMMD(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false,
                    )
                    if (i == selected) {
                        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 10.dp).height(3.dp).background(Color.Black))
                    }
                }
            }
        }
        HorizontalDividerMMD()
    }
}
