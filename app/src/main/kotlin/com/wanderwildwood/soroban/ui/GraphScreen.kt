package com.wanderwildwood.soroban.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.Token
import com.wanderwildwood.soroban.Numbers
import com.wanderwildwood.soroban.R
import com.wanderwildwood.soroban.graph.GraphViewModel
import com.wanderwildwood.soroban.graph.Picked
import com.wanderwildwood.soroban.graph.Plot
import com.wanderwildwood.soroban.graph.Viewport
import io.github.sadellie.evaluatto.PlotExpression
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs
import kotlin.math.hypot

/** The three ways a curve is drawn, told apart by line rather than colour. */
private val STYLES = listOf<PathEffect?>(
    null,
    PathEffect.dashPathEffect(floatArrayOf(18f, 10f)),
    PathEffect.dashPathEffect(floatArrayOf(2f, 9f)),
)

/** y₁, y₂, y₃. */
fun curveName(i: Int) = "y" + "₁₂₃"[i]

/** A sum with x in it, as written on the screen. */
fun showFunction(source: String, symbols: FormatterSymbols) = Numbers.show(source, symbols).replace(PlotExpression.X, "x")

/**
 * A number for a label or a readout: six figures at most, written as the numbers are set to be.
 * Very large and very small ones use E.
 */
fun fmt(d: Double, symbols: FormatterSymbols): String {
    if (!d.isFinite()) return "—"
    if (d == 0.0) return "0"
    val a = abs(d)
    val text = if (a >= 1e7 || a < 1e-4) {
        String.format(java.util.Locale.ROOT, "%.3E", d).replace("E+0", "E").replace("E-0", "E-").replace("E+", "E")
    } else {
        BigDecimal(d).round(MathContext(6)).stripTrailingZeros().toPlainString()
    }
    return Numbers.show(text.replace("-", Token.Operator.MINUS), symbols)
}

/**
 * The graph: the functions over the plot, a line for the point picked, and a row of controls.
 * Dragging moves the window and pinching zooms it; the plot is drawn again once, when the
 * fingers lift, rather than following them, which the panel could only smear.
 */
