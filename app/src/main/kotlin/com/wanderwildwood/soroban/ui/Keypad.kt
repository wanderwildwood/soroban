package com.wanderwildwood.soroban.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.soroban.Prefs

/**
 * One key. [label] is what it says; [icon] stands in for it where a word would not fit
 * (backspace). [filled] marks the one key a sum ends on, =, black with its label in white.
 * A key that does nothing here is left blank rather than greyed, since grey is what this
 * panel does worst.
 */
data class Key(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val description: String? = null,
    val filled: Boolean = false,
    /** Words and functions, smaller than the digits so "sin⁻¹" fits its key. */
    val small: Boolean = false,
    /** A word in the programmer's mode row, a step smaller again so "QWORD" clears its rules. */
    val mode: Boolean = false,
    val enabled: Boolean = true,
    /** A mode that is on, said in bold. */
    val bold: Boolean = false,
)

/**
 * Keys in a grid of equal cells, ruled apart by a hairline of solid black: no raised buttons,
 * no ripple, nothing that moves when pressed. The sum on the screen changing is the answer
 * to a press.
 */
@Composable
fun Keypad(rows: List<List<Key?>>, modifier: Modifier = Modifier) {
    val settings by Prefs.get(LocalContext.current).settings.collectAsStateWithLifecycle()
    Column(modifier.fillMaxWidth()) {
        rows.forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth().weight(1f)) {
                row.forEachIndexed { c, key ->
                    Cell(
                        key = key,
                        vibrate = settings.vibrate,
                        lineRight = c < row.lastIndex,
                        lineBelow = r < rows.lastIndex,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

/**
 * [vibrate] asks the phone for its keyboard tap at each press: the same short tick the stock
 * calculator gives, through the view rather than the vibrator, so the phone's own touch
 * feedback setting still has the last word and a phone with it off stays quiet.
 */
@Composable
private fun Cell(key: Key?, vibrate: Boolean, lineRight: Boolean, lineBelow: Boolean, modifier: Modifier) {
    val live = key != null && key.enabled
    val view = LocalView.current
    val press: () -> Unit = {
        if (vibrate) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        key?.onClick?.invoke()
    }
    Box(
        modifier = modifier
            .drawBehind {
                // Two whole pixels, so the rule is black on the panel and not a grey smear.
                val w = 2f
                if (lineRight) drawLine(Color.Black, Offset(size.width - w / 2, 0f), Offset(size.width - w / 2, size.height), w)
                if (lineBelow) drawLine(Color.Black, Offset(0f, size.height - w / 2), Offset(size.width, size.height - w / 2), w)
            }
            .let { if (live) it.clickable(onClick = press) else it }
            .let { if (key?.description != null) it.semantics { contentDescription = key.description } else it },
        contentAlignment = Alignment.Center,
    ) {
        if (key == null || !key.enabled) return@Box
        if (key.filled) {
            Box(
                Modifier.fillMaxSize().padding(end = 2.dp, bottom = 2.dp).background(Color.Black),
                contentAlignment = Alignment.Center,
            ) { KeyFace(key, Color.White) }
        } else {
            KeyFace(key, Color.Black)
        }
    }
}

@Composable
private fun KeyFace(key: Key, color: Color) {
    if (key.icon != null) {
        Icon(
            imageVector = key.icon,
            contentDescription = key.description,
            tint = color,
            modifier = Modifier.size(30.dp),
        )
    } else {
        TextMMD(
            text = key.label,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            // A key's figure is an instrument's, like a speedometer's, and is sized to the key
            // rather than to the type scale: 30sp digits are what stock calculators use here.
            fontSize = when { key.mode -> 19.sp; key.small -> 22.sp; else -> 30.sp },
            fontWeight = if (key.bold) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
