package com.abdapps.scandocs.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Sistema de cache inteligente para imágenes
 * 
 * Implementa un cache de dos niveles:
 * - Memoria: Cache LRU para acceso rápido
 * - Disco: Almacenamiento persistente para imágenes procesadas
 * 
 * Características:
 * - Gestión automática de memoria
 * - Limpieza automática de cache antiguo
 * - Compresión optimizada para almacenamiento
 */
class ImageCache(private val context: Context) {
    
    companion object {
        private const val MEMORY_CACHE_SIZE = 1024 * 1024 * 10 // 10MB
        private const val DISK_CACHE_SIZE = 1024 * 1024 * 50L // 50MB
        private const val CACHE_DIR_NAME = "image_cache"
        private const val MAX_CACHE_AGE_DAYS = 7
    }
    
    // Cache en memoria usando LRU
    private val memoryCache = object : LruCache<String, Bitmap>(MEMORY_CACHE_SIZE) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.allocationByteCount
        }
        
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            // No reciclar automáticamente, puede estar en uso
        }
    }
    
    // Directorio de cache en disco
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * Obtiene un bitmap del cache
     * @param key Clave única para la imagen
     * @return Bitmap si está en cache, null si no existe
     */
    suspend fun getBitmap(key: String): Bitmap? {
        // Primero buscar en memoria
        memoryCache.get(key)?.let { return it }
        
        // Luego buscar en disco
        return loadFromDisk(key)?.also { bitmap ->
            // Guardar en memoria para próximos accesos
            memoryCache.put(key, bitmap)
        }
    }
    
    /**
     * Guarda un bitmap en el cache
     * @param key Clave única para la imagen
     * @param bitmap Bitmap a guardar
     */
    suspend fun putBitmap(key: String, bitmap: Bitmap) {
        // Guardar en memoria
        memoryCache.put(key, bitmap)
        
        // Guardar en disco de forma asíncrona
        saveToDisk(key, bitmap)
    }
    
    /**
     * Genera una clave única para una imagen
     * @param uri URI de la imagen
     * @param size Tamaño procesado (opcional)
     * @return Clave hash única
     */
    fun generateKey(uri: String, size: Int? = null): String {
        val input = if (size != null) "${uri}_$size" else uri
        return hashString(input)
    }
    
    /**
     * Limpia el cache de memoria
     */
    fun clearMemoryCache() {
        memoryCache.evictAll()
    }
    
    /**
     * Limpia el cache de disco
     */
    suspend fun clearDiskCache() = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.forEach { file ->
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Limpia archivos antiguos del cache
     */
    suspend fun cleanOldCache() = withContext(Dispatchers.IO) {
        try {
            val maxAge = System.currentTimeMillis() - (MAX_CACHE_AGE_DAYS * 24 * 60 * 60 * 1000L)
            
            cacheDir.listFiles()?.forEach { file ->
                if (file.lastModified() < maxAge) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Obtiene el tamaño actual del cache en disco
     * @return Tamaño en bytes
     */
    suspend fun getDiskCacheSize(): Long = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Verifica si el cache de disco excede el límite
     */
    suspend fun isDiskCacheFull(): Boolean {
        return getDiskCacheSize() > DISK_CACHE_SIZE
    }
    
    /**
     * Limpia el cache de disco si está lleno
     */
    suspend fun cleanDiskCacheIfNeeded() {
        if (isDiskCacheFull()) {
            // Eliminar archivos más antiguos hasta estar bajo el límite
            withContext(Dispatchers.IO) {
                try {
                    val files = cacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return@withContext
                    var currentSize = files.sumOf { it.length() }
                    
                    for (file in files) {
                        if (currentSize <= DISK_CACHE_SIZE * 0.8) break // Dejar 20% de margen
                        
                        currentSize -= file.length()
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    /**
     * Carga un bitmap desde el cache de disco
     */
    private suspend fun loadFromDisk(key: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, key)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Guarda un bitmap en el cache de disco
     */
    private suspend fun saveToDisk(key: String, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, key)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            }
            
            // Limpiar cache si es necesario
            cleanDiskCacheIfNeeded()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Genera un hash MD5 para una cadena
     */
    private fun hashString(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Obtiene estadísticas del cache
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            memorySize = memoryCache.size(),
            memoryMaxSize = memoryCache.maxSize(),
            memoryHitCount = memoryCache.hitCount().toLong(),
            memoryMissCount = memoryCache.missCount().toLong()
        )
    }
}

/**
 * Estadísticas del cache para monitoreo
 */
data class CacheStats(
    val memorySize: Int,
    val memoryMaxSize: Int,
    val memoryHitCount: Long,
    val memoryMissCount: Long
) {
    val hitRate: Float
        get() = if (memoryHitCount + memoryMissCount > 0) {
            memoryHitCount.toFloat() / (memoryHitCount + memoryMissCount)
        } else 0f
}