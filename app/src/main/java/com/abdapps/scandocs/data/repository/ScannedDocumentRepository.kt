package com.abdapps.scandocs.data.repository

import com.abdapps.scandocs.data.dao.ScannedDocumentDao
import com.abdapps.scandocs.data.entity.ScannedDocument
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio para documentos escaneados
 * 
 * Esta clase actúa como capa intermedia entre la base de datos y el ViewModel:
 * - Proporciona una API limpia para acceder a los datos
 * - Maneja la lógica de negocio relacionada con documentos
 * - Centraliza el acceso a múltiples fuentes de datos
 * - Facilita el testing y mantenimiento
 */
@Singleton
class ScannedDocumentRepository @Inject constructor(
    private val scannedDocumentDao: ScannedDocumentDao
) {
    
    /**
     * Obtiene todos los documentos escaneados
     * 
     * @return Flow con lista de documentos ordenados por fecha
     */
    fun getAllDocuments(): Flow<List<ScannedDocument>> {
        return scannedDocumentDao.getAllDocuments()
    }
    
    /**
     * Obtiene documentos favoritos
     * 
     * @return Flow con lista de documentos favoritos
     */
    fun getFavoriteDocuments(): Flow<List<ScannedDocument>> {
        return scannedDocumentDao.getFavoriteDocuments()
    }
    
    /**
     * Busca documentos por término de búsqueda
     * 
     * @param query Término de búsqueda
     * @return Flow con documentos que coinciden
     */
    fun searchDocuments(query: String): Flow<List<ScannedDocument>> {
        return scannedDocumentDao.searchDocuments(query)
    }
    
    /**
     * Obtiene un documento específico por ID
     * 
     * @param id ID del documento
     * @return Documento encontrado o null
     */
    suspend fun getDocumentById(id: Long): ScannedDocument? {
        return scannedDocumentDao.getDocumentById(id)
    }
    
    /**
     * Obtiene el documento más reciente
     * 
     * @return Documento más reciente o null
     */
    suspend fun getLatestDocument(): ScannedDocument? {
        return scannedDocumentDao.getLatestDocument()
    }
    
    /**
     * Obtiene estadísticas de la base de datos
     * 
     * @return Pair con (total documentos, tamaño total)
     */
    suspend fun getDatabaseStats(): Pair<Int, Long> {
        val count = scannedDocumentDao.getDocumentCount()
        val size = scannedDocumentDao.getTotalSize() ?: 0L
        return Pair(count, size)
    }
    
    /**
     * Inserta un nuevo documento escaneado
     * 
     * @param document Documento a insertar
     * @return ID del documento insertado
     */
    suspend fun insertDocument(document: ScannedDocument): Long {
        return scannedDocumentDao.insertDocument(document)
    }
    
    /**
     * Actualiza un documento existente
     * 
     * @param document Documento a actualizar
     */
    suspend fun updateDocument(document: ScannedDocument) {
        scannedDocumentDao.updateDocument(document)
    }
    
    /**
     * Elimina un documento por ID
     * 
     * @param id ID del documento a eliminar
     */
    suspend fun deleteDocumentById(id: Long) {
        scannedDocumentDao.deleteDocumentById(id)
    }
    
    /**
     * Elimina todos los documentos
     */
    suspend fun deleteAllDocuments() {
        scannedDocumentDao.deleteAllDocuments()
    }
    
    /**
     * Marca/desmarca un documento como favorito
     * 
     * @param id ID del documento
     * @param isFavorite Estado del favorito
     */
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean) {
        scannedDocumentDao.updateFavoriteStatus(id, isFavorite)
    }
    
    /**
     * Actualiza las etiquetas de un documento
     * 
     * @param id ID del documento
     * @param tags Nuevas etiquetas
     */
    suspend fun updateTags(id: Long, tags: String) {
        scannedDocumentDao.updateTags(id, tags)
    }
    
    /**
     * Obtiene documentos por rango de fechas
     * 
     * @param startDate Fecha de inicio
     * @param endDate Fecha de fin
     * @return Flow con documentos en el rango
     */
    fun getDocumentsByDateRange(startDate: String, endDate: String): Flow<List<ScannedDocument>> {
        return scannedDocumentDao.getDocumentsByDateRange(startDate, endDate)
    }
}
