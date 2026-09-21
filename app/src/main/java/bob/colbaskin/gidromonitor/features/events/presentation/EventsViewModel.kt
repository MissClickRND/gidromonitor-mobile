package bob.colbaskin.gidromonitor.features.events.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bob.colbaskin.gidromonitor.common.UiState
import bob.colbaskin.gidromonitor.common.toUiState
import bob.colbaskin.gidromonitor.features.analysis.domain.AnalysisRepository
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisHistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventsState(
    val history: UiState<List<AnalysisHistoryItem>> = UiState.Loading,
    val query: String = "",
    val isRefreshing: Boolean = false
)

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: AnalysisRepository
) : ViewModel() {
    var state by mutableStateOf(EventsState())
        private set

    init {
        viewModelScope.launch {
            repository.observeHistory().collect { history ->
                if (history.isNotEmpty() || state.history is UiState.Success) {
                    state = state.copy(history = UiState.Success(history), isRefreshing = false)
                }
            }
        }
        refresh()
    }

    fun refresh() {
        if (state.isRefreshing) return
        val isInitialLoad = state.history is UiState.Loading
        state = if (isInitialLoad) state else state.copy(isRefreshing = true)
        viewModelScope.launch {
            state = state.copy(history = repository.getHistory().toUiState(), isRefreshing = false)
        }
    }

    fun updateQuery(value: String) { state = state.copy(query = value) }
}
