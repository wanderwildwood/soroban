/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2025 Elshan Agaev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


package com.wanderwildwood.soroban.calc

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
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.isExpression
import com.sadellie.unitto.core.common.isGreaterThan
import com.sadellie.unitto.core.common.toFormattedString
import com.sadellie.unitto.core.ui.textfield.addBracket
import com.sadellie.unitto.core.ui.textfield.addTokens
import com.sadellie.unitto.core.ui.textfield.deleteTokens
import com.sadellie.unitto.core.ui.textfield.getTextFieldState
import com.sadellie.unitto.core.ui.textfield.observe
import com.sadellie.unitto.core.ui.textfield.placeCursorAtTheEnd
import com.sadellie.unitto.feature.calculator.toFractionalString
import com.wanderwildwood.soroban.Prefs
import io.github.sadellie.evaluatto.Expression
import io.github.sadellie.evaluatto.ExpressionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the line under the sum says. */
sealed class CalculationResult {
  /** A preview while typing, or after = the same answer as a fraction (empty if none). */
  data class Success(val text: String) : CalculationResult()

  data object Empty : CalculationResult()

  data object DivideByZeroError : CalculationResult()

  data object Error : CalculationResult()
}

/**
 * Unitto's calculator, kept as it was but for where it keeps things: the settings and the tape
 * come from [Prefs] and [History] instead of Unitto's DataStore and Room database.
 *
 * The sum being typed is a [androidx.compose.foundation.text.input.TextFieldState] as in Unitto,
 * because its editing rules (an operator replacing the one before it, brackets that open or
 * close as they should, a function deleted whole) are written against one. Its cursor is kept
 * at the end: the screen draws it as plain text with no caret, since a blinking caret is an
 * animation on this panel.
 */
