package fr.enssat.unolingo.mguezenn_mphongsa.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.enssat.unolingo.mguezenn_mphongsa.UnolingoApplication
import fr.enssat.unolingo.mguezenn_mphongsa.data.CharacterRepository
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.data.results.ResultsRepository
import fr.enssat.unolingo.mguezenn_mphongsa.ui.stats.GradeSummary
import fr.enssat.unolingo.mguezenn_mphongsa.ui.stats.gradeSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** Exposes how many characters each set contains and the player's grade, shown on the home screen. */
class HomeViewModel(
    private val repository: CharacterRepository,
    results: ResultsRepository,
) : ViewModel() {

    /** The player's grade, updated live as quizzes are saved; null until the first value is read. */
    val grade: StateFlow<GradeSummary?> = results.observeOutcomes()
        .map { gradeSummary(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)


    private val _characterCounts = MutableStateFlow<Map<CharacterSet, Int>>(emptyMap())
    val characterCounts: StateFlow<Map<CharacterSet, Int>> = _characterCounts.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _characterCounts.value = CharacterSet.entries.associateWith { repository.getCharacters(it).size }
            } catch (e: IOException) {
                Log.e(TAG, "Cannot count characters", e) // the cards just show no count
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Cannot count characters", e)
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"

        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as UnolingoApplication
                HomeViewModel(app.container.characterRepository, app.container.resultsRepository)
            }
        }
    }
}
