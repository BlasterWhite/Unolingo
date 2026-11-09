package fr.enssat.unolingo.mguezenn_mphongsa.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.enssat.unolingo.mguezenn_mphongsa.UnolingoApplication
import fr.enssat.unolingo.mguezenn_mphongsa.data.results.ResultsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Progress page state, recomputed live each time a quiz is saved. */
class StatsViewModel(repository: ResultsRepository) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = repository.observeOutcomes()
        .map(::statsUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState.Loading)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as UnolingoApplication
                StatsViewModel(app.container.resultsRepository)
            }
        }
    }
}
