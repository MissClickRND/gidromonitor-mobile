package bob.colbaskin.gidromonitor.features.analysis.data

import bob.colbaskin.gidromonitor.features.analysis.data.remote.AreaDto
import bob.colbaskin.gidromonitor.features.analysis.data.remote.CreateAreaRequestDto
import bob.colbaskin.gidromonitor.features.analysis.data.remote.GeoJsonPolygonDto
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisArea
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisHistoryItem
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisRequest
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisResult
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisStatus
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AreaMetric
import bob.colbaskin.gidromonitor.features.analysis.domain.model.GeoPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val displayDateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu")

fun AnalysisRequest.toCreateAreaRequest(): CreateAreaRequestDto = CreateAreaRequestDto(
    name = title,
    geometry = GeoJsonPolygonDto(coordinates = listOf(area.toBoundaryPoints().toGeoJsonRing())),
    dateBefore = dateBefore.toApiDate(isEndOfDay = false),
    dateAfter = dateAfter.toApiDate(isEndOfDay = true)
)

fun AreaDto.toAnalysisResult(title: String? = null): AnalysisResult {
    val points = geometry.toPoints()
    return AnalysisResult(
        id = id,
        request = AnalysisRequest(
            title = title?.takeIf { it.isNotBlank() } ?: name?.takeIf { it.isNotBlank() } ?: "Территория ${id.take(8)}",
            area = AnalysisArea.Polygon(points),
            dateBefore = dateBefore.toDisplayDate(),
            dateAfter = dateAfter.toDisplayDate()
        ),
        waterBefore = AreaMetric(0.0, 0.0),
        waterAfter = AreaMetric(0.0, 0.0),
        floodedArea = AreaMetric(0.0, 0.0),
        floodedSharePercent = 0.0,
        landCover = emptyList(),
        observations = emptyList(),
        timeline = emptyList(),
        layers = emptyList(),
        status = status.toAnalysisStatus(),
        hasComputedData = false
    )
}

fun AreaDto.toHistoryItem(title: String? = null): AnalysisHistoryItem = AnalysisHistoryItem(
    id = id,
    title = title?.takeIf { it.isNotBlank() } ?: name?.takeIf { it.isNotBlank() } ?: "Территория ${id.take(8)}",
    dateBefore = dateBefore.toDisplayDate(),
    dateAfter = dateAfter.toDisplayDate(),
    floodedHectares = 0.0,
    status = status,
    previewGeometry = geometry.toPoints()
)

fun AreaDto.toAoiGeoJson(): String {
    val ring = geometry.toPoints().toGeoJsonRing()
    val coordinates = ring.joinToString(",") { "[${it[0]},${it[1]}]" }
    return """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{"areaId":"$id","status":"$status"},"geometry":{"type":"Polygon","coordinates":[[$coordinates]]}}]}"""
}

private fun AnalysisArea.toBoundaryPoints(): List<GeoPoint> = when (this) {
    is AnalysisArea.Polygon -> points
    is AnalysisArea.BoundingBox -> listOf(
        southWest,
        GeoPoint(southWest.latitude, northEast.longitude),
        northEast,
        GeoPoint(northEast.latitude, southWest.longitude)
    )
}

private fun GeoJsonPolygonDto.toPoints(): List<GeoPoint> = coordinates.firstOrNull().orEmpty()
    .mapNotNull { position ->
        val longitude = position.getOrNull(0) ?: return@mapNotNull null
        val latitude = position.getOrNull(1) ?: return@mapNotNull null
        GeoPoint(latitude = latitude, longitude = longitude)
    }
    .let { points -> if (points.size > 1 && points.first() == points.last()) points.dropLast(1) else points }

private fun List<GeoPoint>.toGeoJsonRing(): List<List<Double>> {
    val ring = map { listOf(it.longitude, it.latitude) }
    return if (ring.isNotEmpty() && ring.first() != ring.last()) ring + listOf(ring.first()) else ring
}

private fun String.toApiDate(isEndOfDay: Boolean): String = runCatching {
    val date = LocalDate.parse(this, displayDateFormat)
    if (isEndOfDay) "${date}T23:59:59.999Z" else "${date}T00:00:00.000Z"
}.getOrDefault(this)

private fun String.toDisplayDate(): String = runCatching { LocalDate.parse(this.take(10)).format(displayDateFormat) }.getOrDefault(this)

private fun String.toAnalysisStatus(): AnalysisStatus = when (lowercase()) {
    "processing", "pending" -> AnalysisStatus.PROCESSING
    "completed", "ready", "done" -> AnalysisStatus.COMPLETED
    "failed", "error" -> AnalysisStatus.FAILED
    else -> AnalysisStatus.UNKNOWN
}
