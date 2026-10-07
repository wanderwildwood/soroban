package com.wanderwildwood.soroban.graph

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** The part of the plane on screen. */
data class Viewport(val xMin: Double, val xMax: Double, val yMin: Double, val yMax: Double) {
    val width get() = xMax - xMin
    val height get() = yMax - yMin

    /** [factor] above 1 zooms out, below 1 in, about the point (cx, cy). */
    fun zoom(factor: Double, cx: Double = (xMin + xMax) / 2, cy: Double = (yMin + yMax) / 2): Viewport {
        val f = factor.coerceIn(0.02, 50.0)
        val next = Viewport(cx - (cx - xMin) * f, cx + (xMax - cx) * f, cy - (cy - yMin) * f, cy + (yMax - cy) * f)
        // Neither so close that doubles run out, nor so far that nothing is left to see.
        return if (next.width < 1e-9 || next.width > 1e12 || next.height < 1e-9 || next.height > 1e12) this else next
    }

    fun pan(dx: Double, dy: Double) = Viewport(xMin + dx, xMax + dx, yMin + dy, yMax + dy)

    companion object {
        /** x from −10 to 10, and y to the same scale for a screen [aspect] (height / width). */
        fun standard(aspect: Double) = Viewport(-10.0, 10.0, -10.0 * aspect, 10.0 * aspect)
    }
}

data class Pt(val x: Double, val y: Double)

/** What a point the finder found is. */
enum class Kind { ROOT, MIN, MAX, MEET }

/** A found point: on curve [curve] (0-based), or where curves [curve] and [other] meet. */
data class Found(val kind: Kind, val curve: Int, val at: Pt, val other: Int = -1)

object Plot {

    /**
     * A curve as lines to draw: f sampled at [n] + 1 evenly spaced x across [view], split into
     * separate runs wherever the function has no value or jumps across a gap. A jump counts as
     * a gap (tan at 90°, 1/x at 0) when the step is taller than the screen and the value
     * halfway along does not lie between its ends, so a steep but continuous curve, e⁻¹⁰ˣ or
     * x¹⁰, stays joined while an asymptote is never bridged by a vertical line.
     */
    fun sample(f: (Double) -> Double, view: Viewport, n: Int): List<List<Pt>> {
        val runs = mutableListOf<List<Pt>>()
        var run = mutableListOf<Pt>()
        val dx = view.width / n
        var prev: Pt? = null
        for (i in 0..n) {
            val x = view.xMin + i * dx
            val y = f(x)
            if (!y.isFinite()) {
                if (run.size > 1) runs += run
                run = mutableListOf()
                prev = null
                continue
            }
            val p = Pt(x, y)
            val last = prev
            if (last != null && isGap(f, last, p, view)) {
                if (run.size > 1) runs += run
                run = mutableListOf()
            }
            run += p
            prev = p
        }
        if (run.size > 1) runs += run
        return runs
    }

    private fun isGap(f: (Double) -> Double, a: Pt, b: Pt, view: Viewport): Boolean {
        if (abs(b.y - a.y) < view.height) return false
        // Look closer, a few times: a real curve's middle lies between its ends at every scale.
        var l = a
        var r = b
        repeat(6) {
            val mx = (l.x + r.x) / 2
            val my = f(mx)
            if (!my.isFinite()) return true
            val lo = min(l.y, r.y)
            val hi = max(l.y, r.y)
            if (my < lo - 1e-9 * (abs(lo) + 1) || my > hi + 1e-9 * (abs(hi) + 1)) return true
            // Keep the half with the bigger jump.
            val m = Pt(mx, my)
            if (abs(my - l.y) > abs(r.y - my)) r = m else l = m
        }
        return false
    }

    /**
     * A tidy spacing for grid lines: 1, 2 or 5 times a power of ten, giving about [target]
     * lines across [span].
     */
    fun niceStep(span: Double, target: Int = 8): Double {
        val raw = span / target
        val p = 10.0.pow(floor(log10(raw)))
        val m = raw / p
        return p * when {
            m < 1.5 -> 1.0
            m < 3.5 -> 2.0
            m < 7.5 -> 5.0
            else -> 10.0
        }
    }

    /** Grid positions across [min]..[max] at [step]. */
    fun ticks(min: Double, max: Double, step: Double): List<Double> {
        val first = kotlin.math.ceil(min / step) * step
        val out = mutableListOf<Double>()
        var t = first
        var guard = 0
        while (t <= max + step * 1e-9 && guard++ < 200) {
            out += if (abs(t) < step * 1e-9) 0.0 else t
            t += step
        }
        return out
    }

