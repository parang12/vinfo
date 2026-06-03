package com.example.vinfo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.vinfo.data.local.dao.AlbumDao
import com.example.vinfo.data.local.entity.AlbumEntity

// List<String>을 DB에 저장하기 위한 컨버터
class GenreConverters {
    @TypeConverter
    fun fromString(value: String): List<String> {
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return list.joinToString(",")
    }
}

@Database(entities = [AlbumEntity::class], version = 4, exportSchema = false)
@TypeConverters(GenreConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vinfo_database"
                )
                    .addMigrations(MIGRATION_1_4, MIGRATION_2_4, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateAlbumSchemaTo4(db)
            }
        }

        private val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateAlbumSchemaTo4(db)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateAlbumSchemaTo4(db)
            }
        }

        private fun migrateAlbumSchemaTo4(db: SupportSQLiteDatabase) {
            addColumnIfMissing(db, "album_title", "TEXT NOT NULL DEFAULT ''")
            if (hasColumn(db, "title") && hasColumn(db, "album_title")) {
                db.execSQL("UPDATE albums SET album_title = title WHERE album_title = ''")
            }
            addColumnIfMissing(db, "album", "TEXT")
            addColumnIfMissing(db, "primary_genre", "TEXT")
            addColumnIfMissing(db, "secondary_genre", "TEXT")
            addColumnIfMissing(db, "genre_candidates_json", "TEXT NOT NULL DEFAULT '[]'")
            addColumnIfMissing(db, "genre_source", "TEXT")
            addColumnIfMissing(db, "rym_rating", "REAL")
            addColumnIfMissing(db, "pitchfork_score", "REAL")
            addColumnIfMissing(db, "metacritic_score", "INTEGER")
            addColumnIfMissing(db, "aoty_score", "INTEGER")
            addColumnIfMissing(db, "ratings_json", "TEXT NOT NULL DEFAULT '{}'")
            addColumnIfMissing(db, "critics_summary", "TEXT")
            addColumnIfMissing(db, "interview_summary", "TEXT")
            addColumnIfMissing(db, "listening_guide", "TEXT")
            addColumnIfMissing(db, "samples_used_json", "TEXT NOT NULL DEFAULT '[]'")
            addColumnIfMissing(db, "missing_sources_json", "TEXT NOT NULL DEFAULT '[]'")
            addColumnIfMissing(db, "reliability_notes_json", "TEXT NOT NULL DEFAULT '[]'")
        }

        private fun addColumnIfMissing(
            db: SupportSQLiteDatabase,
            columnName: String,
            definition: String
        ) {
            if (!hasColumn(db, columnName)) {
                db.execSQL("ALTER TABLE albums ADD COLUMN $columnName $definition")
            }
        }

        private fun hasColumn(db: SupportSQLiteDatabase, columnName: String): Boolean {
            db.query("PRAGMA table_info(albums)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == columnName) return true
                }
            }
            return false
        }
    }
}
