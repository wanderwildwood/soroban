package com.wanderwildwood.soroban

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalCursorBlinkEnabled
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.soroban.ui.SorobanApp
import com.wanderwildwood.soroban.ui.monochrome

class MainActivity : ComponentActivity() {
    private val turner = TurnWithPhone(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only the graph turns; the app opens on the page it was left on.
        Prefs.get(this).let { turner.set(it.page == 1 && it.settings.value.turnWithPhone) }
        setContent {
            // A steady cursor: a blinking one is a repaint twice a second on an e-ink panel
            // for as long as a field has focus.
            CompositionLocalProvider(LocalCursorBlinkEnabled provides false) {
                ThemeMMD(colorScheme = monochrome) {
                    SorobanApp(onTurn = { turner.set(it) })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        turner.resume()
    }

    override fun onPause() {
        turner.pause()
        super.onPause()
    }
}
