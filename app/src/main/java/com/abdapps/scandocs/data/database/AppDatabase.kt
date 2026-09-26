package com.abdapps.scandocs.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.abdapps.scandocs.data.converter.DateTimeConverters
import com.abdapps.scandocs.data.dao.DocumentDao
import com.abdapps.scandocs.data.entity.DocumentEntity

/**
 * Base de datos principal de la aplicación
 */
@Database(
    entities = [DocumentEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateTimeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun documentDao(): DocumentDao
    
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE documents ADD COLUMN jpgPaths TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scandocs_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
