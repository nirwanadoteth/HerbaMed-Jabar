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
        var lastError: Exception? = null
        repeat(maxRetries) {
            val result = runCatching {
                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }
                val response = withTimeout(60_000) { generativeModel.generateContent(inputContent) }
                val resultText = response.text ?: throw Exception("Hasil teks dari AI kosong.")
                val parsedData = PlantDataParser.parsePlantData(resultText)
                val imagePath = saveBitmapToFile(bitmap)
                val isHerbal = parsedData.herbalStatus.lowercase().replace(
                    "-",
                    " "
                ).trim().equals("herbal", ignoreCase = true)
                val history = ScanHistory(
                    resultText = resultText,
                    imagePath = imagePath,
                    plantName = parsedData.plantName,
                    benefit = parsedData.benefit,
                    warning = parsedData.warning,
                    content = parsedData.description
                )
                scanHistoryDao.insertHistory(history)
                AnalysisResult(
                    resultText = resultText,
                    imagePath = imagePath,
                    plantName = parsedData.plantName,
                    benefit = parsedData.benefit,
                    warning = parsedData.warning,
                    content = parsedData.description,
                    isHerbal = isHerbal
                )
            }
            result.onSuccess { return it }
            result.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                lastError = e as? Exception
            }
            delay(delayTime)
            delayTime *= 2
        }
        throw lastError ?: IllegalStateException("Gagal menganalisis tanaman setelah beberapa kali percobaan.")
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
