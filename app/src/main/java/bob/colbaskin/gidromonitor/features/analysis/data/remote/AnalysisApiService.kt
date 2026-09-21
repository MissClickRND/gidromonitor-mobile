package bob.colbaskin.gidromonitor.features.analysis.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path

interface AnalysisApiService {
    @POST("areas")
    suspend fun createArea(@Body request: CreateAreaRequestDto): AreaDto

    @GET("areas/{id}")
    suspend fun getArea(@Path("id") areaId: String): AreaDto

    @GET("areas")
    suspend fun getAreas(): List<AreaDto>

    @GET("storage/analysis/{analysisId}")
    suspend fun getAnalysisFiles(@Path("analysisId") analysisId: String): AnalysisFilesDto
}
