package bob.colbaskin.gidromonitor.features.analysis.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val offlineAreaMigration1To2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE cached_areas ADD COLUMN serverId TEXT")
    }
}

val offlineAreaMigration2To3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS cached_raster_files (" +
                "analysisId TEXT NOT NULL, fileName TEXT NOT NULL, sizeBytes INTEGER NOT NULL, " +
                "downloadUrl TEXT NOT NULL, offlineCacheKey TEXT NOT NULL, " +
                "PRIMARY KEY(analysisId, fileName))"
        )
    }
}
