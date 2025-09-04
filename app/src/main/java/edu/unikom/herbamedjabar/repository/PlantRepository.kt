package edu.unikom.herbamedjabar.repository

import android.app.Application
import android.graphics.Bitmap
import androidx.sqlite.SQLiteException
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import edu.unikom.herbamedjabar.dao.ScanHistoryDao
import edu.unikom.herbamedjabar.data.ScanHistory
import edu.unikom.herbamedjabar.util.PlantDataParser
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

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

class PlantRepositoryImpl
@Inject
constructor(
    private val generativeModel: GenerativeModel,
    private val scanHistoryDao: ScanHistoryDao,
    private val application: Application,
) : PlantRepository {

    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 2000L
        private const val AI_TIMEOUT_MS = 60_000L
        private const val COMPRESS_QUALITY = 90
    }

    override suspend fun analyzePlant(bitmap: Bitmap, prompt: String): AnalysisResult {
        var lastError: Throwable? = null
        var delayTime = INITIAL_DELAY_MS
        repeat(MAX_RETRIES) { attempt ->
            var savedImagePath: String? = null
            val result = runCatching {
                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }
                val response =
                    withTimeout(AI_TIMEOUT_MS) { generativeModel.generateContent(inputContent) }
                val resultText = response.text ?: error("Hasil teks dari AI kosong.")
                val parsedData = PlantDataParser.parsePlantData(resultText)
                val imagePath = saveBitmapToFile(bitmap).also { savedImagePath = it }
                val isHerbal =
                    parsedData.herbalStatus
                        .replace("-", " ")
                        .trim()
                        .equals("herbal", ignoreCase = true)
                val history =
                    ScanHistory(
                        resultText = resultText,
                        imagePath = imagePath,
                        plantName = parsedData.plantName,
                        benefit = parsedData.benefit,
                        warning = parsedData.warning,
                        content = parsedData.description,
                    )
                try {
                    scanHistoryDao.insertHistory(history)
                } catch (se: SQLiteException) {
                    savedImagePath?.let { runCatching { File(it).delete() } }
                    throw se
                } catch (iae: IllegalArgumentException) {
                    savedImagePath?.let { runCatching { File(it).delete() } }
                    throw iae
                }
                AnalysisResult(
                    resultText = resultText,
                    imagePath = imagePath,
                    plantName = parsedData.plantName,
                    benefit = parsedData.benefit,
                    warning = parsedData.warning,
                    content = parsedData.description,
                    isHerbal = isHerbal,
                )
            }
            result.onSuccess {
                return it
            }
            result.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException && e !is TimeoutCancellationException) throw e
                savedImagePath?.let { runCatching { File(it).delete() } }
                lastError = e
            }
            if (attempt < MAX_RETRIES - 1) {
                delay(delayTime)
                delayTime = (delayTime * 2).coerceAtMost(AI_TIMEOUT_MS)
            }
        }
        throw lastError ?: error("Gagal menganalisis tanaman setelah beberapa kali percobaan.")
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
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, outputStream)) {
                    "Gagal menyimpan gambar: kompresi gagal."
                }
                outputStream.flush()
            }
            file.absolutePath
        }
    }
}
