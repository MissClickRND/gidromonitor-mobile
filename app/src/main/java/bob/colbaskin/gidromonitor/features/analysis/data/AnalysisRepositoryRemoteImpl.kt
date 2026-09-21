package bob.colbaskin.gidromonitor.features.analysis.data

import bob.colbaskin.gidromonitor.common.ApiResult
import bob.colbaskin.gidromonitor.common.utils.safeApiCall
import bob.colbaskin.gidromonitor.features.analysis.data.local.CachedAreaEntity
import bob.colbaskin.gidromonitor.features.analysis.data.local.CachedRasterFileEntity
import bob.colbaskin.gidromonitor.features.analysis.data.local.OfflineAreaDatabase
import bob.colbaskin.gidromonitor.features.analysis.data.local.PendingAreaRequestEntity
import bob.colbaskin.gidromonitor.features.analysis.data.remote.AnalysisApiService
import bob.colbaskin.gidromonitor.features.analysis.data.remote.AreaDto
import bob.colbaskin.gidromonitor.features.analysis.data.remote.CreateAreaRequestDto
import bob.colbaskin.gidromonitor.features.analysis.data.sync.AreaSyncScheduler
import bob.colbaskin.gidromonitor.features.analysis.domain.AnalysisRepository
import bob.colbaskin.gidromonitor.features.analysis.domain.AreaTitleStore
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisRequest
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisResult
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisRasterFile
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisHistoryItem
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

