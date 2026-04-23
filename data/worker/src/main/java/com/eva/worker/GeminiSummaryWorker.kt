@HiltWorker
class GeminiSummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val keyManager: GeminiKeyManager,
    private val recordingDao: RecordingsMetadataDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val recordingId = inputData.getLong("RECORDING_ID", -1)
        if (recordingId == -1L) return Result.failure()

        var currentKey = keyManager.getActiveKey() ?: run {
            Log.e("GeminiWorker", "No active API keys available.")
            return Result.failure()
        }

        var attempt = 0
        while (attempt < 3) {
            try {
                Log.i("GeminiWorker", "Attempting summary with key: ${currentKey.take(8)}***")
                
                // [Logika pemanggilan Gemini API via Retrofit/SDK]
                // val response = geminiApi.generateContent(currentKey, audioFile, prompt)
                val summaryResult = "Result from API" 

                recordingDao.updateSummary(recordingId, summaryResult)
                Log.i("GeminiWorker", "Summary generation successful.")
                
                return Result.success()

            } catch (e: Exception) {
                if (e.message?.contains("429") == true || e.message?.contains("Too Many Requests") == true) {
                    keyManager.flagRateLimit(currentKey)
                    currentKey = keyManager.getActiveKey() ?: return Result.failure()
                    attempt++
                } else {
                    Log.e("GeminiWorker", "API Call failed: ${e.message}")
                    return Result.retry()
                }
            }
        }
        
        Log.e("GeminiWorker", "Max retries reached.")
        return Result.failure()
    }
}