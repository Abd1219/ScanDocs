package com.abdapps.scandocs.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
// import java.time.LocalDateTime // No utilizada
import java.util.Date

/**
 * Entidad que representa un documento escaneado en la base de datos
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val jpgPath: String, // Múltiples rutas separadas por comas para documentos multi-página
    val pdfPath: String,
    val createdAt: Date = Date(),
    val thumbnailPath: String? = null
) {
    /**
     * Obtiene la lista de rutas JPG individuales
     */
    fun getJpgPaths(): List<String> {
        return if (jpgPath.isBlank()) {
            emptyList()
        } else {
            jpgPath.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
    
    /**
     * Obtiene la primera imagen JPG (para thumbnail)
     */
    fun getFirstJpgPath(): String? {
        return getJpgPaths().firstOrNull()
    }
    
    /**
     * Obtiene el número de páginas JPG
     */
    fun getPageCount(): Int {
        return getJpgPaths().size
    }
}