package bob.colbaskin.gidromonitor.features.report.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bob.colbaskin.gidromonitor.common.UiState
import bob.colbaskin.gidromonitor.common.toUiState
import bob.colbaskin.gidromonitor.features.analysis.domain.AnalysisRepository
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisResult
import bob.colbaskin.gidromonitor.features.analysis.domain.model.isMergedCogFileName
import bob.colbaskin.gidromonitor.features.map.data.CachedCogPreview
import bob.colbaskin.gidromonitor.features.map.data.CogRasterLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

data class ReportState(
    val result: UiState<AnalysisResult> = UiState.Loading,
    val geoJson: UiState<String>? = null,
    val isRefreshing: Boolean = false,
    val occurrencePreview: CachedCogPreview? = null
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: AnalysisRepository,
    private val cogRasterLoader: CogRasterLoader
) : ViewModel() {
    private var observationJob: Job? = null
    var state by mutableStateOf(ReportState())
        private set

    fun load(analysisId: String, force: Boolean = false) {
        if (analysisId.isBlank() || (!force && state.result !is UiState.Loading)) return
        if (force) {
            refresh(analysisId)
            return
        }
        state = state.copy(result = UiState.Loading, isRefreshing = false)
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            repository.observeAnalysis(analysisId).collect { cached ->
                if (cached != null) state = state.copy(result = UiState.Success(cached), isRefreshing = false)
            }
        }
        viewModelScope.launch { state = state.copy(result = repository.getAnalysis(analysisId).toUiState()) }
        loadSavedOccurrence(analysisId)
    }

    fun refresh(analysisId: String) {
        if (analysisId.isBlank() || state.isRefreshing) return
        state = state.copy(isRefreshing = true)
        viewModelScope.launch {
            state = state.copy(result = repository.getAnalysis(analysisId).toUiState(), isRefreshing = false)
        }
    }

    fun prepareGeoJson(analysisId: String) {
        viewModelScope.launch { state = state.copy(geoJson = repository.exportGeoJson(analysisId).toUiState()) }
    }

    fun consumeExport() { state = state.copy(geoJson = null) }

    private fun loadSavedOccurrence(analysisId: String) = viewModelScope.launch {
        val files = repository.getCachedAnalysisRasterFiles(analysisId)
        val file = files.firstOrNull { it.fileName.isMergedCogFileName() }
        val preview = if (file == null) null else cogRasterLoader.cachedOccurrencePreview(file)
        state = state.copy(occurrencePreview = preview)
    }
}
