package com.wanderwildwood.soroban.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.ui.textfield.addTokens
import com.sadellie.unitto.core.ui.textfield.deleteTokens
import com.wanderwildwood.soroban.Numbers
import com.wanderwildwood.soroban.Prefs
import com.wanderwildwood.soroban.R
import com.wanderwildwood.soroban.graph.GraphViewModel
import com.wanderwildwood.soroban.graph.Kind
import com.wanderwildwood.soroban.graph.Picked
import com.wanderwildwood.soroban.graph.Plot
import io.github.sadellie.evaluatto.PlotExpression

/**
 * Writing one of the functions: the calculator's keys with an x among them, the functions a
 * page away behind fx as on the calculator. Done draws it; a function cleared to nothing is
 * taken off the graph.
 */
@Composable
fun FunctionEditor(vm: GraphViewModel, index: Int, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val initial = vm.functions.value.getOrNull(index).orEmpty()
    val state = remember(index) { TextFieldState(initial) }
    var functions by rememberSaveable { mutableStateOf(false) }
    val add: (String) -> Unit = { state.addTokens(it) }
    val insert: (String) -> Unit = {
        state.addTokens(it)
        functions = false
    }
    val done = {
        if (vm.save(index, state.text.toString())) onBack()
        else Toast.makeText(context, context.getString(R.string.graph_not_a_function), Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = curveName(index.coerceAtMost(2)) + " =") },
                navigationIcon = { BarButton(Icons.Back, stringResource(R.string.cd_back), onBack) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            val text = showFunction(state.text.toString(), settings.symbols)
            Box(Modifier.fillMaxWidth().height(110.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterEnd) {
                TextMMD(
                    text = text.ifEmpty { stringResource(R.string.graph_write_hint) },
                    fontSize = if (text.length > 14) 28.sp else 38.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            HorizontalDividerMMD()
            val backspace = stringResource(R.string.cd_backspace)
            val clear = stringResource(R.string.key_clear)
            val keys = if (!functions) {
                fun d(t: String) = Key(t, { add(t) })
                listOf(
                    listOf(Key(clear, { state.clearText() }, small = true), Key("(", { add("(") }), Key(")", { add(")") }), Key("", { state.deleteTokens() }, icon = Icons.Backspace, description = backspace)),
                    listOf(Key("x", { add(PlotExpression.X) }, bold = true), Key(Token.Operator.SQRT, { add(Token.Operator.SQRT) }), Key("xʸ", { add(Token.Operator.POWER) }, small = true), d(Token.Operator.DIVIDE)),
                    listOf(d("7"), d("8"), d("9"), d(Token.Operator.MULTIPLY)),
                    listOf(d("4"), d("5"), d("6"), d(Token.Operator.MINUS)),
                    listOf(d("1"), d("2"), d("3"), d(Token.Operator.PLUS)),
                    listOf(
                        Key(stringResource(R.string.key_functions), { functions = true }, small = true),
                        d("0"),
                        Key(settings.symbols.fractional, { add(Token.Digit.DOT) }),
                        Key(stringResource(R.string.graph_done), done, filled = true, small = true),
                    ),
                )
            } else {
                val f = Token.Func
                listOf(
                    listOf(Key(f.SIN, { insert(f.SIN_BRACKET) }, small = true), Key(f.COS, { insert(f.COS_BRACKET) }, small = true), Key(f.TAN, { insert(f.TAN_BRACKET) }, small = true), Key("x", { insert(PlotExpression.X) }, bold = true)),
                    listOf(Key(f.ARSIN, { insert(f.ARSIN_BRACKET) }, small = true), Key(f.ARCOS, { insert(f.ARCOS_BRACKET) }, small = true), Key(f.ACTAN, { insert(f.ACTAN_BRACKET) }, small = true), Key("x!", { insert(Token.Operator.FACTORIAL) }, small = true)),
                    listOf(Key(f.LN, { insert(f.LN_BRACKET) }, small = true), Key(f.LOG, { insert(f.LOG_BRACKET) }, small = true), Key("eˣ", { insert(f.EXP_BRACKET) }, small = true), Key("10ˣ", { insert("10" + Token.Operator.POWER) }, small = true)),
                    listOf(Key(Token.Const.PI, { insert(Token.Const.PI) }), Key(Token.Const.E, { insert(Token.Const.E) }), Key("x²", { insert(Token.Operator.POWER + "2") }, small = true), Key("mod", { insert(Token.Operator.MODULO) }, small = true)),
                    listOf(Key(clear, { state.clearText() }, small = true), Key("(", { insert("(") }), Key(")", { insert(")") }), Key("", { state.deleteTokens() }, icon = Icons.Backspace, description = backspace)),
                    listOf(
                        Key(stringResource(R.string.key_numbers), { functions = false }, small = true),
                        Key(Token.Operator.SQRT, { insert(Token.Operator.SQRT) }),
                        Key("xʸ", { insert(Token.Operator.POWER) }, small = true),
                        Key(stringResource(R.string.graph_done), done, filled = true, small = true),
                    ),
                )
            }
            Keypad(keys, Modifier.weight(1f))
        }
    }
}

/**
 * The roots, highest and lowest points, and meetings of the curves in the window, from left
 * to right. Pressing one marks it on the graph.
 */
@Composable
fun PointsScreen(vm: GraphViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val found by vm.found.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (vm.found.value == null) vm.findPoints() }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = stringResource(R.string.points_title)) },
                navigationIcon = { BarButton(Icons.Back, stringResource(R.string.cd_back), onBack) },
            )
        },
    ) { padding ->
        LazyColumnMMD(Modifier.padding(padding).fillMaxSize()) {
            val list = found
            item(key = "note") {
                TextMMD(
                    text = stringResource(
                        when {
                            list == null -> R.string.points_looking
                            list.isEmpty() -> R.string.points_none
                            else -> R.string.points_note
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
            list?.forEachIndexed { i, f ->
                item(key = "p$i") {
                    val x = fmt(f.at.x, settings.symbols)
                    val y = fmt(f.at.y, settings.symbols)
                    val name = curveName(f.curve)
                    val (title, detail) = when (f.kind) {
                        Kind.ROOT -> stringResource(R.string.points_root, name) to "x = $x"
                        Kind.MIN -> stringResource(R.string.points_min, name) to "x = $x,  y = $y"
                        Kind.MAX -> stringResource(R.string.points_max, name) to "x = $x,  y = $y"
                        Kind.MEET -> stringResource(R.string.points_meet, name, curveName(f.other)) to "x = $x,  y = $y"
                    }
                    Column(
                        Modifier.fillMaxWidth().clickable {
                            vm.pick(Picked(f.curve, f.at, f.kind, f.other))
                            onBack()
                        },
                    ) {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                            TextMMD(text = title, style = MaterialTheme.typography.bodySmall)
                            TextMMD(text = detail, style = MaterialTheme.typography.bodyMedium)
                        }
                        HorizontalDividerMMD()
                    }
                }
            }
        }
    }
}

/**
 * A table of values: x from a start, a step at a time, and each function's y. The start and
 * step are typed, and kept.
 */
@Composable
fun TableScreen(vm: GraphViewModel, prefs: Prefs, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val settings by vm.settings.collectAsStateWithLifecycle()
    val functions by vm.functions.collectAsStateWithLifecycle()
    var start by rememberSaveable { mutableStateOf(prefs.tableStart) }
    var step by rememberSaveable { mutableStateOf(prefs.tableStep) }
    val curves = remember(functions, settings.radians) { vm.curves() }
    val s = settings.symbols
    val startValue = Numbers.parse(start, s)?.let { runCatching { it.replace(Token.Operator.MINUS, "-").toDouble() }.getOrNull() }
    val stepValue = Numbers.parse(step, s)?.let { runCatching { it.toDouble() }.getOrNull() }?.takeIf { it > 0 }
    val rows = remember(startValue, stepValue, curves) {
        if (startValue == null || stepValue == null) emptyList()
        else Plot.table(curves.map { it ?: { Double.NaN } }, startValue, stepValue, ROWS)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = stringResource(R.string.table_title)) },
                navigationIcon = { BarButton(Icons.Back, stringResource(R.string.cd_back), onBack) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                NumberField(stringResource(R.string.table_start), start, Modifier.weight(1f)) {
                    start = it
                    prefs.tableStart = it
                }
                Spacer(Modifier.width(12.dp))
                NumberField(stringResource(R.string.table_step), step, Modifier.weight(1f)) {
                    step = it
                    prefs.tableStep = it
                }
            }
            // Its end kept clear of the list's rail below, so the headings sit over their columns.
            Box(Modifier.padding(end = 30.dp)) { TableRow(listOf("x") + functions.indices.map { curveName(it) }, bold = true) }
            HorizontalDividerMMD()
            if (rows.isEmpty()) {
                TextMMD(
                    text = stringResource(R.string.table_bad_step),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(16.dp),
                )
            }
            LazyColumnMMD(Modifier.fillMaxSize()) {
                rows.forEachIndexed { i, (x, ys) ->
                    item(key = "r$i") {
                        TableRow(listOf(fmt(x, s)) + ys.map { fmt(it, s) }, bold = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, bold: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        cells.forEach {
            TextMMD(
                text = it,
                // The type scale's smallest, so four columns of six figures fit across.
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f).padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, onValue: (String) -> Unit) {
    Column(modifier) {
        TextMMD(text = label, style = MaterialTheme.typography.labelSmall)
        TextFieldMMD(
            value = value,
            onValueChange = { v -> onValue(v.take(16)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val ROWS = 50
