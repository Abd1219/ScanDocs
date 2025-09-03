package com.abdapps.scandocs.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale // Added for Locale.ROOT

class FileService(private val context: Context) {

    private val documentsDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        "ScanDocsApp" // Renamed to avoid potential conflicts
    )

    // Ensure minSdk is 26+ or handle older APIs for these date/time features
    @RequiresApi(Build.VERSION_CODES.O)
    private val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    init {
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveJpgFile(imageUri: Uri, fileName: String): String {
        val finalFileName = "${fileName}_${LocalDateTime.now().format(timestampFormatter)}.jpg"
        val jpgFile = File(documentsDir, finalFileName)
        try {
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                FileOutputStream(jpgFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: IOException) {
            throw IOException("Error al guardar JPG: ${e.message}", e)
        }
        return jpgFile.absolutePath
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun savePdfFile(pdfUri: Uri, fileName: String): String {
        val finalFileName = "${fileName}_${LocalDateTime.now().format(timestampFormatter)}.pdf"
        val pdfFile = File(documentsDir, finalFileName)
         try {
            context.contentResolver.openInputStream(pdfUri)?.use { inputStream ->
                FileOutputStream(pdfFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: IOException) {
            throw IOException("Error al guardar PDF: ${e.message}", e)
        }
        return pdfFile.absolutePath
    }

    /**
     * Elimina un archivo de la ruta especificada.
     * @param filePath Ruta absoluta del archivo a eliminar (puede ser null).
     * @return true si el archivo fue eliminado o no existía/path era null/blank, false si hubo error de permisos.
     */
    fun deleteFile(filePath: String?): Boolean {
        // Check for null or blank path first. If so, consider it "deleted" or "nothing to delete".
        if (filePath == null || filePath.isBlank()) {
            return true // Or false depending on desired behavior for blank/null paths
        }
        // At this point, filePath is smart-cast to a non-null String.
        return try {
            val file = File(filePath) // Safe: filePath is non-null here
            if (file.exists()) {
                file.delete()
            } else {
                true // File doesn't exist, so consider it successfully "deleted"
            }
        } catch (_: SecurityException) {
            // Log permission error, e.g., Log.e("FileService", "Permission error deleting $filePath", e)
            false
        }
    }

    private fun getUriForFile(file: File): Uri? {
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider", 
                file
            )
        } catch (e: IllegalArgumentException) {
            // Log error, e.g., Log.e("FileService", "Error getting URI for file: ${file.path}", e)
            null
        }
    }

    /**
     * Crea un Intent para VER un archivo.
     */
    fun createViewIntent(context: Context, filePath: String, mimeType: String? = null): Intent? {
        val file = File(filePath)
        if (!file.exists()) return null

        val contentUri = getUriForFile(file) ?: return null
        val actualMimeType = mimeType ?: determineMimeTypeSafely(file) ?: return null

        return Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(contentUri, actualMimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Crea un Intent para COMPARTIR un archivo.
     */
    fun createShareIntent(context: Context, filePath: String, mimeType: String? = null): Intent? {
        val file = File(filePath)
        if (!file.exists()) return null

        val contentUri = getUriForFile(file) ?: return null
        val actualMimeType = mimeType ?: determineMimeTypeSafely(file) ?: return null

        return Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, contentUri)
            type = actualMimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    // Renamed and ensured Locale.ROOT is used correctly
    private fun determineMimeTypeSafely(file: File): String? {
        val extension = file.extension
        if (extension.isNotEmpty()) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT))
        }
        return null // Or a generic MIME type like "application/octet-stream"
    }

    fun createThumbnail(jpgPath: String): String? {
        return jpgPath // Placeholder, consider actual thumbnail generation
    }
}
