package com.wanderwildwood.soroban.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.radio_button.RadioButtonMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.soroban.R
import kotlinx.coroutines.delay

/** An icon in a top bar, with a target a thumb can find. */
@Composable
fun BarButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(48.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** A row that says what it is and what it is set to, and does something when pressed. */
@Composable
fun SettingRow(title: String, value: String?, onClick: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 14.dp),
    ) {
        TextMMD(text = title, style = MaterialTheme.typography.bodyMedium)
        if (value != null) TextMMD(text = value, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, note: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            TextMMD(text = title, style = MaterialTheme.typography.bodyMedium)
            if (note != null) TextMMD(text = note, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.width(12.dp))
        SwitchMMD(checked = checked, onCheckedChange = null)
    }
}

/** One of a few choices, pressed in place: MMD's radio button, the row taking the press. */
@Composable
fun ChoiceRow(title: String, chosen: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButtonMMD(selected = chosen, onClick = null)
        Spacer(Modifier.width(8.dp))
        TextMMD(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * A row that asks once, in its own face: the first press arms it and changes what it says,
 * the second does it. It disarms itself after four seconds, so a stray press does not leave
 * a live trigger for whoever picks the phone up next.
 */
@Composable
fun ArmedRow(label: String, armedLabel: String, onConfirmed: () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (!armed) return@LaunchedEffect
        delay(4000)
        armed = false
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (armed) {
                    armed = false
                    onConfirmed()
                } else {
                    armed = true
                }
            }
            .padding(vertical = 14.dp),
    ) {
        TextMMD(
            text = if (armed) armedLabel else label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (armed) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/** A short list of choices in a dialog; choosing one closes it. */
@Composable
fun <T> OptionsDialog(
    title: String,
    options: List<Pair<T, String>>,
    chosen: T,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        options.forEach { (value, label) ->
            ChoiceRow(label, value == chosen) {
                onPick(value)
                onDismiss()
            }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedButtonMMD(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            TextMMD(text = stringResource(R.string.cancel), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** A plain bordered box: the house's way of marking a thing as chosen, or as pressable. */
fun Modifier.rule(width: Int = 2, corner: Int = 8) =
    border(BorderStroke(width.dp, androidx.compose.ui.graphics.Color.Black), RoundedCornerShape(corner.dp))
