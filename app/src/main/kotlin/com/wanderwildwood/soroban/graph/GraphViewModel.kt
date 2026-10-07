package com.wanderwildwood.soroban.graph

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wanderwildwood.soroban.Prefs
import io.github.sadellie.evaluatto.PlotExpression
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A point picked on the graph, by a tap or from the list of found points. */
data class Picked(val curve: Int, val at: Pt, val kind: Kind? = null, val other: Int = -1)

/**
 * The graph: up to three functions of x, the window onto them, and a point picked. The
 * functions and the window are kept between visits.
 */
class GraphViewModel(private val prefs: Prefs) : ViewModel() {

    private val _functions = MutableStateFlow(prefs.functions.take(MAX))
    /** Each function as calculator tokens, x written as [PlotExpression.X]. */
    val functions: StateFlow<List<String>> = _functions.asStateFlow()

    private val _view = MutableStateFlow(prefs.viewport?.let { Viewport(it[0], it[1], it[2], it[3]) })
    /** Null until the graph has been laid out once and knows its shape. */
    val view: StateFlow<Viewport?> = _view.asStateFlow()

    private val _picked = MutableStateFlow<Picked?>(null)
    val picked: StateFlow<Picked?> = _picked.asStateFlow()

    private val _found = MutableStateFlow<List<Found>?>(null)
    /** The roots, turning points and meetings in view, once asked for. */
    val found: StateFlow<List<Found>?> = _found.asStateFlow()

    val settings = prefs.settings

    /** The functions, ready to evaluate in the current angle unit; a broken one is null. */
    fun curves(): List<((Double) -> Double)?> {
        val radians = prefs.settings.value.radians
        return _functions.value.map { src ->
            runCatching { PlotExpression(src, radians).also { it.check() } }.getOrNull()?.let { e -> { x: Double -> runCatching { e.at(x) }.getOrDefault(Double.NaN) } }
        }
    }

    /** Sets the window once the graph knows its shape, unless one was kept from before. */
    fun ensureView(aspect: Double) {
        if (_view.value == null) setView(Viewport.standard(aspect))
    }

    fun setView(v: Viewport) {
        _view.value = v
        _found.value = null
        prefs.viewport = listOf(v.xMin, v.xMax, v.yMin, v.yMax)
    }

    fun zoom(factor: Double) = _view.value?.let { setView(it.zoom(factor)) }

    fun reset(aspect: Double) {
        _picked.value = null
        setView(Viewport.standard(aspect))
    }

    /**
     * Saves function [index] (or a new one when it is [functions]' size). An empty one is
     * removed. Returns false, saving nothing, when the sum cannot be read as a function.
     */
    fun save(index: Int, source: String): Boolean {
        val list = _functions.value.toMutableList()
        if (source.isBlank()) {
            if (index < list.size) list.removeAt(index)
        } else {
            val ok = runCatching { PlotExpression(source, prefs.settings.value.radians).check() }.isSuccess
            if (!ok) return false
            if (index < list.size) list[index] = source else if (list.size < MAX) list += source
        }
        _functions.value = list
        prefs.functions = list
        _picked.value = null
        _found.value = null
        return true
    }

    fun pick(p: Picked?) {
        _picked.value = p
    }

    /** Finds the points worth knowing in the window, off the main thread. */
    fun findPoints() {
        val v = _view.value ?: return
        val curves = curves()
        viewModelScope.launch {
            val live = curves.mapIndexedNotNull { i, f -> f?.let { i to it } }
            val found = withContext(Dispatchers.Default) {
                Plot.find(live.map { it.second }, v).map { f ->
                    f.copy(curve = live[f.curve].first, other = if (f.other >= 0) live[f.other].first else -1)
                }
            }
            _found.value = found
        }
    }

    companion object {
        const val MAX = 3

        fun factory(app: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer { GraphViewModel(Prefs.get(app)) }
        }
    }
}
