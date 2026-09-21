package bob.colbaskin.gidromonitor.features.analysis.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_areas")
data class CachedAreaEntity(
    @androidx.room.PrimaryKey val id: String,
    val payload: String,
    val serverId: String? = null,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "pending_area_requests")
data class PendingAreaRequestEntity(
    @androidx.room.PrimaryKey val localId: String,
    val payload: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_raster_files", primaryKeys = ["analysisId", "fileName"])
data class CachedRasterFileEntity(
    val analysisId: String,
    val fileName: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val offlineCacheKey: String
)

@Dao
interface OfflineAreaDao {
    @Query("SELECT * FROM cached_areas WHERE id = :id LIMIT 1")
    suspend fun area(id: String): CachedAreaEntity?

    @Query("SELECT * FROM cached_areas ORDER BY updatedAtMillis DESC")
    suspend fun areas(): List<CachedAreaEntity>

    @Query("SELECT * FROM cached_areas ORDER BY updatedAtMillis DESC")
    fun observeAreas(): Flow<List<CachedAreaEntity>>

    @Query("SELECT * FROM cached_areas WHERE id = :id LIMIT 1")
    fun observeArea(id: String): Flow<CachedAreaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(area: CachedAreaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(areas: List<CachedAreaEntity>)

    @Query("DELETE FROM cached_areas WHERE id = :id")
    suspend fun deleteArea(id: String)
}

@Dao
interface PendingAreaRequestDao {
    @Query("SELECT * FROM pending_area_requests ORDER BY createdAtMillis")
    suspend fun pending(): List<PendingAreaRequestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(request: PendingAreaRequestEntity)

    @Query("DELETE FROM pending_area_requests WHERE localId = :localId")
    suspend fun delete(localId: String)
}

@Dao
interface CachedRasterFileDao {
    @Query("SELECT * FROM cached_raster_files WHERE analysisId = :analysisId")
    suspend fun files(analysisId: String): List<CachedRasterFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(files: List<CachedRasterFileEntity>)

    @Query("DELETE FROM cached_raster_files WHERE analysisId = :analysisId")
    suspend fun deleteForAnalysis(analysisId: String)
}

@Database(
    entities = [CachedAreaEntity::class, PendingAreaRequestEntity::class, CachedRasterFileEntity::class],
    version = 3,
    exportSchema = true
)
abstract class OfflineAreaDatabase : RoomDatabase() {
    abstract fun areaDao(): OfflineAreaDao
    abstract fun pendingRequestDao(): PendingAreaRequestDao
    abstract fun rasterFileDao(): CachedRasterFileDao
}
