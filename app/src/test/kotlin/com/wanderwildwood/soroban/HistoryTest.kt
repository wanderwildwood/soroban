package com.wanderwildwood.soroban

import com.wanderwildwood.soroban.calc.History
import com.wanderwildwood.soroban.calc.Line
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryTest {

    @Test
    fun newestFirstKeptAcrossARestartAndCapped() {
        val dir = Files.createTempDirectory("tape").toFile()
        val file = File(dir, "history.json")
        val h = History(file)
        h.add(Line("1+1", "2"))
        h.add(Line("2×3", "6"))
        h.add(Line("1+1", "2"))
        assertEquals(listOf(Line("1+1", "2"), Line("2×3", "6")), h.lines.value)

        // A fresh instance reads the same tape back from the file.
        assertEquals(h.lines.value, History(file).lines.value)

        repeat(History.MAX + 20) { h.add(Line("$it+0", "$it")) }
        assertEquals(History.MAX, h.lines.value.size)
        assertEquals(Line("${History.MAX + 19}+0", "${History.MAX + 19}"), h.lines.value.first())

        h.clear()
        assertEquals(emptyList<Line>(), History(file).lines.value)
        dir.deleteRecursively()
    }
}