    /**
     * Roots, lowest and highest points, and where curves meet, inside [view]. Each is found from
     * a sample of the visible range and then narrowed, so a point closer to another than one
     * sample apart can be missed, and points just off screen are left out. A sign change across
     * a gap (tan at 90°) is not a root and is not reported.
     */
    fun find(curves: List<(Double) -> Double>, view: Viewport, n: Int = 400): List<Found> {
        val out = mutableListOf<Found>()
        val dx = view.width / n
        val xs = List(n + 1) { view.xMin + it * dx }
        curves.forEachIndexed { c, f ->
            val ys = xs.map(f)
            // Roots
            for (i in 0 until n) {
                val a = ys[i]
                val b = ys[i + 1]
                if (!a.isFinite() || !b.isFinite()) continue
                if (a == 0.0) {
                    out += Found(Kind.ROOT, c, Pt(xs[i], 0.0))
                } else if (a * b < 0) {
                    val r = bisect(f, xs[i], xs[i + 1])
                    if (r != null && inView(r, 0.0, view)) out += Found(Kind.ROOT, c, Pt(r, 0.0))
                }
            }
            // Turning points
            for (i in 1 until n) {
                val a = ys[i - 1]
                val m = ys[i]
                val b = ys[i + 1]
                if (!a.isFinite() || !m.isFinite() || !b.isFinite()) continue
                val isMax = m > a && m >= b
                val isMin = m < a && m <= b
                if (!isMax && !isMin) continue
                val x = golden(f, xs[i - 1], xs[i + 1], isMax)
                val y = f(x)
                if (y.isFinite() && inView(x, y, view) && abs(y - m) < view.height) {
                    out += Found(if (isMax) Kind.MAX else Kind.MIN, c, Pt(x, y))
                }
            }
        }
        // Where two curves meet: the roots of their difference.
        for (c in curves.indices) for (d in c + 1 until curves.size) {
            val f = curves[c]
            val g = curves[d]
            val diff = { x: Double -> f(x) - g(x) }
            val ys = xs.map(diff)
            for (i in 0 until n) {
                val a = ys[i]
                val b = ys[i + 1]
                if (!a.isFinite() || !b.isFinite()) continue
                if (a == 0.0 || a * b < 0) {
                    val x = if (a == 0.0) xs[i] else bisect(diff, xs[i], xs[i + 1]) ?: continue
                    val y = f(x)
                    if (y.isFinite() && inView(x, y, view)) out += Found(Kind.MEET, c, Pt(x, y), d)
                }
            }
        }
        // Narrowing stops a hair short: the lowest point of x² comes out at 1.5E-8, not 0. A
        // figure that small against the window is 0.
        val snapped = out.map { it.copy(at = Pt(snap(it.at.x, view.width), snap(it.at.y, view.height))) }
        return snapped.sortedWith(compareBy({ it.at.x }, { it.curve }))
    }

    private fun snap(v: Double, scale: Double) = if (abs(v) < scale * 1e-6) 0.0 else v

    private fun inView(x: Double, y: Double, v: Viewport) = x >= v.xMin && x <= v.xMax && y >= v.yMin && y <= v.yMax

    /**
     * The root between [a] and [b], where f changes sign, or null when the change is a jump
     * rather than a crossing: the value there does not shrink as the bracket does.
     */
    fun bisect(f: (Double) -> Double, a0: Double, b0: Double): Double? {
        var a = a0
        var b = b0
        var fa = f(a)
        val start = max(abs(fa), abs(f(b)))
        repeat(80) {
            val m = (a + b) / 2
            val fm = f(m)
            if (!fm.isFinite()) return null
            if (fm == 0.0) return m
            if (fa * fm < 0) b = m else { a = m; fa = fm }
        }
        val x = (a + b) / 2
        val fx = f(x)
        return if (fx.isFinite() && abs(fx) <= max(1e-6, start * 1e-6)) x else null
    }

    /** The x of the highest ([max]) or lowest point of f between [a] and [b]. */
    fun golden(f: (Double) -> Double, a0: Double, b0: Double, max: Boolean): Double {
        val g = (sqrt(5.0) - 1) / 2
        var a = a0
        var b = b0
        val s = if (max) -1.0 else 1.0
        var c = b - g * (b - a)
        var d = a + g * (b - a)
        repeat(60) {
            if (s * f(c) < s * f(d)) b = d else a = c
            c = b - g * (b - a)
            d = a + g * (b - a)
        }
        return (a + b) / 2
    }

    /** Rows of a table: x from [start], [rows] steps of [step]. */
    fun table(curves: List<(Double) -> Double>, start: Double, step: Double, rows: Int): List<Pair<Double, List<Double>>> =
        List(rows) { i ->
            // Multiplying rather than adding keeps 0.1 steps from drifting to 0.30000000000000004.
            val x = start + i * step
            x to curves.map { it(x) }
        }
}