@Composable
fun GraphScreen(
    vm: GraphViewModel,
    onEdit: (Int) -> Unit,
    onPoints: () -> Unit,
    onTable: () -> Unit,
) {
    val functions by vm.functions.collectAsStateWithLifecycle()
    val view by vm.view.collectAsStateWithLifecycle()
    val picked by vm.picked.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var size by remember { mutableStateOf(IntSize.Zero) }
    val curves = remember(functions, settings.radians) { vm.curves() }

    Column(Modifier.fillMaxSize()) {
        functions.forEachIndexed { i, f ->
            FunctionRow(i, showFunction(f, settings.symbols), broken = curves.getOrNull(i) == null) { onEdit(i) }
        }
        if (functions.size < GraphViewModel.MAX) {
            Row(
                Modifier.fillMaxWidth().height(36.dp).clickable { onEdit(functions.size) }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextMMD(text = stringResource(R.string.graph_add), style = MaterialTheme.typography.bodySmall)
            }
        }
        HorizontalDividerMMD()
        Box(Modifier.fillMaxWidth().height(26.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            TextMMD(
                text = picked?.let { readout(it, settings.symbols) } ?: stringResource(if (functions.isEmpty()) R.string.graph_empty else R.string.graph_hint),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier.fillMaxWidth().weight(1f).onSizeChanged {
                size = it
                if (it.width > 0) vm.ensureView(it.height.toDouble() / it.width)
            },
        ) {
            val v = view
            if (v != null && size.width > 0) {
                PlotCanvas(v, curves, picked, settings.symbols, onView = vm::setView, onPick = vm::pick)
            }
        }
        HorizontalDividerMMD()
        Keypad(
            listOf(
                listOf(
                    Key("−", { vm.zoom(2.0) }, description = stringResource(R.string.cd_zoom_out)),
                    Key("+", { vm.zoom(0.5) }, description = stringResource(R.string.cd_zoom_in)),
                    Key(stringResource(R.string.graph_reset), { if (size.width > 0) vm.reset(size.height.toDouble() / size.width) }, small = true),
                    Key(stringResource(R.string.graph_points), onPoints, small = true, enabled = functions.isNotEmpty()),
                    Key(stringResource(R.string.graph_table), onTable, small = true, enabled = functions.isNotEmpty()),
                ),
            ),
            Modifier.height(52.dp),
        )
    }
}

@Composable
private fun FunctionRow(i: Int, text: String, broken: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(36.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.width(36.dp).height(12.dp)) {
            drawLine(Color.Black, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 3f, pathEffect = STYLES[i], cap = if (i == 2) StrokeCap.Round else StrokeCap.Butt)
        }
        Spacer(Modifier.width(10.dp))
        TextMMD(
            text = "${curveName(i)} = $text" + if (broken) "  " + "(?)" else "",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun readout(p: Picked, symbols: FormatterSymbols): String {
    val x = fmt(p.at.x, symbols)
    val y = fmt(p.at.y, symbols)
    val name = curveName(p.curve)
    return when (p.kind) {
        com.wanderwildwood.soroban.graph.Kind.ROOT -> stringResource(R.string.found_root, name, x)
        com.wanderwildwood.soroban.graph.Kind.MIN -> stringResource(R.string.found_min, name, x, y)
        com.wanderwildwood.soroban.graph.Kind.MAX -> stringResource(R.string.found_max, name, x, y)
        com.wanderwildwood.soroban.graph.Kind.MEET -> stringResource(R.string.found_meet, name, curveName(p.other), x, y)
        null -> stringResource(R.string.graph_point, name, x, y)
    }
}

@Composable
private fun PlotCanvas(
    view: Viewport,
    curves: List<((Double) -> Double)?>,
    picked: Picked?,
    symbols: FormatterSymbols,
    onView: (Viewport) -> Unit,
    onPick: (Picked?) -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = Color.Black)
    val slop = LocalViewConfiguration.current.touchSlop
    val runs = remember(view, curves) {
        curves.map { f -> f?.let { Plot.sample(it, view, 480) }.orEmpty() }
    }

    Canvas(
        Modifier.fillMaxSize().clipToBounds().pointerInput(view, curves) {
            awaitEachGesture {
                val first = awaitFirstDown()
                val start = first.position
                var pan = Offset.Zero
                var zoom = 1f
                var centre = start
                var moved = false
                var lastSpread = 0f
                var lastCentroid = start
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.isEmpty()) break
                    val c = pressed.fold(Offset.Zero) { acc, ch -> acc + ch.position } / pressed.size.toFloat()
                    val spread = if (pressed.size > 1) pressed.map { (it.position - c).getDistance() }.average().toFloat() else 0f
                    if (pressed.size > 1 && lastSpread > 0f && spread > 0f) zoom *= spread / lastSpread
                    if (pressed.size == event.changes.size) pan += c - lastCentroid
                    if (pressed.size > 1) centre = c
                    lastSpread = if (pressed.size > 1) spread else 0f
                    lastCentroid = c
                    if (hypot(pan.x, pan.y) > slop || zoom !in 0.95f..1.05f) moved = true
                    event.changes.forEach { it.consume() }
                }
                val w = size.width.toDouble()
                val h = size.height.toDouble()
                if (!moved) {
                    // A tap: the nearest curve at that x, if the tap was near enough to it.
                    val x = view.xMin + start.x / w * view.width
                    var best: Picked? = null
                    var bestDist = 56.0
                    curves.forEachIndexed { i, f ->
                        val y = f?.invoke(x) ?: return@forEachIndexed
                        if (!y.isFinite()) return@forEachIndexed
                        val py = (view.yMax - y) / view.height * h
                        val d = abs(py - start.y)
                        if (d < bestDist) {
                            bestDist = d
                            best = Picked(i, com.wanderwildwood.soroban.graph.Pt(x, y))
                        }
                    }
                    onPick(best)
                } else {
                    var next = view.pan(-pan.x / w * view.width, pan.y / h * view.height)
                    if (zoom != 1f) {
                        val cx = next.xMin + centre.x / w * next.width
                        val cy = next.yMax - centre.y / h * next.height
                        next = next.zoom(1.0 / zoom, cx, cy)
                    }
                    onView(next)
                }
            }
        },
    ) {
        drawGrid(view, measurer, labelStyle, symbols)
        runs.forEachIndexed { i, curve ->
            curve.forEach { run ->
                val path = Path()
                run.forEachIndexed { k, p ->
                    val o = toScreen(view, p.x, p.y)
                    // Far off the panel, a point is clamped, so the path stays drawable.
                    val y = o.y.coerceIn(-size.height * 4f, size.height * 5f)
                    if (k == 0) path.moveTo(o.x, y) else path.lineTo(o.x, y)
                }
                drawPath(path, Color.Black, style = Stroke(width = 3f, cap = if (i == 2) StrokeCap.Round else StrokeCap.Butt, pathEffect = STYLES[i]))
            }
        }
        picked?.let { p ->
            val o = toScreen(view, p.at.x, p.at.y)
            drawCircle(Color.White, 10f, o)
            drawCircle(Color.Black, 10f, o, style = Stroke(3f))
        }
    }
}

private fun DrawScope.toScreen(v: Viewport, x: Double, y: Double) =
    Offset(((x - v.xMin) / v.width * size.width).toFloat(), ((v.yMax - y) / v.height * size.height).toFloat())

/**
 * A grid of dotted lines a round number apart, solid axes, and the numbers along them. Where an
 * axis is off the panel its numbers stay at the nearest edge.
 */
private fun DrawScope.drawGrid(v: Viewport, measurer: androidx.compose.ui.text.TextMeasurer, style: TextStyle, symbols: FormatterSymbols) {
    val dotted = PathEffect.dashPathEffect(floatArrayOf(2f, 8f))
    val step = Plot.niceStep(v.width, 6)
    val stepY = Plot.niceStep(v.height, (6 * v.height / v.width).toInt().coerceAtLeast(3))
    val xs = Plot.ticks(v.xMin, v.xMax, step)
    val ys = Plot.ticks(v.yMin, v.yMax, stepY)
    xs.forEach { x ->
        val px = toScreen(v, x, 0.0).x
        drawLine(Color.Black, Offset(px, 0f), Offset(px, size.height), 1f, pathEffect = dotted)
    }
    ys.forEach { y ->
        val py = toScreen(v, 0.0, y).y
        drawLine(Color.Black, Offset(0f, py), Offset(size.width, py), 1f, pathEffect = dotted)
    }
    val origin = toScreen(v, 0.0, 0.0)
    val axisY = origin.y.coerceIn(0f, size.height)
    val axisX = origin.x.coerceIn(0f, size.width)
    if (origin.y in 0f..size.height) drawLine(Color.Black, Offset(0f, origin.y), Offset(size.width, origin.y), 3f)
    if (origin.x in 0f..size.width) drawLine(Color.Black, Offset(origin.x, 0f), Offset(origin.x, size.height), 3f)
    xs.forEach { x ->
        if (x == 0.0) return@forEach
        val layout = measurer.measure(fmt(x, symbols), style)
        val px = toScreen(v, x, 0.0).x - layout.size.width / 2
        val py = (axisY + 4f).coerceAtMost(size.height - layout.size.height - 2f)
        if (px > 0 && px + layout.size.width < size.width) drawLabel(layout, Offset(px, py))
    }
    ys.forEach { y ->
        if (y == 0.0) return@forEach
        val layout = measurer.measure(fmt(y, symbols), style)
        val px = (axisX + 6f).coerceAtMost(size.width - layout.size.width - 2f)
        val py = toScreen(v, 0.0, y).y - layout.size.height / 2
        if (py > 0 && py + layout.size.height < size.height) drawLabel(layout, Offset(px, py))
    }
}

/** A label on a white patch, so a grid line through it does not cut the figures. */
private fun DrawScope.drawLabel(layout: androidx.compose.ui.text.TextLayoutResult, at: Offset) {
    drawRect(Color.White, at, androidx.compose.ui.geometry.Size(layout.size.width.toFloat(), layout.size.height.toFloat()))
    drawText(layout, topLeft = at)
}
