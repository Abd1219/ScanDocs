package com.abdapps.scandocs.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.abdapps.scandocs.data.converter.Converters
import com.abdapps.scandocs.data.dao.ScannedDocumentDao
import com.abdapps.scandocs.data.entity.ScannedDocument

/**
 * Base de datos principal de la aplicación ScanDocs
 * 
 * Esta base de datos utiliza Room para almacenar:
 * - Documentos escaneados
 * - Metadatos de archivos
 * - Información de historial
 * 
 * Características:
 * - Migraciones automáticas
 * - Conversores personalizados
 * - Acceso asíncrono
 * - Backup automático
 */
@Database(
    entities = [ScannedDocument::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ScanDocsDatabase : RoomDatabase() {
    
    /**
     * DAO para acceder a documentos escaneados
     */
    abstract fun scannedDocumentDao(): ScannedDocumentDao
    
    companion object {
        @Volatile
        private var INSTANCE: ScanDocsDatabase? = null
        
        /**
         * Obtiene la instancia de la base de datos
         * 
         * @param context Contexto de la aplicación
         * @return Instancia única de la base de datos
         */
        fun getDatabase(context: Context): ScanDocsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScanDocsDatabase::class.java,
                    "scandocs_database"
                )
                .fallbackToDestructiveMigration() // En producción, implementar migraciones reales
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
