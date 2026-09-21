package bob.colbaskin.gidromonitor.features.analysis.presentation

import bob.colbaskin.gidromonitor.common.UiState
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisArea
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisResult
import bob.colbaskin.gidromonitor.features.analysis.domain.model.GeoPoint
import bob.colbaskin.gidromonitor.features.map.domain.model.BaseMapStyle

data class NewAnalysisState(
    val title: String = "",
    val dateBefore: String = "",
    val dateAfter: String = "",
    val area: AnalysisArea? = null,
    val drawingMode: DrawMode = DrawMode.NONE,
    val draftPoints: List<GeoPoint> = emptyList(),
    val mapStyle: BaseMapStyle = BaseMapStyle.SATELLITE,
    val resultState: UiState<AnalysisResult>? = null,
    val validationMessage: String? = null
)
