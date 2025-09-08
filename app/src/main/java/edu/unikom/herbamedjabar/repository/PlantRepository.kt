package edu.unikom.herbamedjabar.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import edu.unikom.herbamedjabar.R
import edu.unikom.herbamedjabar.dao.ScanHistoryDao
import edu.unikom.herbamedjabar.data.ScanHistory
import edu.unikom.herbamedjabar.util.PlantDataParser
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
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
    private val context: Context,
) : PlantRepository {

    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 2000L
        private const val AI_TIMEOUT_MS = 60_000L
        private const val COMPRESS_QUALITY = 90
        private const val MAX_BACKOFF_MS = 60_000L
    }

    override suspend fun analyzePlant(bitmap: Bitmap, prompt: String): AnalysisResult {
        var lastError: Throwable? = null
        var delayTime = INITIAL_DELAY_MS
        repeat(MAX_RETRIES) { attempt ->
            var savedImagePath: String? = null
            val result = runCatching {
                val inputContent = content {
                    image(bitmap.downscaled())
                    text(prompt)
                }
                val response =
                    withTimeout(AI_TIMEOUT_MS) { generativeModel.generateContent(inputContent) }
                val resultText = response.text?.trim().orEmpty()
                check(resultText.isNotBlank()) { context.getString(R.string.ai_text_empty) }
                val parsedData = PlantDataParser.parsePlantData(resultText)
                val imagePath = saveBitmapToFile(bitmap).also { savedImagePath = it }
                val isHerbal = parsedData.isHerbal
                val history =
                    ScanHistory(
                        resultText = resultText,
                        imagePath = imagePath,
                        plantName = parsedData.plantName,
                        benefit = parsedData.benefit,
                        warning = parsedData.warning,
                        content = parsedData.description,
                    )
                scanHistoryDao.insertHistory(history)
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
                savedImagePath?.let { runCatching { File(it).delete() } }
                if (
                    e is kotlinx.coroutines.CancellationException &&
                        e !is kotlinx.coroutines.TimeoutCancellationException
                )
                    throw e
                lastError = e
            }
            if (attempt < MAX_RETRIES - 1) {
                val jitter = (delayTime / 5) // ±20%
                delay(delayTime + kotlin.random.Random.nextLong(-jitter, jitter + 1))
                delayTime = (delayTime * 2).coerceAtMost(MAX_BACKOFF_MS)
            }
        }
        throw lastError ?: error(context.getString(R.string.analysis_failed_after_retries))
    }

    override fun getAllHistory(): Flow<List<ScanHistory>> {
        return scanHistoryDao.getAllHistory()
    }

    override suspend fun deleteHistory(history: ScanHistory) {
        scanHistoryDao.deleteHistory(history)
    }

    private fun Bitmap.downscaled(maxDim: Int = 1280): Bitmap {
        val scale = maxOf(width, height).toFloat() / maxDim
        return if (scale > 1f) {
            val newW = maxOf(1, kotlin.math.round(width / scale).toInt())
            val newH = maxOf(1, kotlin.math.round(height / scale).toInt())
            this.scale(newW, newH, /* filter= */ true)
        } else this
    }

    private suspend fun saveBitmapToFile(bitmap: Bitmap): String {
        return withContext(Dispatchers.IO) {
            val directory = context.getDir("images", Context.MODE_PRIVATE)
            val file = File(directory, "${UUID.randomUUID()}.jpg")
            try {
                FileOutputStream(file).buffered().use { outputStream ->
                    check(
                        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, outputStream)
                    ) {
                        context.getString(R.string.compression_failed)
                    }
                    outputStream.flush()
                }
            } catch (ioe: java.io.IOException) {
                runCatching { file.delete() }
                throw ioe
            } catch (ise: IllegalStateException) {
                runCatching { file.delete() }
                throw ise
            }
            file.absolutePath
        }
    }
}
