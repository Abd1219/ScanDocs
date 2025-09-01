package com.abdapps.scandocs.service

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.abdapps.scandocs.data.entity.ScannedDocument
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio para manejo de archivos
 * 
 * Esta clase se encarga de:
 * - Crear archivos JPG y PDF
 * - Guardar archivos en almacenamiento externo
 * - Generar URIs para compartir archivos
 * - Manejar la organización de archivos
 * - Proporcionar acceso seguro a archivos
 */
@Singleton
class FileService @Inject constructor(
    private val context: Context
) {
    
    // Directorio principal para documentos escaneados
    private val documentsDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        "ScanDocs"
    )
    
    // Formato para nombres de archivo con timestamp
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    
    init {
        // Crear directorio si no existe
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }
    }
    
    /**
     * Guarda un archivo JPG desde un URI
     * 
     * @param imageUri URI de la imagen original
     * @param fileName Nombre personalizado del archivo
     * @return Ruta del archivo guardado
     */
    suspend fun saveJpgFile(imageUri: Uri, fileName: String): String {
        val timestamp = LocalDateTime.now().format(timestampFormatter)
        val jpgFile = File(documentsDir, "${fileName}_${timestamp}.jpg")
        
        context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
            FileOutputStream(jpgFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        
        return jpgFile.absolutePath
    }
    
    /**
     * Guarda un archivo PDF desde un URI
     * 
     * @param pdfUri URI del PDF original
     * @param fileName Nombre personalizado del archivo
     * @return Ruta del archivo guardado
     */
    suspend fun savePdfFile(pdfUri: Uri, fileName: String): String {
        val timestamp = LocalDateTime.now().format(timestampFormatter)
        val pdfFile = File(documentsDir, "${fileName}_${timestamp}.pdf")
        
        context.contentResolver.openInputStream(pdfUri)?.use { inputStream ->
            FileOutputStream(pdfFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        
        return pdfFile.absolutePath
    }
    
    /**
     * Guarda múltiples páginas JPG
     * 
     * @param imageUris Lista de URIs de imágenes
     * @param fileName Nombre base del archivo
     * @return Lista de rutas de archivos guardados
     */
    suspend fun saveMultipleJpgFiles(imageUris: List<Uri>, fileName: String): List<String> {
        val savedPaths = mutableListOf<String>()
        
        imageUris.forEachIndexed { index, uri ->
            val timestamp = LocalDateTime.now().format(timestampFormatter)
            val jpgFile = File(documentsDir, "${fileName}_page${index + 1}_${timestamp}.jpg")
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(jpgFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            savedPaths.add(jpgFile.absolutePath)
        }
        
        return savedPaths
    }
    
    /**
     * Obtiene el URI para compartir un archivo
     * 
     * @param filePath Ruta del archivo
     * @return URI para compartir usando FileProvider
     */
    fun getShareableUri(filePath: String): Uri {
        val file = File(filePath)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    
    /**
     * Obtiene el URI para compartir un documento completo
     * 
     * @param document Documento escaneado
     * @param fileType Tipo de archivo a compartir (JPG o PDF)
     * @return URI para compartir
     */
    fun getDocumentShareableUri(document: ScannedDocument, fileType: String): Uri {
        val filePath = when (fileType.uppercase()) {
            "JPG" -> document.jpgFilePath
            "PDF" -> document.pdfFilePath
            else -> throw IllegalArgumentException("Tipo de archivo no soportado: $fileType")
        }
        
        return getShareableUri(filePath)
    }
    
    /**
     * Calcula el tamaño total de un archivo
     * 
     * @param filePath Ruta del archivo
     * @return Tamaño en bytes
     */
    fun getFileSize(filePath: String): Long {
        val file = File(filePath)
        return if (file.exists()) file.length() else 0L
    }
    
    /**
     * Verifica si un archivo existe
     * 
     * @param filePath Ruta del archivo
     * @return true si el archivo existe
     */
    fun fileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }
    
    /**
     * Elimina un archivo
     * 
     * @param filePath Ruta del archivo a eliminar
     * @return true si se eliminó correctamente
     */
    fun deleteFile(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) file.delete() else false
    }
    
    /**
     * Obtiene estadísticas del directorio de documentos
     * 
     * @return Pair con (número de archivos, tamaño total)
     */
    fun getDirectoryStats(): Pair<Int, Long> {
        if (!documentsDir.exists()) return Pair(0, 0L)
        
        var fileCount = 0
        var totalSize = 0L
        
        documentsDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                fileCount++
                totalSize += file.length()
            }
        }
        
        return Pair(fileCount, totalSize)
    }
    
    /**
     * Limpia archivos temporales y obsoletos
     * 
     * @param maxAgeInDays Edad máxima en días para mantener archivos
     */
    fun cleanupOldFiles(maxAgeInDays: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (maxAgeInDays * 24 * 60 * 60 * 1000L)
        
        documentsDir.walkTopDown().forEach { file ->
            if (file.isFile && file.lastModified() < cutoffTime) {
                file.delete()
            }
        }
    }
}
