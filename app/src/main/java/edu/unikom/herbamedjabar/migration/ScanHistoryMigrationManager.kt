package edu.unikom.herbamedjabar.migration

import android.content.Context
import androidx.core.content.edit
import androidx.room.withTransaction
import edu.unikom.herbamedjabar.dao.ScanHistoryDao
import edu.unikom.herbamedjabar.db.AppDatabase
import edu.unikom.herbamedjabar.util.PlantDataParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class ScanHistoryMigrationManager @Inject constructor(
    private val scanHistoryDao: ScanHistoryDao,
    private val db: AppDatabase
) {
    private val scope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)

    fun runMigrationIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("migration_prefs", Context.MODE_PRIVATE)
        val migrated = prefs.getBoolean("scan_history_migrated_v2", false)
        if (migrated) return

        scope.launch {
            try {
                db.withTransaction {
                    val allHistory = scanHistoryDao.getAllHistory().first()
                    for (history in allHistory) {
                        val parsed = PlantDataParser.parsePlantData(history.resultText)
                        val updatedHistory = history.copy(
                            plantName = parsed["plantName"] ?: "",
                            content = parsed["description"] ?: "",
                            benefit = parsed["benefit"] ?: "",
                            warning = parsed["warning"] ?: ""
                        )
                        // updateHistory is suspend, so mark lambda as suspend
                        scanHistoryDao.updateHistory(updatedHistory)
                    }
                }
                prefs.edit { putBoolean("scan_history_migrated_v2", true) }
            } catch (e: Exception) {
                android.util.Log.e("ScanHistoryMigration", "v2 migration failed", e)
            }
        }
    }
}
