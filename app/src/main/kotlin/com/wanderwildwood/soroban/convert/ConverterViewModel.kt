package com.wanderwildwood.soroban.convert

import android.app.Application
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.data.converter.Converter
import com.sadellie.unitto.core.data.converter.ConverterResult
import com.sadellie.unitto.core.data.converter.UnitID
import com.sadellie.unitto.core.data.converter.Units
import com.sadellie.unitto.core.model.converter.UnitGroup
import com.sadellie.unitto.core.model.converter.unit.BasicUnit
import com.sadellie.unitto.core.ui.textfield.addBracket
import com.sadellie.unitto.core.ui.textfield.addTokens
import com.sadellie.unitto.core.ui.textfield.deleteTokens
import com.sadellie.unitto.core.ui.textfield.getTextFieldState
import com.sadellie.unitto.core.ui.textfield.observe
import com.wanderwildwood.soroban.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the converter knows about exchange rates at the moment. */
data class RatesView(
    /** The rates in use, or null when the phone has never fetched any. */
    val table: RateTable? = null,
    val fetching: Boolean = false,
    /** The last attempt to fetch failed; [table], if any, is an older day's. */
    val failed: Boolean = false,
)

/**
 * The converter: a group, a unit to convert from and one to convert to, and what is typed.
 *
 * Exchange rates are fetched when currency is opened and not otherwise, and at most once a day;
 * between times, and with no signal, the rates kept on the phone are used and their date shown.
 */
