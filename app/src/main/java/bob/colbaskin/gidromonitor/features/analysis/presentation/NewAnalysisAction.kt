package bob.colbaskin.gidromonitor.features.analysis.presentation

import bob.colbaskin.gidromonitor.features.map.domain.model.BaseMapStyle
import bob.colbaskin.gidromonitor.features.analysis.domain.model.GeoPoint

sealed interface NewAnalysisAction {
    data class UpdateTitle(val value: String) : NewAnalysisAction
    data class UpdateDateBefore(val value: String) : NewAnalysisAction
    data class UpdateDateAfter(val value: String) : NewAnalysisAction
    data class ChangeBaseMap(val style: BaseMapStyle) : NewAnalysisAction
    data class StartDrawing(val mode: DrawMode) : NewAnalysisAction
    data class AddMapPoint(val point: GeoPoint) : NewAnalysisAction
    data object RemoveLastPoint : NewAnalysisAction
    data object ClearArea : NewAnalysisAction
    data object RunAnalysis : NewAnalysisAction
    data object DismissResult : NewAnalysisAction
}

enum class DrawMode { NONE, RECTANGLE, POLYGON }
