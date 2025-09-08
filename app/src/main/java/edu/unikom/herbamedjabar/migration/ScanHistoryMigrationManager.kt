package edu.unikom.herbamedjabar.migration

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.room.withTransaction
import edu.unikom.herbamedjabar.dao.ScanHistoryDao
import edu.unikom.herbamedjabar.db.AppDatabase
import edu.unikom.herbamedjabar.util.PlantDataParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Singleton
class ScanHistoryMigrationManager
@Inject
constructor(private val scanHistoryDao: ScanHistoryDao, private val db: AppDatabase) {
    companion object {
        private const val PAGE_SIZE = 200
        private const val TAG = "ScanHistoryMigration"
        private const val MIGRATED_KEY = "scan_history_migrated_v2"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)

    fun runMigrationIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("migration_prefs", Context.MODE_PRIVATE)
        val migrated = prefs.getBoolean(MIGRATED_KEY, false)
        if (migrated) return
        if (!started.compareAndSet(expectedValue = false, newValue = true)) return

        scope.launch {
            try {
                var offset = 0
                while (true) {
                    val page = scanHistoryDao.getHistoryPage(limit = PAGE_SIZE, offset = offset)
                    if (page.isEmpty()) break
                    db.withTransaction {
                        for (history in page) {
                            runCatching {
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
                                .onFailure {
                                    android.util.Log.w(
                                        TAG,
                                        "Skipping a history row due to parse/update error",
                                        it,
                                    )
                                }
                        }
                    }
                    offset += page.size
                }
                prefs.edit { putBoolean(MIGRATED_KEY, true) }
                Log.i(TAG, "v2 migration completed")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: androidx.sqlite.SQLiteException) {
                Log.e(TAG, "v2 migration failed (sqlite)", e)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "v2 migration failed (illegal argument)", e)
            } catch (e: Exception) {
                Log.e(TAG, "v2 migration failed (unexpected)", e)
            } finally {
                started.store(false)
            }
        }
    }
}
