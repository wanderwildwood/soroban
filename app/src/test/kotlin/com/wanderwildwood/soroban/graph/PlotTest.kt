package com.wanderwildwood.soroban.graph

import io.github.sadellie.evaluatto.PlotExpression
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlotTest {

    private fun fn(src: String, radians: Boolean = true): (Double) -> Double {
        val e = PlotExpression(src, radians)
        return { x -> e.at(x) }
    }

    private val view = Viewport(-10.0, 10.0, -10.0, 10.0)

    /** No run may step from one side of [x0] to the other. */
    private fun assertNoJoinAcross(runs: List<List<Pt>>, x0: Double) {
        for (run in runs) for ((a, b) in run.zipWithNext()) {
            assertTrue("joined across $x0 between ${a.x} and ${b.x}", !(a.x < x0 && b.x > x0))
        }
    }

    @Test
    fun oneOverXIsTwoPiecesNotJoinedAtZero() {
        // 401 samples put a point exactly at 0, where 1/x has no value.
        val runs = Plot.sample(fn("1÷X"), view, 400)
        assertEquals(2, runs.size)
        assertNoJoinAcross(runs, 0.0)
        // An even count straddles 0 without landing on it: still no join.
        val straddled = Plot.sample(fn("1÷X"), view, 399)
        assertNoJoinAcross(straddled, 0.0)
    }

    @Test
    fun tanIsBrokenAtEveryAsymptote() {
        val runs = Plot.sample(fn("tan(X)"), view, 480)
        for (k in -3..3) assertNoJoinAcross(runs, PI / 2 + k * PI)
        assertEquals(7, runs.size)
        val deg = Plot.sample(fn("tan(X)", radians = false), Viewport(-360.0, 360.0, -10.0, 10.0), 480)
        for (k in listOf(-270.0, -90.0, 90.0, 270.0)) assertNoJoinAcross(deg, k)
    }

    @Test
    fun aSteepButContinuousCurveStaysWhole() {
        assertEquals(1, Plot.sample(fn("X^9"), view, 200).size)
        assertEquals(1, Plot.sample(fn("exp(X)"), view, 200).size)
        assertEquals(1, Plot.sample(fn("X^3−X"), view, 50).size)
    }

    @Test
    fun undefinedRegionsAreLeftOut() {
        val runs = Plot.sample(fn("√(X)"), view, 200)
        assertEquals(1, runs.size)
        assertTrue(runs[0].all { it.x >= 0 })
        val ln = Plot.sample(fn("ln(X)"), view, 200)
        assertTrue(ln.flatten().all { it.x > 0 })
    }

    @Test
    fun rootsOfAParabola() {
        val roots = Plot.find(listOf(fn("X^2−2")), view).filter { it.kind == Kind.ROOT }
        assertEquals(2, roots.size)
        assertEquals(-sqrt(2.0), roots[0].at.x, 1e-9)
        assertEquals(sqrt(2.0), roots[1].at.x, 1e-9)
    }

    @Test
    fun tanHasRootsButNoneAtItsAsymptotes() {
        val roots = Plot.find(listOf(fn("tan(X)")), view).filter { it.kind == Kind.ROOT }
        // 0, ±π, ±2π, ±3π are in −10..10; ±π/2 and the rest are gaps, not roots.
        assertEquals(7, roots.size)
        roots.forEach { r -> assertTrue("${r.at.x}", abs(Math.IEEEremainder(r.at.x, PI)) < 1e-6) }
    }

    @Test
    fun turningPoints() {
        val found = Plot.find(listOf(fn("X^3−3X")), view)
        val max = found.single { it.kind == Kind.MAX }
        val min = found.single { it.kind == Kind.MIN }
        assertEquals(-1.0, max.at.x, 1e-5)
        assertEquals(2.0, max.at.y, 1e-9)
        assertEquals(1.0, min.at.x, 1e-5)
        assertEquals(-2.0, min.at.y, 1e-9)
    }

    @Test
    fun whereTwoCurvesMeet() {
        val meets = Plot.find(listOf(fn("X"), fn("X^2−2")), view).filter { it.kind == Kind.MEET }
        assertEquals(listOf(-1.0, 2.0), meets.map { Math.round(it.at.x * 1e6) / 1e6 })
        assertTrue(meets.all { it.curve == 0 && it.other == 1 })
    }

    @Test
    fun pointsOffScreenAreLeftOut() {
        val roots = Plot.find(listOf(fn("X−20")), view).filter { it.kind == Kind.ROOT }
        assertTrue(roots.isEmpty())
    }

    @Test
    fun tableStepsDoNotDrift() {
        val rows = Plot.table(listOf(fn("X")), 0.0, 0.1, 31)
        assertEquals(3.0, rows.last().first, 1e-12)
        assertEquals(0.3, rows[3].first, 1e-15)
    }

    @Test
    fun niceSteps() {
        assertEquals(2.0, Plot.niceStep(20.0, 8), 0.0)
        assertEquals(0.5, Plot.niceStep(3.0, 6), 0.0)
        assertEquals(50.0, Plot.niceStep(400.0, 8), 0.0)
    }

    @Test
    fun zoomKeepsTheCentre() {
        val v = Viewport(-10.0, 10.0, -5.0, 5.0).zoom(0.5)
        assertEquals(Viewport(-5.0, 5.0, -2.5, 2.5), v)
    }
}
