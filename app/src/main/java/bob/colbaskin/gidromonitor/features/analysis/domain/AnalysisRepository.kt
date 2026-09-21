package bob.colbaskin.gidromonitor.features.analysis.domain

import bob.colbaskin.gidromonitor.common.ApiResult
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisRequest
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisResult
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisHistoryItem
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisRasterFile
import kotlinx.coroutines.flow.Flow

interface AnalysisRepository {
    suspend fun runAnalysis(request: AnalysisRequest): ApiResult<AnalysisResult>
    suspend fun getAnalysis(analysisId: String): ApiResult<AnalysisResult>
    suspend fun exportGeoJson(analysisId: String): ApiResult<String>
    suspend fun getHistory(): ApiResult<List<AnalysisHistoryItem>>
    fun observeHistory(): Flow<List<AnalysisHistoryItem>>
    fun observeAnalysis(analysisId: String): Flow<AnalysisResult?>
    suspend fun getAnalysisRasterFiles(analysisId: String): ApiResult<List<AnalysisRasterFile>>
    suspend fun getCachedAnalysisRasterFiles(analysisId: String): List<AnalysisRasterFile>
}
