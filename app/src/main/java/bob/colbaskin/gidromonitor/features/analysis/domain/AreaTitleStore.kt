package bob.colbaskin.gidromonitor.features.analysis.domain

interface AreaTitleStore {
    suspend fun save(areaId: String, title: String)
    suspend fun get(areaId: String): String?
}
