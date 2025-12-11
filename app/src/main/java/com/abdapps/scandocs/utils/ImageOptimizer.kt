package com.abdapps.scandocs.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Utilidad para optimizar imágenes escaneadas
 * 
 * Esta clase proporciona métodos para:
 * - Comprimir imágenes manteniendo calidad
 * - Generar thumbnails eficientemente
 * - Corregir orientación automáticamente
 * - Optimizar memoria durante el procesamiento
 */
class ImageOptimizer(private val context: Context) {
    
    companion object {
        private const val MAX_IMAGE_SIZE = 2048
        private const val THUMBNAIL_SIZE = 200
        private const val COMPRESSION_QUALITY = 85
        private const val THUMBNAIL_QUALITY = 70
    }
    
    /**
     * Comprime una imagen manteniendo la calidad visual
     * @param uri URI de la imagen original
     * @param maxSize Tamaño máximo en píxeles (por defecto 2048)
     * @return Bitmap optimizado o null si hay error
     */
    suspend fun compressImage(
        uri: Uri, 
        maxSize: Int = MAX_IMAGE_SIZE
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                // Obtener dimensiones sin cargar la imagen completa
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)
                
                // Calcular factor de escala
                val scaleFactor = calculateScaleFactor(
                    options.outWidth, 
                    options.outHeight, 
                    maxSize
                )
                
                // Cargar imagen con escala optimizada
                val scaledOptions = BitmapFactory.Options().apply {
                    inSampleSize = scaleFactor
                    inJustDecodeBounds = false
                    inPreferredConfig = Bitmap.Config.RGB_565 // Menos memoria
                }
                
                context.contentResolver.openInputStream(uri)?.use { scaledStream ->
                    val bitmap = BitmapFactory.decodeStream(scaledStream, null, scaledOptions)
                    bitmap?.let { correctOrientation(it, uri) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Genera un thumbnail optimizado
     * @param uri URI de la imagen original
     * @param size Tamaño del thumbnail (por defecto 200px)
     * @return Bitmap del thumbnail o null si hay error
     */
    suspend fun createThumbnail(
        uri: Uri, 
        size: Int = THUMBNAIL_SIZE
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)
                
                val scaleFactor = calculateScaleFactor(
                    options.outWidth, 
                    options.outHeight, 
                    size
                )
                
                val thumbnailOptions = BitmapFactory.Options().apply {
                    inSampleSize = scaleFactor
                    inJustDecodeBounds = false
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                
                context.contentResolver.openInputStream(uri)?.use { thumbnailStream ->
                    BitmapFactory.decodeStream(thumbnailStream, null, thumbnailOptions)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Comprime un bitmap a ByteArray para almacenamiento
     * @param bitmap Bitmap a comprimir
     * @param quality Calidad de compresión (0-100)
     * @return ByteArray comprimido
     */
    fun compressBitmapToByteArray(
        bitmap: Bitmap, 
        quality: Int = COMPRESSION_QUALITY
    ): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }
    
    /**
     * Calcula el factor de escala óptimo para redimensionar
     * @param width Ancho original
     * @param height Alto original
     * @param maxSize Tamaño máximo deseado
     * @return Factor de escala (potencia de 2)
     */
    private fun calculateScaleFactor(width: Int, height: Int, maxSize: Int): Int {
        var scaleFactor = 1
        val maxDimension = max(width, height)
        
        while (maxDimension / scaleFactor > maxSize) {
            scaleFactor *= 2
        }
        
        return scaleFactor
    }
    
    /**
     * Corrige la orientación de la imagen basándose en EXIF
     * @param bitmap Bitmap original
     * @param uri URI de la imagen para leer EXIF
     * @return Bitmap con orientación corregida
     */
    private suspend fun correctOrientation(bitmap: Bitmap, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                }
                
                if (!matrix.isIdentity) {
                    val rotatedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                    )
                    if (rotatedBitmap != bitmap) {
                        bitmap.recycle() // Liberar memoria del bitmap original
                    }
                    rotatedBitmap
                } else {
                    bitmap
                }
            } ?: bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }
    
    /**
     * Libera recursos de bitmaps no utilizados
     * @param bitmaps Lista de bitmaps a reciclar
     */
    fun recycleBitmaps(vararg bitmaps: Bitmap?) {
        bitmaps.forEach { bitmap ->
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
    
    /**
     * Calcula el tamaño en memoria de un bitmap
     * @param bitmap Bitmap a analizar
     * @return Tamaño en bytes
     */
    fun getBitmapSize(bitmap: Bitmap): Long {
        return bitmap.allocationByteCount.toLong()
    }
    
    /**
     * Verifica si hay suficiente memoria para procesar una imagen
     * @param width Ancho de la imagen
     * @param height Alto de la imagen
     * @return true si hay suficiente memoria
     */
    fun hasEnoughMemory(width: Int, height: Int): Boolean {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val availableMemory = maxMemory - usedMemory
        
        // Estimar memoria necesaria (4 bytes por píxel para ARGB_8888)
        val estimatedSize = width * height * 4L
        
        return availableMemory > estimatedSize * 2 // Factor de seguridad
    }
}