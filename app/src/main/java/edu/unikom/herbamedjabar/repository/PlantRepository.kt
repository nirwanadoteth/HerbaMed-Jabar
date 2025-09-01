package edu.unikom.herbamedjabar.repository

import android.app.Application
import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import edu.unikom.herbamedjabar.dao.ScanHistoryDao
import edu.unikom.herbamedjabar.data.ScanHistory
import edu.unikom.herbamedjabar.util.PlantDataParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

data class AnalysisResult(
    val resultText: String,
    val imagePath: String,
    val plantName: String,
    val benefit: String,
    val warning: String,
    val content: String,
    val isHerbal: Boolean,
)

interface PlantRepository {
    suspend fun analyzePlant(bitmap: Bitmap, prompt: String): AnalysisResult
    fun getAllHistory(): Flow<List<ScanHistory>>
    suspend fun deleteHistory(history: ScanHistory)
}

class PlantRepositoryImpl @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val scanHistoryDao: ScanHistoryDao,
    private val application: Application
) : PlantRepository {

    override suspend fun analyzePlant(bitmap: Bitmap, prompt: String): AnalysisResult {
        val maxRetries = 3
        var delayTime = 2000L

        repeat(maxRetries) { attempt ->
            var savedImagePath: String? = null
            try {
                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }
                val response = withTimeout(60_000) { generativeModel.generateContent(inputContent) }

                val resultText = response.text ?: throw Exception("Hasil teks dari AI kosong.")
                val parsedData = PlantDataParser.parsePlantData(resultText)
                // Simpan gambar ke file
                val imagePath = saveBitmapToFile(bitmap).also { savedImagePath = it }

                val plantName = parsedData["plantName"] ?: ""
                val benefit = parsedData["benefit"] ?: ""
                val warning = parsedData["warning"] ?: ""
                val content = parsedData["description"] ?: ""

                // Use parsed herbalStatus from PlantDataParser
                val herbalStatus = parsedData["herbalStatus"]?.lowercase() ?: "unknown"
                val normalized = herbalStatus.replace("-", " ").trim()
                val isHerbal = normalized.equals("herbal", ignoreCase = true)

                // Simpan ke database (sebagai side-effect)
                val history = ScanHistory(
                    resultText = resultText,
                    imagePath = imagePath,
                    plantName = plantName,
                    benefit = benefit,
                    warning = warning,
                    content = content
                )
                try {
                    scanHistoryDao.insertHistory(history)
                } catch (dbEx: Exception) {
                    // Cleanup orphaned image file
                    try {
                        savedImagePath?.let { File(it).delete() }
                    } catch (cleanupEx: Exception) {
                        android.util.Log.e(
                            "PlantRepository",
                            "Failed to delete orphaned image file: $savedImagePath",
                            cleanupEx
                        )
                    }
                    throw dbEx
                }

                // Kembalikan objek AnalysisResult yang dibutuhkan untuk navigasi
                return AnalysisResult(
                    resultText = resultText,
                    imagePath = imagePath,
                    plantName = plantName,
                    benefit = benefit,
                    warning = warning,
                    content = content,
                    isHerbal = isHerbal
                )

            } catch (e: Exception) {
                // Don’t swallow coroutine cancellations – rethrow immediately
                if (e is kotlinx.coroutines.CancellationException) throw e

                // Cleanup orphaned image file before retry
                savedImagePath?.let {
                    try {
                        File(it).delete()
                    } catch (cleanupEx: Exception) {
                        android.util.Log.e(
                            "PlantRepository",
                            "Failed to delete orphaned image file: $savedImagePath",
                            cleanupEx
                        )
                    }
                }
                if (attempt == maxRetries - 1) {
                    throw e
                }
                delay(delayTime)
                delayTime *= 2
            }
        }
        throw IllegalStateException("Gagal menganalisis tanaman setelah beberapa kali percobaan.")
    }

    override fun getAllHistory(): Flow<List<ScanHistory>> {
        return scanHistoryDao.getAllHistory()
    }

    override suspend fun deleteHistory(history: ScanHistory) {
        return scanHistoryDao.deleteHistory(history)
    }

    private suspend fun saveBitmapToFile(bitmap: Bitmap): String {
        return withContext(Dispatchers.IO) {
            val wrapper = application.applicationContext
            val directory = wrapper.getDir("images", android.content.Context.MODE_PRIVATE)
            val file = File(directory, "${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { outputStream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)) {
                    throw IllegalStateException("Gagal menyimpan gambar: kompresi gagal.")
                }
                outputStream.flush()
            }
            file.absolutePath
        }
    }
}
