package com.eva.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eva.database.dao.RecordingsMetadataDao
import com.eva.utils.GeminiKeyManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class GeminiSummaryWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val keyManager: GeminiKeyManager,
    private val recordingDao: RecordingsMetadataDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val recordingId = inputData.getLong("RECORDING_ID", -1)
        val filePath = inputData.getString("FILE_PATH") // Pastikan ViewModel ngirim path file

        if (recordingId == -1L || filePath == null) return Result.failure()

        var currentKey = keyManager.getActiveKey() ?: run {
            Log.e("GeminiWorker", "No active API keys available.")
            return Result.failure()
        }

        var attempt = 0
        while (attempt < 3) {
            try {
                Log.i("GeminiWorker", "Attempting summary with key: ${currentKey.take(8)}***")

                
                val generativeModel = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = currentKey
                )

                // 2. Siapkan file audio
                val file = File(filePath)
                if (!file.exists()) {
                    Log.e("GeminiWorker", "File not found at $filePath")
                    return Result.failure()
                }
                
                val audioBytes = file.readBytes()

                // 3. Bangun konten (Instruksi + Audio)
                val inputContent = content {
                    // Prompt ditaruh di sini
                    text("Tolong buatkan transkrip lengkap dan rangkuman poin-poin penting dari audio rekaman ini dalam Bahasa Indonesia.")
                    blob("audio/mp4", audioBytes)
                }

                // 4. Panggil API secara asinkron
                val response = generativeModel.generateContent(inputContent)
                val resultText = response.text ?: ""

                if (resultText.isNotEmpty()) {
                    // 5. Simpan hasil ke database
                    recordingDao.updateSummary(recordingId, resultText)
                    Log.i("GeminiWorker", "Summary generation successful.")
                    return Result.success()
                } else {
                    Log.w("GeminiWorker", "Empty response from Gemini.")
                    attempt++
                }

            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                if (errorMsg.contains("429") || errorMsg.contains("Too Many Requests")) {
                    Log.w("GeminiWorker", "Rate limit hit. Rotating key...")
                    keyManager.flagRateLimit(currentKey)
                    currentKey = keyManager.getActiveKey() ?: return Result.failure()
                    attempt++
                } else {
                    Log.e("GeminiWorker", "API Call failed: $errorMsg")
                    // Jika error bukan karena kuota (misal internet putus), suruh WorkManager coba lagi nanti
                    return Result.retry()
                }
            }
        }

        Log.e("GeminiWorker", "Max retries reached.")
        return Result.failure()
    }
}