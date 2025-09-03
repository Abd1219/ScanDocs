package com.abdapps.scandocs.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Entidad que representa un documento escaneado en la base de datos
 * 
 * Esta entidad almacena toda la información necesaria para:
 * - Identificar el documento
 * - Mostrar en el historial
 * - Acceder a los archivos generados
 * - Compartir documentos
 * 
 * @param id Identificador único del documento (autogenerado)
 * @param fileName Nombre personalizado del archivo
 * @param originalFileName Nombre original del archivo
 * @param jpgFilePath Ruta al archivo JPG guardado
 * @param pdfFilePath Ruta al archivo PDF guardado
 * @param pageCount Número de páginas del documento
 * @param fileSize Tamaño del archivo en bytes
 * @param scanDate Fecha y hora del escaneo
 * @param isFavorite Indica si el documento está marcado como favorito
 * @param tags Etiquetas opcionales para organizar documentos
 */
@Entity(tableName = "scanned_documents")
data class ScannedDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val fileName: String,
    val originalFileName: String,
    val jpgFilePath: String,
    val pdfFilePath: String,
    val pageCount: Int,
    val fileSize: Long,
    val scanDate: LocalDateTime = LocalDateTime.now(),
    val isFavorite: Boolean = false,
    val tags: String = ""
)
