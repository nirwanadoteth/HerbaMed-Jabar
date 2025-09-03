package edu.unikom.herbamedjabar.db

import androidx.room.Database
import androidx.room.RoomDatabase
import edu.unikom.herbamedjabar.dao.ScanHistoryDao
import edu.unikom.herbamedjabar.data.ScanHistory

@Database(entities = [ScanHistory::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        val MIGRATION_1_2 =
            object : androidx.room.migration.Migration(1, 2) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
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
                }
            }
    }
}
