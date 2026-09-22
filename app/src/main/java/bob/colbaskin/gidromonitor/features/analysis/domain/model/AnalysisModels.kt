package bob.colbaskin.gidromonitor.features.analysis.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

@Serializable
sealed interface AnalysisArea {
    @Serializable
    data class Polygon(val points: List<GeoPoint>) : AnalysisArea

    @Serializable
    data class BoundingBox(
        val southWest: GeoPoint,
        val northEast: GeoPoint
    ) : AnalysisArea
}

@Serializable
data class AnalysisRequest(
    val title: String,
    val area: AnalysisArea,
    val dateBefore: String,
    val dateAfter: String
)

@Serializable
data class AreaMetric(
    val hectares: Double,
    val squareKilometers: Double
)

@Serializable
data class LandCoverShare(
    val name: String,
    val percent: Int
)

@Serializable
data class TimelineMetric(
    val date: String,
    val waterAreaHectares: Double
)

@Serializable
enum class ObservationLayerType {
    WATER_BEFORE,
    WATER_AFTER,
    FLOOD_GAIN,
    WATER_LOSS,
    PERMANENT_WATER,
    SENTINEL_1,
    SENTINEL_2
}

@Serializable
data class ObservationLayer(
    val type: ObservationLayerType,
    val title: String,
    val colorHex: String,
    val geometry: List<GeoPoint>
)

@Serializable
data class AnalysisResult(
    val id: String,
    val request: AnalysisRequest,
    val waterBefore: AreaMetric,
    val waterAfter: AreaMetric,
    val floodedArea: AreaMetric,
    val floodedSharePercent: Double,
    val landCover: List<LandCoverShare>,
    val observations: List<String>,
    val timeline: List<TimelineMetric>,
    val layers: List<ObservationLayer>,
    val status: AnalysisStatus = AnalysisStatus.COMPLETED,
    val hasComputedData: Boolean = true
)

enum class AnalysisStatus {
    PROCESSING,
    COMPLETED,
    FAILED,
    UNKNOWN
}

@Serializable
data class AnalysisHistoryItem(
    val id: String,
    val title: String,
    val dateBefore: String,
    val dateAfter: String,
    val floodedHectares: Double,
    val status: String = "Готово",
    val previewGeometry: List<GeoPoint> = emptyList()
)

@Serializable
data class AnalysisRasterFile(
    val fileName: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val offlineCacheKey: String = fileName
)

fun String.isMergedCogFileName(): Boolean =
    matches(Regex("^merged_.+_cog\\.tif$", RegexOption.IGNORE_CASE))