class AnalysisRepositoryRemoteImpl @Inject constructor(
    private val api: AnalysisApiService,
    private val areaTitleStore: AreaTitleStore,
    private val database: OfflineAreaDatabase,
    private val syncScheduler: AreaSyncScheduler,
    @Named("apiUrl") private val apiUrl: String
) : AnalysisRepository {
    private val areaDao get() = database.areaDao()
    private val pendingDao get() = database.pendingRequestDao()
    private val rasterFileDao get() = database.rasterFileDao()
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    override suspend fun runAnalysis(request: AnalysisRequest): ApiResult<AnalysisResult> {
        val localId = "local-${UUID.randomUUID()}"
        val apiRequest = request.toCreateAreaRequest()
        val payload = json.encodeToString(CreateAreaRequestDto.serializer(), apiRequest)
        val localArea = AreaDto(
            id = localId, name = request.title, geometry = apiRequest.geometry,
            dateBefore = apiRequest.dateBefore, dateAfter = apiRequest.dateAfter, status = "queued"
        )
        areaDao.upsert(localArea.toCachedEntity(serverId = null))
        pendingDao.upsert(PendingAreaRequestEntity(localId = localId, payload = payload))
        syncScheduler.scheduleImmediate()

        return when (val sync = sendPending(localId)) {
            is ApiResult.Success -> sync
            is ApiResult.Error -> if (sync.title in setOf("Нет соединения", "Превышено время ожидания")) {
                ApiResult.Success(localArea.toAnalysisResult(request.title))
            } else {
                pendingDao.delete(localId)
                areaDao.deleteArea(localId)
                sync
            }
        }
    }

    override suspend fun getAnalysis(analysisId: String): ApiResult<AnalysisResult> {
        val cachedEntity = areaDao.area(analysisId)
        val cached = cachedEntity?.toAreaDtoOrNull()
        val serverId = cachedEntity?.serverId ?: analysisId
        return when (val remote = safeApiCall(apiCall = { api.getArea(serverId) }, successHandler = { it })) {
            is ApiResult.Success -> {
                cache(remote.data, localId = cachedEntity?.id)
                ApiResult.Success(
                    remote.data.toAnalysisResult(areaTitleStore.get(remote.data.id)).copy(id = analysisId)
                )
            }
            is ApiResult.Error -> cached?.let { ApiResult.Success(it.toAnalysisResult()) } ?: remote
        }
    }

    override suspend fun exportGeoJson(analysisId: String): ApiResult<String> {
        val cachedEntity = areaDao.area(analysisId)
        val cached = cachedEntity?.toAreaDtoOrNull()
        return when (val remote = safeApiCall(
            apiCall = { api.getArea(cachedEntity?.serverId ?: analysisId) },
            successHandler = { it }
        )) {
            is ApiResult.Success -> { cache(remote.data, cachedEntity?.id); ApiResult.Success(remote.data.toAoiGeoJson()) }
            is ApiResult.Error -> cached?.let { ApiResult.Success(it.toAoiGeoJson()) } ?: remote
        }
    }

    override suspend fun getHistory(): ApiResult<List<AnalysisHistoryItem>> {
        val cachedEntities = areaDao.areas()
        val cached = cachedEntities
            .filterNot { it.id.startsWith("local-") && it.serverId != null }
            .mapNotNull { it.toAreaDtoOrNull() }
        return when (val remote = safeApiCall(apiCall = { api.getAreas() }, successHandler = { it })) {
            is ApiResult.Success -> {
                for (area in remote.data) cache(area)
                val queued = cachedEntities
                    .filter { it.id.startsWith("local-") && it.serverId == null }
                    .mapNotNull { it.toAreaDtoOrNull() }
                ApiResult.Success((queued + remote.data).map { it.toHistoryItem() })
            }
            is ApiResult.Error -> if (cached.isNotEmpty()) {
                ApiResult.Success(cached.map { it.toHistoryItem() })
            } else remote
        }
    }

    override fun observeHistory(): Flow<List<AnalysisHistoryItem>> = areaDao.observeAreas().map { entities ->
        entities
            .filterNot { it.id.startsWith("local-") && it.serverId != null }
            .mapNotNull { it.toAreaDtoOrNull()?.toHistoryItem() }
    }

    override fun observeAnalysis(analysisId: String): Flow<AnalysisResult?> =
        areaDao.observeArea(analysisId).map { entity -> entity?.toAreaDtoOrNull()?.toAnalysisResult()?.copy(id = analysisId) }

    override suspend fun getAnalysisRasterFiles(analysisId: String): ApiResult<List<AnalysisRasterFile>> {
        val remote = safeApiCall(
            apiCall = { api.getAnalysisFiles(analysisId) },
            successHandler = { response ->
                response.files
                    .filter { it.fileName.endsWith(".tif", ignoreCase = true) || it.fileName.endsWith(".tiff", ignoreCase = true) }
                    .map { file -> AnalysisRasterFile(
                        fileName = file.fileName,
                        sizeBytes = file.size,
                        downloadUrl = resolveDownloadUrl(file.url),
                        offlineCacheKey = "$analysisId/${file.fileName}"
                    ) }
            }
        )
        return when (remote) {
            is ApiResult.Success -> {
                rasterFileDao.deleteForAnalysis(analysisId)
                rasterFileDao.upsertAll(remote.data.map { it.toCachedEntity(analysisId) })
                remote
            }
            is ApiResult.Error -> getCachedAnalysisRasterFiles(analysisId)
                .takeIf { it.isNotEmpty() }
                ?.let { ApiResult.Success(it) }
                ?: remote
        }
    }

    override suspend fun getCachedAnalysisRasterFiles(analysisId: String): List<AnalysisRasterFile> =
        rasterFileDao.files(analysisId).map { it.toDomain() }

    private fun resolveDownloadUrl(url: String): String =
        url.toHttpUrlOrNull()?.toString() ?: apiUrl.toHttpUrl().resolve(url)?.toString() ?: url

    suspend fun syncPendingAndRefresh(): Boolean {
        for (item in pendingDao.pending()) {
            if (sendPending(item.localId, item.payload) is ApiResult.Error) return false
        }
        return when (val refresh = safeApiCall(apiCall = { api.getAreas() }, successHandler = { it })) {
            is ApiResult.Success -> {
                for (area in refresh.data) cache(area)
                true
            }
            is ApiResult.Error -> false
        }
    }

    private suspend fun sendPending(localId: String, persistedPayload: String? = null): ApiResult<AnalysisResult> {
        val payload = persistedPayload ?: pendingDao.pending().firstOrNull { it.localId == localId }?.payload
            ?: return ApiResult.Error("Запрос не найден", "Нет сохранённого запроса для синхронизации.")
        val request = runCatching { json.decodeFromString(CreateAreaRequestDto.serializer(), payload) }
            .getOrElse { return ApiResult.Error("Повреждённый запрос", "Не удалось прочитать сохранённый запрос.") }
        return when (val remote = safeApiCall(apiCall = { api.createArea(request) }, successHandler = { it })) {
            is ApiResult.Success -> {
                areaTitleStore.save(remote.data.id, request.name)
                cache(remote.data, localId)
                pendingDao.delete(localId)
                ApiResult.Success(remote.data.toAnalysisResult(request.name))
            }
            is ApiResult.Error -> remote
        }
    }

    private suspend fun cache(area: AreaDto, localId: String? = null) = areaDao.upsert(
        area.toCachedEntity(id = localId ?: area.id, serverId = area.id)
    )

    private fun AreaDto.toCachedEntity(id: String = this.id, serverId: String? = this.id) = CachedAreaEntity(
        id = id, serverId = serverId,
        payload = json.encodeToString(AreaDto.serializer(), this)
    )

    private fun CachedAreaEntity.toAreaDtoOrNull(): AreaDto? =
        runCatching { json.decodeFromString(AreaDto.serializer(), payload) }.getOrNull()

    private fun AnalysisRasterFile.toCachedEntity(analysisId: String) = CachedRasterFileEntity(
        analysisId, fileName, sizeBytes, downloadUrl, offlineCacheKey
    )

    private fun CachedRasterFileEntity.toDomain() = AnalysisRasterFile(
        fileName, sizeBytes, downloadUrl, offlineCacheKey
    )
}
