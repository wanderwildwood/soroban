package com.wanderwildwood.soroban.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.sadellie.unitto.core.common.normalizeSuperscript
import com.sadellie.unitto.core.data.converter.ConverterResult
import com.sadellie.unitto.core.model.converter.UnitGroup
import com.sadellie.unitto.core.model.converter.unit.BasicUnit
import com.wanderwildwood.soroban.R
import com.wanderwildwood.soroban.convert.ConverterViewModel

/** The groups of units, in Unitto's order, the one open marked in bold. */
@Composable
fun GroupPicker(current: UnitGroup, onPick: (UnitGroup) -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = stringResource(R.string.groups_title)) },
                navigationIcon = { BarButton(Icons.Back, stringResource(R.string.cd_back), onBack) },
            )
        },
    ) { padding ->
        LazyColumnMMD(Modifier.padding(padding).fillMaxSize()) {
            UnitGroup.entries.forEach { group ->
                item(key = group.name) {
                    Column {
                        TextMMD(
                            text = stringResource(group.res),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (group == current) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(group) }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                        )
                        HorizontalDividerMMD()
                    }
                }
            }
        }
    }
}

/**
 * The units of the open group, to convert from or to. A search field at the top narrows the
 * list by name or symbol; the keyboard opens only when the field is pressed. Choosing what to
 * convert *to* shows, beside each unit, what the typed value comes to in it, so the list is
 * itself an answer.
 */
@Composable
fun UnitPicker(vm: ConverterViewModel, choosingTo: Boolean, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val group by vm.group.collectAsStateWithLifecycle()
    val pair by vm.pair.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val rates by vm.ratesView.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    // Read once on opening, so the list does not reorder under the thumb as a unit is chosen.
    val units = remember(group) { vm.unitsInOrder(group) }
    val names = remember(group) { units.associate { it.id to (context.getString(it.displayName) to context.getString(it.shortName)) } }
    val shown = remember(query, units) { filter(units, names, query) }
    val conversions by produceState<Map<String, ConverterResult>>(emptyMap(), group, pair, rates, choosingTo) {
        value = if (choosingTo) vm.convertAll(units) else emptyMap()
    }
    val current = if (choosingTo) pair.second else pair.first

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = stringResource(if (choosingTo) R.string.pick_to else R.string.pick_from)) },
                navigationIcon = { BarButton(Icons.Back, stringResource(R.string.cd_back), onBack) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TextFieldMMD(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { TextMMD(text = stringResource(R.string.pick_search), style = MaterialTheme.typography.bodySmall) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            if (shown.isEmpty()) {
                TextMMD(
                    text = stringResource(R.string.pick_none),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(20.dp),
                )
            }
            LazyColumnMMD(Modifier.fillMaxSize()) {
                shown.forEach { unit ->
                    item(key = unit.id) {
                        val (name, short) = names.getValue(unit.id)
                        val answer = conversions[unit.id]?.let { answerText(it, settings) }
                        Column(
                            Modifier.fillMaxWidth().clickable {
                                if (choosingTo) vm.selectTo(unit) else vm.selectFrom(unit)
                                onBack()
                            },
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    TextMMD(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (unit.id == current.id) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    TextMMD(text = short, style = MaterialTheme.typography.labelSmall)
                                }
                                if (answer != null) {
                                    Spacer(Modifier.width(10.dp))
                                    TextMMD(
                                        text = answer,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.widthIn(max = 170.dp),
                                    )
                                }
                            }
                            HorizontalDividerMMD()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Units whose name or symbol holds [query], those that begin with it first. Superscripts are
 * read as plain digits, so "m2" finds m².
 */
internal fun filter(units: List<BasicUnit>, names: Map<String, Pair<String, String>>, query: String): List<BasicUnit> {
    val q = query.trim().lowercase().normalizeSuperscript()
    if (q.isEmpty()) return units
    fun clean(s: String) = s.lowercase().normalizeSuperscript()
    val scored = units.mapNotNull { unit ->
        val (name, short) = names.getValue(unit.id)
        val n = clean(name)
        val s = clean(short)
        when {
            s == q -> 0 to unit
            n.startsWith(q) -> 1 to unit
            s.startsWith(q) -> 2 to unit
            n.contains(q) -> 3 to unit
            s.contains(q) -> 4 to unit
            else -> null
        }
    }
    return scored.sortedBy { it.first }.map { it.second }
}
