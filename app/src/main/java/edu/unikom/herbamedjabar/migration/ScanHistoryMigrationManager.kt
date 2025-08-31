package edu.unikom.herbamedjabar.migration

import android.content.Context
import androidx.core.content.edit
import edu.unikom.herbamedjabar.dao.ScanHistoryDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import edu.unikom.herbamedjabar.util.PlantDataParser
import javax.inject.Inject

class ScanHistoryMigrationManager @Inject constructor(
    private val scanHistoryDao: ScanHistoryDao
) {
    fun runMigrationIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("migration_prefs", Context.MODE_PRIVATE)
        val migrated = prefs.getBoolean("scan_history_migrated_v2", false)
        if (migrated) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allHistory = scanHistoryDao.getAllHistory().first()
                for (history in allHistory) {
                    val parsed = PlantDataParser.parsePlantData(history.resultText)
                    val updatedHistory = history.copy(
                        plantName = parsed["plantName"] ?: "",
                        content = parsed["description"] ?: "",
                        benefit = parsed["benefit"] ?: "",
                        warning = parsed["warning"] ?: ""
                    )
                    scanHistoryDao.updateHistory(updatedHistory)
                }
                prefs.edit { putBoolean("scan_history_migrated_v2", true) }
            } catch (e: Exception) {
                // TODO: Add logging if needed
            }
        }
    }
}
