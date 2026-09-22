package bob.colbaskin.gidromonitor.features.comparison.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bob.colbaskin.gidromonitor.common.UiState
import bob.colbaskin.gidromonitor.common.ApiResult
import bob.colbaskin.gidromonitor.common.toUiState
import bob.colbaskin.gidromonitor.features.analysis.domain.AnalysisRepository
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisResult
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisRasterFile
import bob.colbaskin.gidromonitor.features.analysis.domain.model.ObservationLayerType
import bob.colbaskin.gidromonitor.features.map.domain.model.BaseMapStyle
import bob.colbaskin.gidromonitor.features.map.data.CogRasterLoader
import bob.colbaskin.gidromonitor.features.map.data.DecodedCogRaster
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import javax.inject.Inject

data class ComparisonState(
    val result: UiState<AnalysisResult> = UiState.Loading,
    val splitPosition: Float = 0.5f,
    val mapStyle: BaseMapStyle = BaseMapStyle.SATELLITE,
    val enabledLayers: Set<ObservationLayerType> = setOf(ObservationLayerType.WATER_AFTER),
    val rasterFiles: UiState<List<AnalysisRasterFile>> = UiState.Loading,
    val enabledRasterFiles: Set<String> = emptySet(),
    val enabledCogChannels: Set<String> = emptySet(),
    val cogOverlayOpacity: Float = 0.40f,
    val decodedRasters: Map<String, DecodedCogRaster> = emptyMap(),
    val rasterLoadError: String? = null,
    val loadingRasterFiles: Set<String> = emptySet()
) {
    val isDecodingRaster: Boolean get() = loadingRasterFiles.isNotEmpty()
}

