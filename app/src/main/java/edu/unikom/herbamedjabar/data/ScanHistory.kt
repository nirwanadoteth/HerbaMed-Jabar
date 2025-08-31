package edu.unikom.herbamedjabar.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "scan_history")
data class ScanHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val resultText: String,
    val imagePath: String,
    val plantName: String = "",
    val content: String = "",
    val benefit: String = "",
    val warning: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable
