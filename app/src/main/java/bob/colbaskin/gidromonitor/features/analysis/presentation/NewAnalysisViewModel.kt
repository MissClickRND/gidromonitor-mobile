package bob.colbaskin.gidromonitor.features.analysis.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bob.colbaskin.gidromonitor.common.UiState
import bob.colbaskin.gidromonitor.common.toUiState
import bob.colbaskin.gidromonitor.features.analysis.domain.AnalysisRepository
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisRequest
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisArea
import bob.colbaskin.gidromonitor.features.analysis.domain.model.GeoPoint
import bob.colbaskin.gidromonitor.features.map.domain.AmurOblast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

@HiltViewModel
class NewAnalysisViewModel @Inject constructor(
    private val analysisRepository: AnalysisRepository
) : ViewModel() {
    var state by mutableStateOf(NewAnalysisState())
        private set

    fun onAction(action: NewAnalysisAction) {
        when (action) {
            is NewAnalysisAction.UpdateTitle -> state = state.copy(title = action.value, validationMessage = null)
            is NewAnalysisAction.UpdateDateBefore -> state = state.copy(dateBefore = action.value, validationMessage = null)
            is NewAnalysisAction.UpdateDateAfter -> state = state.copy(dateAfter = action.value, validationMessage = null)
            is NewAnalysisAction.ChangeBaseMap -> state = state.copy(mapStyle = action.style)
            is NewAnalysisAction.StartDrawing -> state = state.copy(
                drawingMode = action.mode,
                draftPoints = emptyList(),
                area = null,
                validationMessage = "Нажмите на карту, чтобы ${if (action.mode == DrawMode.RECTANGLE) "задать две противоположные точки" else "добавить вершины полигона"}."
            )
            is NewAnalysisAction.AddMapPoint -> addMapPoint(action.point)
            NewAnalysisAction.RemoveLastPoint -> removeLastPoint()
            NewAnalysisAction.ClearArea -> state = state.copy(area = null, draftPoints = emptyList(), drawingMode = DrawMode.NONE)
            NewAnalysisAction.RunAnalysis -> runAnalysis()
            NewAnalysisAction.DismissResult -> state = state.copy(resultState = null)
        }
    }

    private fun runAnalysis() {
        val selectedArea = state.area
        if (state.title.isBlank() || state.dateBefore.isBlank() || state.dateAfter.isBlank() || selectedArea == null) {
            state = state.copy(validationMessage = "Заполните название, даты и выберите область на карте.")
            return
        }
        val formatter = DateTimeFormatter.ofPattern("dd.MM.uuuu")
        val before = try {
            LocalDate.parse(state.dateBefore, formatter)
        } catch (_: DateTimeParseException) {
            state = state.copy(validationMessage = "Введите дату «до» в формате ДД.ММ.ГГГГ.")
            return
        }
        val after = try {
            LocalDate.parse(state.dateAfter, formatter)
        } catch (_: DateTimeParseException) {
            state = state.copy(validationMessage = "Введите дату «после» в формате ДД.ММ.ГГГГ.")
            return
        }
        if (!after.isAfter(before)) {
            state = state.copy(validationMessage = "Дата «после» должна быть позже даты «до».")
            return
        }
        val request = AnalysisRequest(
            title = state.title,
            area = selectedArea,
            dateBefore = state.dateBefore,
            dateAfter = state.dateAfter
        )
        state = state.copy(resultState = UiState.Loading, validationMessage = null)
        viewModelScope.launch {
            state = state.copy(resultState = analysisRepository.runAnalysis(request).toUiState())
        }
    }

    private fun addMapPoint(point: GeoPoint) {
        if (state.drawingMode == DrawMode.NONE) return
        if (!AmurOblast.contains(point)) {
            state = state.copy(validationMessage = "Можно выбирать точки только в пределах Амурской области.")
            return
        }
        when (state.drawingMode) {
            DrawMode.RECTANGLE -> addRectanglePoint(point)
            DrawMode.POLYGON -> {
                val points = state.draftPoints + point
                state = state.copy(
                    draftPoints = points,
                    area = if (points.size >= 3) AnalysisArea.Polygon(points) else null,
                    validationMessage = when {
                        points.size < 3 -> "Вершин: ${points.size}. Для области нужно минимум три."
                        else -> "Полигон выбран. Можно добавить вершины или перейти к параметрам."
                    }
                )
            }
            DrawMode.NONE -> Unit
        }
    }

    private fun addRectanglePoint(point: GeoPoint) {
        val firstPoint = state.draftPoints.firstOrNull()
        if (firstPoint == null) {
            state = state.copy(draftPoints = listOf(point), validationMessage = "Выберите противоположный угол прямоугольника.")
            return
        }
        val southWest = GeoPoint(
            latitude = minOf(firstPoint.latitude, point.latitude),
            longitude = minOf(firstPoint.longitude, point.longitude)
        )
        val northEast = GeoPoint(
            latitude = maxOf(firstPoint.latitude, point.latitude),
            longitude = maxOf(firstPoint.longitude, point.longitude)
        )
        val corners = listOf(
            southWest,
            GeoPoint(southWest.latitude, northEast.longitude),
            northEast,
            GeoPoint(northEast.latitude, southWest.longitude)
        )
        state = state.copy(
            area = AnalysisArea.BoundingBox(southWest, northEast),
            drawingMode = DrawMode.NONE,
            draftPoints = corners,
            validationMessage = "Прямоугольник выбран."
        )
    }

    private fun removeLastPoint() {
        val points = state.draftPoints.dropLast(1)
        state = state.copy(
            draftPoints = points,
            area = if (state.drawingMode == DrawMode.POLYGON && points.size >= 3) {
                AnalysisArea.Polygon(points)
            } else {
                null
            },
            validationMessage = if (points.isEmpty()) "Область очищена." else "Вершин: ${points.size}."
        )
    }
}
