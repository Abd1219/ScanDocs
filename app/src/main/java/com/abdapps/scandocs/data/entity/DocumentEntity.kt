package com.abdapps.scandocs.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
// import java.time.LocalDateTime // No utilizada
import java.util.Date
import org.json.JSONArray

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
    val thumbnailPath: String? = null,
    /** JSON array with every JPG page path. Empty on legacy rows, which fall back to jpgPath. */
    @ColumnInfo(defaultValue = "''")
    val jpgPaths: String = ""
) {
    val allJpgPaths: List<String>
        get() = runCatching {
            JSONArray(jpgPaths).let { paths ->
                (0 until paths.length()).mapNotNull { paths.optString(it).takeIf(String::isNotBlank) }
            }
        }.getOrDefault(emptyList()).ifEmpty { listOfNotNull(jpgPath.takeIf(String::isNotBlank)) }
}
