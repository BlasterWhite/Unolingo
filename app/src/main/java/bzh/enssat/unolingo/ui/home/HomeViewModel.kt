package bzh.enssat.unolingo.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import bzh.enssat.unolingo.UnolingoApplication
import bzh.enssat.unolingo.data.CharacterRepository
import bzh.enssat.unolingo.domain.model.CharacterSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** Exposes how many characters each set contains, shown on the home cards. */
class HomeViewModel(private val repository: CharacterRepository) : ViewModel() {

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
                HomeViewModel(app.container.characterRepository)
            }
        }
    }
}
