package com.abdapps.scandocs.data.dao

import androidx.room.*
import com.abdapps.scandocs.data.entity.ScannedDocument
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para documentos escaneados
 * 
 * Esta interfaz define todas las operaciones de base de datos:
 * - Insertar nuevos documentos
 * - Consultar documentos existentes
 * - Actualizar documentos
 * - Eliminar documentos
 * - Búsquedas y filtros
 */
@Dao
interface ScannedDocumentDao {
    
    /**
     * Obtiene todos los documentos escaneados ordenados por fecha
     * 
     * @return Flow con lista de documentos ordenados por fecha de escaneo
     */
    @Query("SELECT * FROM scanned_documents ORDER BY scanDate DESC")
    fun getAllDocuments(): Flow<List<ScannedDocument>>
    
    /**
     * Obtiene documentos favoritos ordenados por fecha
     * 
     * @return Flow con lista de documentos favoritos
     */
    @Query("SELECT * FROM scanned_documents WHERE isFavorite = 1 ORDER BY scanDate DESC")
    fun getFavoriteDocuments(): Flow<List<ScannedDocument>>
    
    /**
     * Busca documentos por nombre o etiquetas
     * 
     * @param query Término de búsqueda
     * @return Flow con lista de documentos que coinciden con la búsqueda
     */
    @Query("SELECT * FROM scanned_documents WHERE fileName LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY scanDate DESC")
    fun searchDocuments(query: String): Flow<List<ScannedDocument>>
    
    /**
     * Obtiene un documento específico por ID
     * 
     * @param id ID del documento
     * @return Documento encontrado o null
     */
    @Query("SELECT * FROM scanned_documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): ScannedDocument?
    
    /**
     * Obtiene el documento más reciente
     * 
     * @return Documento más reciente o null
     */
    @Query("SELECT * FROM scanned_documents ORDER BY scanDate DESC LIMIT 1")
    suspend fun getLatestDocument(): ScannedDocument?
    
    /**
     * Obtiene el total de documentos escaneados
     * 
     * @return Número total de documentos
     */
    @Query("SELECT COUNT(*) FROM scanned_documents")
    suspend fun getDocumentCount(): Int
    
    /**
     * Obtiene el tamaño total de todos los documentos
     * 
     * @return Tamaño total en bytes
     */
    @Query("SELECT SUM(fileSize) FROM scanned_documents")
    suspend fun getTotalSize(): Long?
    
    /**
     * Inserta un nuevo documento escaneado
     * 
     * @param document Documento a insertar
     * @return ID del documento insertado
     */
    @Insert
    suspend fun insertDocument(document: ScannedDocument): Long
    
    /**
     * Actualiza un documento existente
     * 
     * @param document Documento a actualizar
     */
    @Update
    suspend fun updateDocument(document: ScannedDocument)
    
    /**
     * Elimina un documento por ID
     * 
     * @param id ID del documento a eliminar
     */
    @Query("DELETE FROM scanned_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)
    
    /**
     * Elimina todos los documentos
     */
    @Query("DELETE FROM scanned_documents")
    suspend fun deleteAllDocuments()
    
    /**
     * Marca/desmarca un documento como favorito
     * 
     * @param id ID del documento
     * @param isFavorite Estado del favorito
     */
    @Query("UPDATE scanned_documents SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)
    
    /**
     * Actualiza las etiquetas de un documento
     * 
     * @param id ID del documento
     * @param tags Nuevas etiquetas
     */
    @Query("UPDATE scanned_documents SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: Long, tags: String)
    
    /**
     * Obtiene documentos por rango de fechas
     * 
     * @param startDate Fecha de inicio
     * @param endDate Fecha de fin
     * @return Flow con lista de documentos en el rango
     */
    @Query("SELECT * FROM scanned_documents WHERE scanDate BETWEEN :startDate AND :endDate ORDER BY scanDate DESC")
    fun getDocumentsByDateRange(startDate: String, endDate: String): Flow<List<ScannedDocument>>
}
