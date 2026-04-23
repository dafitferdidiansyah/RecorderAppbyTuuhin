class GeminiKeyManager @Inject constructor(
    private val tokenDao: GeminiTokenDao
) {
    suspend fun getActiveKey(): String? {
        return tokenDao.getValidKey()
    }

    suspend fun flagRateLimit(failedKey: String) {
        Log.w("GeminiKeyManager", "Rate limit hit for key: ${failedKey.take(8)}***")
        tokenDao.markExhausted(failedKey)
    }
}