class CalculatorViewModel(
  private val prefs: Prefs,
  private val history: History,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
  private var _calculationJob: Job? = null
  private val _inputKey = "CALCULATOR_INPUT"
  val input = savedStateHandle.getTextFieldState(_inputKey)
  private val _result = MutableStateFlow<CalculationResult>(CalculationResult.Empty)
  val result: StateFlow<CalculationResult> = _result.asStateFlow()
  private val _equalClicked = MutableStateFlow(savedStateHandle.get<Boolean>(EQUALLED_KEY) ?: false)
  /** True straight after =, while the answer stands in the sum's place. */
  val equalled: StateFlow<Boolean> = _equalClicked.asStateFlow()
  val settings = prefs.settings
  val lines = history.lines

  init {
    viewModelScope.launch { observeInput() }
    // A change of setting (decimal places, degrees) redraws the preview.
    viewModelScope.launch { prefs.settings.drop(1).collectLatest { if (!_equalClicked.value) calculateInput() } }
  }

  private suspend fun observeInput() {
    input.observe().collectLatest {
      // Do not process input if equal was clicked to keep fractional output
      if (_equalClicked.value) return@collectLatest
      savedStateHandle[_inputKey] = it.toString()
      calculateInput()
    }
  }

  private fun setEqualled(value: Boolean) {
    _equalClicked.update { value }
    savedStateHandle[EQUALLED_KEY] = value
  }

  fun addTokens(tokens: String) {
    when {
      // Equal was clicked and user tries to type a digit or dot
      _equalClicked.value && tokens in Token.Digit.allWithDot -> input.clearText()
      // ...or starts something new: a function, a constant, a root or a bracket. Only an
      // operator carries on from the answer, as "40" then "×2" does.
      _equalClicked.value && startsAfresh(tokens) -> input.clearText()
      // Equal was clicked and user tries to add operator or something
      _equalClicked.value -> input.placeCursorAtTheEnd()
    }
    setEqualled(false)
    input.addTokens(tokens)
  }

  fun addBracket() {
    if (_equalClicked.value) {
      // Cursor is set to 0 when equal is clicked
      input.placeCursorAtTheEnd()
    }
    setEqualled(false)
    input.addBracket()
  }

  fun deleteTokens() {
    val wasEqualled = _equalClicked.value
    setEqualled(false)
    if (wasEqualled) {
      input.clearText()
    } else {
      input.deleteTokens()
    }
  }

  fun clearInput() {
    setEqualled(false)
    input.clearText()
  }

  /** Puts [tokens] in place of the whole sum: a line chosen from the tape, or a paste. */
  fun replaceInput(tokens: String) {
    setEqualled(false)
    input.setTextAndPlaceCursorAtEnd(tokens)
  }

  fun updateRadianMode(newValue: Boolean) {
    prefs.update { it.copy(radians = newValue) }
    if (_equalClicked.value) {
      setEqualled(false)
      calculateInput()
    }
  }

  fun clearHistory() = viewModelScope.launch(Dispatchers.IO) { history.clear() }

  fun onEqualClick() =
    viewModelScope.launch {
      val prefs = settings.value
      if (_equalClicked.value) return@launch
      val inputValue = input.text.toString()
      if (!inputValue.isExpression()) return@launch

      val calculated =
        try {
            calculate(inputValue, prefs.radians, KRoundingMode.HALF_EVEN)
              .toFormattedString(prefs.precision, prefs.outputFormat)
          } catch (e: ExpressionException.DivideByZero) {
            _result.update { CalculationResult.DivideByZeroError }
            return@launch
          } catch (e: Exception) {
            _result.update { CalculationResult.Error }
            return@launch
          }
          // replace minus symbols since it is not recognized by evaluatto
          .replace("-", Token.Operator.MINUS)

      val fractional =
        // A whole answer has no fraction to show. Without this, a sum whose exact value is
        // whole but whose arithmetic is not (40 × sin 30° is 19.999…) came out as "19 1⁄1".
        if (prefs.fractions && Token.PERIOD in calculated) {
          try {
            // Different rounding mode to properly calculate fractional
            calculate(inputValue, prefs.radians, KRoundingMode.DOWN).toFractionalString()
              .takeUnless { it.endsWith(Token.DisplayOnly.FRACTION + "1") }
              .orEmpty()
          } catch (e: Exception) {
            ""
          }
        } else {
          // User doesn't want fractional output, clear result field
          ""
        }

      withContext(Dispatchers.IO) { history.add(Line(expression = inputValue, result = calculated)) }

      setEqualled(true)
      savedStateHandle[_inputKey] = calculated
      input.setTextAndPlaceCursorAtEnd(calculated)
      _result.update { CalculationResult.Success(fractional) }
    }

  private fun calculateInput() {
    _calculationJob?.cancel()
    _calculationJob =
      viewModelScope.launch {
        if (!input.text.toString().isExpression()) {
          _result.update { CalculationResult.Empty }
          return@launch
        }

        val prefs = settings.value
        val newResult =
          try {
            val calculated =
              calculate(
                input = input.text.toString(),
                radianMode = prefs.radians,
                roundingMode = KRoundingMode.HALF_EVEN,
              )
            CalculationResult.Success(
              calculated.toFormattedString(prefs.precision, prefs.outputFormat).replace("-", Token.Operator.MINUS)
            )
          } catch (e: Exception) {
            CalculationResult.Empty
          }
        _result.update { newResult }
      }
  }

  private suspend fun calculate(
    input: String,
    radianMode: Boolean,
    roundingMode: KRoundingMode = KRoundingMode.HALF_EVEN,
  ): KBigDecimal =
    withContext(Dispatchers.Default) {
      Expression(input, radianMode, roundingMode).calculate().also {
        if (it.isGreaterThan(maxCalculationResult)) throw ExpressionException.TooBig()
      }
    }

  private val maxCalculationResult = KBigDecimal.valueOf(Double.MAX_VALUE)

  private fun startsAfresh(tokens: String): Boolean =
    tokens in Token.Func.allWithOpeningBracket ||
      tokens in Token.Const.all ||
      tokens == Token.Operator.SQRT ||
      tokens == Token.Operator.LEFT_BRACKET ||
      tokens.first().isDigit()

  companion object {
    private const val EQUALLED_KEY = "CALCULATOR_EQUALLED"

    fun factory(app: Application): ViewModelProvider.Factory = viewModelFactory {
      initializer { CalculatorViewModel(Prefs.get(app), History.get(app), createSavedStateHandle()) }
    }
  }
}
