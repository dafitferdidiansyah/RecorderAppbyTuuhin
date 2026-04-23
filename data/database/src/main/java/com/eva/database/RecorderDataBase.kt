package com.eva.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.eva.database.convertors.LocalDateTimeConvertors
import com.eva.database.convertors.LocalTimeConvertors
import com.eva.database.dao.GeminiTokenDao
import com.eva.database.dao.RecordingCategoryDao
import com.eva.database.dao.RecordingsBookmarkDao
import com.eva.database.dao.RecordingsMetadataDao
import com.eva.database.dao.TrashFileDao
import com.eva.database.entity.GeminiTokenEntity
import com.eva.database.entity.RecordingBookMarkEntity
import com.eva.database.entity.RecordingCategoryEntity
import com.eva.database.entity.RecordingsMetaDataEntity
import com.eva.database.entity.TrashFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

@Database(
	entities = [
		TrashFileEntity::class,
		RecordingsMetaDataEntity::class,
		RecordingCategoryEntity::class,
		RecordingBookMarkEntity::class,
		GeminiTokenEntity::class // TAMBAHAN: Daftarkan entitas token AI
	],
	version = 7, // NAIKKAN VERSI: Dari 6 ke 7
	exportSchema = true,
	autoMigrations = [
		AutoMigration(from = 1, to = 2),
		AutoMigration(from = 2, to = 3),
		AutoMigration(from = 3, to = 4),
		AutoMigration(from = 4, to = 5),
	]
)
@TypeConverters(
	value = [
		LocalDateTimeConvertors::class,
		LocalTimeConvertors::class,
	],
)
abstract class RecorderDataBase : RoomDatabase() {

	abstract fun trashMetadataEntityDao(): TrashFileDao

	abstract fun categoriesDao(): RecordingCategoryDao

	abstract fun recordingMetaData(): RecordingsMetadataDao

	abstract fun recordingBookMarkDao(): RecordingsBookmarkDao

	abstract fun geminiTokenDao(): GeminiTokenDao // TAMBAHAN: Fungsi DAO Token

	companion object {

		@Volatile
		private var instance: RecorderDataBase? = null

		private val localDateTimeConvertor = LocalDateTimeConvertors()
		private val localtimeConvertor = LocalTimeConvertors()

		// TAMBAHAN: Skrip Migrasi Manual 6 -> 7
		private val MIGRATION_6_7 = object : Migration(6, 7) {
			override fun migrate(db: SupportSQLiteDatabase) {
				// 1. Tambah kolom ai_summary ke tabel rekaman yang sudah ada
				db.execSQL("ALTER TABLE recordings_meta_data ADD COLUMN ai_summary TEXT NOT NULL DEFAULT ''")
				
				// 2. Buat tabel baru untuk menyimpan token AI
				db.execSQL("""
					CREATE TABLE IF NOT EXISTS gemini_tokens (
						id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
						api_key TEXT NOT NULL,
						is_exhausted INTEGER NOT NULL DEFAULT 0
					)
				""".trimIndent())
			}
		}

		fun createDataBase(context: Context): RecorderDataBase {
			return instance ?: synchronized(this) {
				Room.databaseBuilder(
					context,
					RecorderDataBase::class.java,
					DataBaseConstants.DATABASE_NAME
				)
					.addTypeConverter(localtimeConvertor)
					.addTypeConverter(localDateTimeConvertor)
					.addMigrations(DBMigrations.MIGRATE_5_6)
					.addMigrations(MIGRATION_6_7) // TAMBAHKAN INI
					.setQueryExecutor(Dispatchers.IO.asExecutor())
					.build()
					.also { db -> instance = db }
			}
		}

		fun createInMemoryDatabase(context: Context): RecorderDataBase {
			return Room.inMemoryDatabaseBuilder(context, RecorderDataBase::class.java)
				.addTypeConverter(localtimeConvertor)
				.addTypeConverter(localDateTimeConvertor)
				.addMigrations(DBMigrations.MIGRATE_5_6)
				.addMigrations(MIGRATION_6_7) // TAMBAHKAN INI
				.setQueryExecutor(Dispatchers.IO.asExecutor())
				.build()
		}
	}
}