class ConverterViewModel(
    private val prefs: Prefs,
    private val rates: Rates,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val input = savedStateHandle.getTextFieldState(INPUT_KEY)

    private val _group = MutableStateFlow(
        prefs.group?.let { name -> UnitGroup.entries.firstOrNull { it.name == name } } ?: UnitGroup.LENGTH
    )
    val group: StateFlow<UnitGroup> = _group.asStateFlow()

    private val _pair = MutableStateFlow(pairFor(_group.value))
    /** The unit converted from, and the unit converted to. */
    val pair: StateFlow<Pair<BasicUnit, BasicUnit>> = _pair.asStateFlow()

    private val _rates = MutableStateFlow(RatesView())
    val ratesView: StateFlow<RatesView> = _rates.asStateFlow()

    private val _result = MutableStateFlow<ConverterResult?>(null)
    val result: StateFlow<ConverterResult?> = _result.asStateFlow()

    val settings = prefs.settings

    private val converter = Converter { from, to -> _rates.value.table?.rate(from, to) }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val cached = rates.cached()
            _rates.update { it.copy(table = cached) }
            if (_group.value == UnitGroup.CURRENCY) fetchRatesIfStale()
        }
        viewModelScope.launch {
            combine(input.observe(), _pair, _rates) { text, pair, _ -> text.toString() to pair }
                .collectLatest { (text, pair) -> _result.value = convert(text, pair.first, pair.second) }
        }
        viewModelScope.launch { input.observe().collectLatest { savedStateHandle[INPUT_KEY] = it.toString() } }
    }

    private suspend fun convert(text: String, from: BasicUnit, to: BasicUnit): ConverterResult? =
        withContext(Dispatchers.Default) {
            if (text.isEmpty()) return@withContext null
            try {
                converter.convert(from, to, text)
            } catch (e: Exception) {
                // Half-typed: "12+" has no answer yet, and says nothing rather than an error.
                null
            }
        }

    /** What [value] comes to in each unit of the group, for the list of units to convert to. */
    suspend fun convertAll(units: List<BasicUnit>): Map<String, ConverterResult> =
        withContext(Dispatchers.Default) {
            val text = input.text.toString()
            val from = _pair.value.first
            if (text.isEmpty()) return@withContext emptyMap()
            units.mapNotNull { unit ->
                runCatching { converter.convert(from, unit, text) }.getOrNull()?.let { unit.id to it }
            }.toMap()
        }

    /** What is typed, a length of time, written out in days, hours and so on. */
    fun timeBreakdown(): ConverterResult.Time = converter.timeBreakdown(_pair.value.first, input.text.toString())

    /** The group's units, those chosen lately first, then the rest in Unitto's order. */
    fun unitsInOrder(group: UnitGroup): List<BasicUnit> {
        val units = Units.inGroup(group)
        val pair = _pair.value
        val recent = (listOf(pair.first.id, pair.second.id) + prefs.recent(group.name)).distinct()
        val first = recent.mapNotNull { id -> units.firstOrNull { it.id == id } }
        return first + units.filter { it !in first }
    }

    fun selectGroup(group: UnitGroup) {
        if (group == _group.value) return
        val wasNumberBase = _group.value == UnitGroup.NUMBER_BASE
        _group.value = group
        prefs.group = group.name
        _pair.value = pairFor(group)
        // Digits typed for hexadecimal mean nothing in metres, and an expression nothing in hex.
        if (wasNumberBase || group == UnitGroup.NUMBER_BASE) input.clearText()
        if (group == UnitGroup.CURRENCY) viewModelScope.launch(Dispatchers.IO) { fetchRatesIfStale() }
    }

    fun selectFrom(unit: BasicUnit) {
        val (from, to) = _pair.value
        if (unit.id == from.id) return
        val next = if (unit.id == to.id) unit to from else unit to to
        setPair(next)
        if (unit.group == UnitGroup.NUMBER_BASE) dropInvalidDigits(unit)
    }

    fun selectTo(unit: BasicUnit) {
        val (from, to) = _pair.value
        if (unit.id == to.id) return
        setPair(if (unit.id == from.id) to to from else from to unit)
    }

    fun swap() {
        val (from, to) = _pair.value
        // The answer becomes what is typed, so swapping twice comes back to where it started.
        val answer = (_result.value as? ConverterResult.NumberBase)?.value
        setPair(to to from)
        if (answer != null) input.setTextAndPlaceCursorAtEnd(answer.uppercase())
        else if (to.group == UnitGroup.NUMBER_BASE) dropInvalidDigits(to)
    }

    private fun setPair(pair: Pair<BasicUnit, BasicUnit>) {
        _pair.value = pair
        prefs.setPair(pair.first.group.name, pair.first.id, pair.second.id)
    }

    fun key(token: String) {
        if (_group.value == UnitGroup.NUMBER_BASE) {
            // Number bases take digits only, appended as typed.
            if (input.text.length < MAX_BASE_DIGITS) input.edit { append(token) }
        } else {
            input.addTokens(token)
        }
    }

    fun bracket() = input.addBracket()

    fun delete() = input.deleteTokens()

    fun clear() = input.clearText()

    /** Puts a pasted number in place of what is typed. */
    fun replaceInput(tokens: String) = input.setTextAndPlaceCursorAtEnd(tokens)

    /** Tries the rates again, from the line that says they could not be fetched. */
    fun retryRates() = viewModelScope.launch(Dispatchers.IO) { fetchRates() }

    private suspend fun fetchRatesIfStale() {
        if (_rates.value.fetching) return
        if (rates.isStale() || _rates.value.table == null) fetchRates()
    }

    private suspend fun fetchRates() {
        if (_rates.value.fetching) return
        _rates.update { it.copy(fetching = true) }
        try {
            val table = withContext(Dispatchers.IO) { rates.fetch() }
            _rates.value = RatesView(table = table)
        } catch (e: Exception) {
            _rates.update { it.copy(fetching = false, failed = true) }
        }
    }

    private fun dropInvalidDigits(unit: BasicUnit) {
        val base = unit.factor.intValueExact()
        val valid = input.text.filter { Character.digit(it, base) >= 0 }
        if (valid.length != input.text.length) input.setTextAndPlaceCursorAtEnd(valid.toString())
    }

    private fun pairFor(group: UnitGroup): Pair<BasicUnit, BasicUnit> {
        val units = Units.inGroup(group)
        val saved = prefs.pair(group.name)?.let { (a, b) -> Units.byId(a) to Units.byId(b) }
        if (saved != null && saved.first != null && saved.second != null) return saved.first!! to saved.second!!
        val (a, b) = DEFAULT_PAIRS[group] ?: (units[0].id to units[1].id)
        return (Units.byId(a) ?: units[0]) to (Units.byId(b) ?: units[1])
    }

    companion object {
        private const val INPUT_KEY = "CONVERTER_INPUT"
        private const val MAX_BASE_DIGITS = 64

        /** Where each group starts the first time it is opened. */
        private val DEFAULT_PAIRS = mapOf(
            UnitGroup.LENGTH to (UnitID.kilometer to UnitID.mile),
            UnitGroup.CURRENCY to (UnitID.currency_usd to UnitID.currency_eur),
            UnitGroup.MASS to (UnitID.kilogram to UnitID.pound),
            UnitGroup.SPEED to (UnitID.kilometer_per_hour to UnitID.mile_per_hour),
            UnitGroup.TEMPERATURE to (UnitID.celsius to UnitID.fahrenheit),
            UnitGroup.AREA to (UnitID.square_meter to UnitID.square_foot),
            UnitGroup.TIME to (UnitID.hour to UnitID.minute),
            UnitGroup.VOLUME to (UnitID.liter to UnitID.us_liquid_gallon),
            UnitGroup.DATA to (UnitID.gigabyte to UnitID.megabyte),
            UnitGroup.PRESSURE to (UnitID.kilopascal to UnitID.psi),
            UnitGroup.ENERGY to (UnitID.kilojoule to UnitID.kilocalorie_th),
            UnitGroup.ANGLE to (UnitID.degree to UnitID.radian),
            UnitGroup.NUMBER_BASE to (UnitID.decimal to UnitID.hexadecimal),
        )

        fun factory(app: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer { ConverterViewModel(Prefs.get(app), Rates.get(app), createSavedStateHandle()) }
        }
    }
}

/** The digits a number base's keypad offers, in keypad order. */
val BASE_DIGITS = Token.Digit.all + Token.Letter.all
