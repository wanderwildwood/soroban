package com.wanderwildwood.soroban.calc

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** One finished calculation: what was typed, and what = gave. */
data class Line(val expression: String, val result: String)

/**
 * The calculator's tape: the last [MAX] calculations, newest first, in one small file in the
 * app's own storage. It is written whole each time, to a temporary file renamed over the old,
 * so a phone that dies mid-write keeps the previous tape rather than half of a new one.
 */
class History internal constructor(private val file: File) {

    private val state = MutableStateFlow(read())
    val lines: StateFlow<List<Line>> = state.asStateFlow()

    private fun read(): List<Line> = runCatching {
        if (!file.isFile) return emptyList()
        val array = JSONArray(file.readText())
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            Line(o.getString("e"), o.getString("r"))
        }
    }.getOrElse { emptyList() }

    @Synchronized
    fun add(line: Line) {
        val next = (listOf(line) + state.value.filter { it != line }).take(MAX)
        write(next)
    }

    @Synchronized
    fun clear() = write(emptyList())

    private fun write(lines: List<Line>) {
        state.value = lines
        val array = JSONArray()
        lines.forEach { array.put(JSONObject().put("e", it.expression).put("r", it.result)) }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(array.toString())
        tmp.renameTo(file)
    }

    companion object {
        const val MAX = 100

        @Volatile private var instance: History? = null

        fun get(context: Context): History = instance ?: synchronized(this) {
            instance ?: History(File(context.applicationContext.filesDir, "history.json")).also { instance = it }
        }
    }
}
