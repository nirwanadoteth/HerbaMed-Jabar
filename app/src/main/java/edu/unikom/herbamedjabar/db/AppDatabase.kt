package edu.unikom.herbamedjabar.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import edu.unikom.herbamedjabar.dao.ScanHistoryDao
import edu.unikom.herbamedjabar.data.ScanHistory

@Database(entities = [ScanHistory::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        val MIGRATION_1_2: Migration =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE scan_history ADD COLUMN plantName TEXT NOT NULL DEFAULT ''"
                    )
                    db.execSQL(
                        "ALTER TABLE scan_history ADD COLUMN content TEXT NOT NULL DEFAULT ''"
                    )
                    db.execSQL(
                        "ALTER TABLE scan_history ADD COLUMN benefit TEXT NOT NULL DEFAULT ''"
                    )
                    db.execSQL(
                        "ALTER TABLE scan_history ADD COLUMN warning TEXT NOT NULL DEFAULT ''"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_scan_history_plantName ON scan_history(plantName COLLATE NOCASE)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_scan_history_timestamp ON scan_history(timestamp)"
                    )
                }
            }
    }
}
