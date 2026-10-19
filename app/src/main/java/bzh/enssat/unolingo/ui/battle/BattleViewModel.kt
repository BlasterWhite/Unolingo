package bzh.enssat.unolingo.ui.battle

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import bzh.enssat.unolingo.UnolingoApplication
import bzh.enssat.unolingo.data.CharacterRepository
import bzh.enssat.unolingo.domain.battle.BattleGame
import bzh.enssat.unolingo.domain.battle.Player
import bzh.enssat.unolingo.domain.model.CharacterSet
import bzh.enssat.unolingo.domain.quiz.QuestionGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface BattleUiState {
    data object Setup : BattleUiState

    data object Loading : BattleUiState

    data object LoadError : BattleUiState

    data class Playing(val game: BattleGame) : BattleUiState
}

/** Local battle: both players answer on this device (split screen). */
class BattleViewModel(
    private val repository: CharacterRepository,
    private val generator: QuestionGenerator = QuestionGenerator(),
    private val revealDelayMillis: Long = REVEAL_DELAY_MILLIS,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BattleUiState>(BattleUiState.Setup)
    val uiState: StateFlow<BattleUiState> = _uiState.asStateFlow()

    private var characterSet: CharacterSet? = null
    private var gameJob: Job? = null

    fun start(set: CharacterSet) {
        characterSet = set
        gameJob?.cancel()
        _uiState.value = BattleUiState.Loading
        gameJob = viewModelScope.launch {
            try {
                val characters = repository.getCharacters(set)
                _uiState.value = BattleUiState.Playing(BattleGame.create(characters, generator))
            } catch (e: IOException) {
                onLoadError(set, e)
            } catch (e: IllegalArgumentException) { // malformed JSON or invalid character entry
                onLoadError(set, e)
            }
        }
    }

    fun rematch() {
        characterSet?.let(::start)
    }

    fun quit() {
        gameJob?.cancel()
        _uiState.value = BattleUiState.Setup
    }

    fun submitAnswer(player: Player, answer: String) {
        val game = (_uiState.value as? BattleUiState.Playing)?.game ?: return
        val updated = game.submit(player, answer)
        if (updated == game) return
        _uiState.value = BattleUiState.Playing(updated)
        if (updated.currentRound.isOver && !updated.isFinished) {
            // Leave both players time to see the right answer before the next round.
            gameJob = viewModelScope.launch {
                delay(revealDelayMillis)
                val current = (_uiState.value as? BattleUiState.Playing)?.game ?: return@launch
                _uiState.value = BattleUiState.Playing(current.nextRound())
            }
        }
    }

    private fun onLoadError(set: CharacterSet, e: Exception) {
        Log.e(TAG, "Cannot load $set characters", e)
        _uiState.value = BattleUiState.LoadError
    }

    companion object {
        private const val TAG = "BattleViewModel"
        private const val REVEAL_DELAY_MILLIS = 1_500L

        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as UnolingoApplication
                BattleViewModel(app.container.characterRepository)
            }
        }
    }
}
