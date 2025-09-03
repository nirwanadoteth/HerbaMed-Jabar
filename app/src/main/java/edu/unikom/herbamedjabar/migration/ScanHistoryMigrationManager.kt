package edu.unikom.herbamedjabar.migration

import android.content.Context
import androidx.core.content.edit
import androidx.room.withTransaction
import edu.unikom.herbamedjabar.dao.ScanHistoryDao
import edu.unikom.herbamedjabar.db.AppDatabase
import edu.unikom.herbamedjabar.util.PlantDataParser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Singleton
class ScanHistoryMigrationManager
@Inject
constructor(private val scanHistoryDao: ScanHistoryDao, private val db: AppDatabase) {
    companion object {
        private const val PAGE_SIZE = 200
        private const val TAG = "ScanHistoryMigration"
    }

    private val scope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private val started = java.util.concurrent.atomic.AtomicBoolean(false)

    fun runMigrationIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("migration_prefs", Context.MODE_PRIVATE)
        val migrated = prefs.getBoolean("scan_history_migrated_v2", false)
        if (migrated) return
        if (!started.compareAndSet(false, true)) return

        scope.launch {
            try {
                var offset = 0
                while (true) {
                    val page = scanHistoryDao.getHistoryPage(limit = PAGE_SIZE, offset = offset)
                    if (page.isEmpty()) break
                    db.withTransaction {
                        for (history in page) {
                            val parsed = PlantDataParser.parsePlantData(history.resultText)
                            scanHistoryDao.updateHistory(
                                history.copy(
                                    plantName = parsed.plantName,
                                    content = parsed.description,
                                    benefit = parsed.benefit,
                                    warning = parsed.warning,
                                )
                            )
                        }
                    }
                    offset += page.size
                }
                prefs.edit { putBoolean("scan_history_migrated_v2", true) }
                android.util.Log.i(TAG, "v2 migration completed")
            } catch (e: android.database.sqlite.SQLiteException) {
                android.util.Log.e(TAG, "v2 migration failed (sqlite)", e)
            } catch (e: IllegalArgumentException) {
                android.util.Log.e(TAG, "v2 migration failed (illegal argument)", e)
            } finally {
                started.set(false)
            }
        }
    }
}
