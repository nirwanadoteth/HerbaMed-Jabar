package edu.unikom.herbamedjabar.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import edu.unikom.herbamedjabar.data.ScanHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(scanHistory: ScanHistory)

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ScanHistory>>

    @Delete(entity = ScanHistory::class) suspend fun deleteHistory(vararg scanHistory: ScanHistory)

    @Update(entity = ScanHistory::class) suspend fun updateHistory(scanHistory: ScanHistory)

    @Query("SELECT * FROM scan_history ORDER BY id LIMIT :limit OFFSET :offset")
    suspend fun getHistoryPage(limit: Int, offset: Int): List<ScanHistory>
}
