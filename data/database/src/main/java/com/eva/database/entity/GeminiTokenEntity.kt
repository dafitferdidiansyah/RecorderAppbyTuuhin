@Entity(tableName = "gemini_tokens")
data class GeminiTokenEntity(
	@PrimaryKey(autoGenerate = true) val id: Int = 0,
	@ColumnInfo(name = "api_key") val apiKey: String,
	@ColumnInfo(name = "is_exhausted") val isExhausted: Boolean = false
)