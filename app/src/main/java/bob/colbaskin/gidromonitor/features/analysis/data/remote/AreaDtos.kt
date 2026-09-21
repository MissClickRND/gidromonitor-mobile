package bob.colbaskin.gidromonitor.features.analysis.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CreateAreaRequestDto(
    val name: String,
    val geometry: GeoJsonPolygonDto,
    val dateBefore: String,
    val dateAfter: String
)

@Serializable
data class AreaDto(
    val id: String,
    val name: String? = null,
    val geometry: GeoJsonPolygonDto = GeoJsonPolygonDto(),
    val dateBefore: String,
    val dateAfter: String,
    val status: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class GeoJsonPolygonDto(
    val type: String = "Polygon",
    val coordinates: List<List<List<Double>>> = emptyList()
)

@Serializable
data class AnalysisFilesDto(
    val analysisId: String,
    val files: List<AnalysisFileDto> = emptyList()
)

@Serializable
data class AnalysisFileDto(
    val fileName: String,
    val size: Long,
    val url: String
)
