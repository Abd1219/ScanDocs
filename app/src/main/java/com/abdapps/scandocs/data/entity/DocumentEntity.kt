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
    val jpgPath: String,
    val pdfPath: String,
    val createdAt: Date = Date(), // CORREGIDO
    val thumbnailPath: String? = null
)