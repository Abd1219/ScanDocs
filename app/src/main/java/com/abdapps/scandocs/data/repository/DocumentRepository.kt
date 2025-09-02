package com.abdapps.scandocs.data.repository

import com.abdapps.scandocs.data.dao.DocumentDao
import com.abdapps.scandocs.data.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para manejar los documentos escaneados
 */
class DocumentRepository(private val documentDao: DocumentDao) {
    
    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()
    
    suspend fun getDocumentById(id: Long): DocumentEntity? {
        return documentDao.getDocumentById(id)
    }
    
    suspend fun insertDocument(document: DocumentEntity): Long {
        return documentDao.insertDocument(document)
    }
    
    suspend fun updateDocument(document: DocumentEntity) {
        documentDao.updateDocument(document)
    }
    
    suspend fun deleteDocument(document: DocumentEntity) {
        documentDao.deleteDocument(document)
    }
    
    suspend fun deleteDocumentById(id: Long) {
        documentDao.deleteDocumentById(id)
    }
}