@HiltViewModel
class ComparisonViewModel @Inject constructor(
    private val repository: AnalysisRepository,
    private val cogRasterLoader: CogRasterLoader
) : ViewModel() {
    var state by mutableStateOf(ComparisonState())
        private set
    private var loadedAnalysisId: String? = null
    private var rasterGeneration = 0
    private val rasterJobs = mutableMapOf<String, Job>()
    private var rasterFilesJob: Job? = null

    fun load(analysisId: String) {
        if (analysisId.isBlank() || loadedAnalysisId == analysisId) return
        loadedAnalysisId = analysisId
        state = ComparisonState()

        requestAnalysis(analysisId)
        requestRasterFiles(analysisId)
    }

    fun retryRasterFiles() {
        loadedAnalysisId?.let { analysisId ->
            requestAnalysis(analysisId)
            requestRasterFiles(analysisId)
        }
    }

    private fun requestAnalysis(analysisId: String) {
        viewModelScope.launch {
            val result = repository.getAnalysis(analysisId)
            state = state.copy(result = result.toUiState())
            if (result is ApiResult.Error) stopRasterLoading(result.text)
        }
    }

    private fun requestRasterFiles(analysisId: String) {
        rasterFilesJob?.cancel()
        rasterGeneration++
        rasterJobs.values.forEach { it.cancel() }
        rasterJobs.clear()
        state = state.copy(
            rasterFiles = UiState.Loading,
            decodedRasters = emptyMap(),
            enabledRasterFiles = emptySet(),
            enabledCogChannels = emptySet(),
            loadingRasterFiles = emptySet(),
            rasterLoadError = null
        )
        rasterFilesJob = viewModelScope.launch {
            when (val files = repository.getAnalysisRasterFiles(analysisId)) {
                is ApiResult.Success -> if (files.data.isNotEmpty()) {
                    replaceRasterFiles(files.data)
                } else {
                    state = state.copy(
                        rasterFiles = UiState.Error(
                            "Файлы сравнения пока не готовы",
                            "Попробуйте открыть карту немного позже."
                        ),
                        rasterLoadError = "Файлы сравнения пока не готовы."
                    )
                }
                is ApiResult.Error -> state = state.copy(
                    rasterFiles = UiState.Error(files.title, files.text),
                    rasterLoadError = if (files.title == "Данные не найдены") {
                        "Данные для этого сравнения не найдены."
                    } else {
                        files.text
                    }
                )
            }
        }
    }

    private fun stopRasterLoading(errorText: String) {
        rasterFilesJob?.cancel()
        rasterFilesJob = null
        rasterGeneration++
        rasterJobs.values.forEach { it.cancel() }
        rasterJobs.clear()
        state = state.copy(
            rasterFiles = UiState.Error("Не удалось получить слои", errorText),
            loadingRasterFiles = emptySet(),
            rasterLoadError = errorText
        )
    }

    fun setSplitPosition(value: Float) { state = state.copy(splitPosition = value) }

    fun toggleBaseMap() {
        state = state.copy(
            mapStyle = if (state.mapStyle == BaseMapStyle.SATELLITE) {
                BaseMapStyle.STREETS
            } else {
                BaseMapStyle.SATELLITE
            }
        )
    }

    fun selectLayer(type: ObservationLayerType) {
        state = state.copy(
            enabledLayers = state.enabledLayers.toMutableSet().apply {
                if (!add(type)) remove(type)
            }
        )
    }

    fun toggleRasterFile(file: AnalysisRasterFile) {
        val fileName = file.fileName
        val isEnabled = fileName !in state.enabledRasterFiles
        val defaultChannels = state.decodedRasters[fileName]
            ?.channels
            ?.take(3)
            ?.map { it.id }
            .orEmpty()
        state = state.copy(
            enabledRasterFiles = state.enabledRasterFiles.toMutableSet().apply {
                if (!add(fileName)) remove(fileName)
            },
            enabledCogChannels = if (isEnabled) state.enabledCogChannels + defaultChannels else {
                state.enabledCogChannels.filterNot { it.startsWith("$fileName#") }.toSet()
            }
        )
        if (isEnabled && fileName !in state.decodedRasters) loadRaster(file, rasterGeneration)
    }

    fun toggleCogChannel(channelId: String) {
        state = state.copy(
            enabledCogChannels = state.enabledCogChannels.toMutableSet().apply {
                if (!add(channelId)) remove(channelId)
            }
        )
    }

    fun setCogOverlayOpacity(value: Float) {
        state = state.copy(cogOverlayOpacity = value.coerceIn(0.10f, 1f))
    }

    private fun replaceRasterFiles(files: List<AnalysisRasterFile>) {
        rasterGeneration++
        rasterJobs.values.forEach { it.cancel() }
        rasterJobs.clear()
        val defaultFile = files.firstOrNull { it.fileName.isCogFileName() } ?: files.first()
        state = state.copy(
            rasterFiles = UiState.Success(files),
            enabledRasterFiles = setOf(defaultFile.fileName),
            enabledCogChannels = emptySet(),
            decodedRasters = emptyMap(),
            rasterLoadError = null,
            loadingRasterFiles = setOf(defaultFile.fileName)
        )
        loadRaster(defaultFile, rasterGeneration)
    }

    private fun loadRaster(file: AnalysisRasterFile, generation: Int) {
        if (rasterJobs[file.fileName]?.isActive == true) return
        val timeoutJob = viewModelScope.launch {
            delay(RASTER_LOAD_TIMEOUT_MILLIS)
            if (generation == rasterGeneration && file.fileName in state.loadingRasterFiles) {
                rasterGeneration++
                rasterJobs.values.forEach { it.cancel() }
                rasterJobs.clear()
                state = state.copy(
                    loadingRasterFiles = state.loadingRasterFiles - file.fileName,
                    rasterLoadError = "Не удалось загрузить слои. Повторите попытку."
                )
            }
        }
        val job = viewModelScope.launch {
            if (generation != rasterGeneration) return@launch
            state = state.copy(
                loadingRasterFiles = state.loadingRasterFiles + file.fileName,
                rasterLoadError = null
            )
        val loaded = withTimeoutOrNull(RASTER_LOAD_TIMEOUT_MILLIS) {
            cogRasterLoader.load(file) { partial ->
            withContext(Dispatchers.Main.immediate) {
                if (generation != rasterGeneration) return@withContext
                state = state.copy(
                    decodedRasters = state.decodedRasters + (file.fileName to partial),
                    enabledCogChannels = state.enabledCogChannels + partial.channels.take(3).map { it.id }
                )
            }
            }
        } ?: ApiResult.Error(
            "Не удалось загрузить слои",
            "Загрузка заняла слишком много времени. Повторите попытку."
        )
        timeoutJob.cancel()
        when (loaded) {
            is bob.colbaskin.gidromonitor.common.ApiResult.Success -> if (generation == rasterGeneration) state = state.copy(
                    decodedRasters = state.decodedRasters + (file.fileName to loaded.data),
                    enabledCogChannels = state.enabledCogChannels + loaded.data.channels.take(3).map { it.id },
                    rasterLoadError = null,
                    loadingRasterFiles = state.loadingRasterFiles - file.fileName
                )
            is bob.colbaskin.gidromonitor.common.ApiResult.Error -> if (generation == rasterGeneration) state = state.copy(
                    rasterLoadError = loaded.text,
                    loadingRasterFiles = state.loadingRasterFiles - file.fileName
                )
            }
        }
        rasterJobs[file.fileName] = job
    }

    private fun String.isCogFileName(): Boolean =
        lowercase().endsWith(".cog.tif") || lowercase().endsWith(".cog.tiff") ||
            lowercase().endsWith("_cog.tif") || lowercase().endsWith("_cog.tiff")

    private companion object {
        const val RASTER_LOAD_TIMEOUT_MILLIS = 75_000L
    }

}
