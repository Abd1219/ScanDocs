package com.abdapps.scandocs

import android.content.Context
import android.content.Intent // Necesario para iniciar actividades
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.abdapps.scandocs.data.entity.DocumentEntity
import com.abdapps.scandocs.data.repository.DocumentRepository
import com.abdapps.scandocs.service.FileService
import com.abdapps.scandocs.utils.UiText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScannerViewModel(
    private val repository: DocumentRepository,
    private val fileService: FileService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var documentScanner: DocumentScanner? = null

    private val _scannerState = MutableStateFlow(ScannerState.IDLE)
    val scannerState: StateFlow<ScannerState> = _scannerState.asStateFlow()

    val allDocuments: Flow<List<DocumentEntity>> = repository.allDocuments

    private val _showSaveDialog = MutableStateFlow(false)
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    private val _documentName = MutableStateFlow("")
    val documentName: StateFlow<String> = _documentName.asStateFlow()

    fun initializeScanner(context: Context, activity: androidx.fragment.app.FragmentActivity) {
        viewModelScope.launch {
            try {
                setLoading(true, UiText.StringResource(R.string.init_scanner))
                
                // La inicialización debe hacerse en el hilo principal
                documentScanner = DocumentScanner(context, activity)
                documentScanner?.initializeScanner()
                
                // Pequeña pausa para mostrar el loading (en background)
                withContext(Dispatchers.IO) {
                    kotlinx.coroutines.delay(800)
                }
                
                _uiState.update { it.copy(status = ScannerStatus.Ready) }
                _scannerState.value = ScannerState.READY
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun handleScanCancelled() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = ScannerStatus.Ready) }
            _scannerState.value = ScannerState.READY
        }
    }

    fun startScanning() {
        if (_scannerState.value != ScannerState.READY) {
            _uiState.update { it.copy(status = ScannerStatus.Error(UiText.StringResource(R.string.no_retry_operation))) }
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(status = ScannerStatus.Scanning) }
                _scannerState.value = ScannerState.SCANNING
                
                documentScanner?.startScan(
                    onResult = { result -> handleScanResult(ScannerResult.Success(result.pages, result.pdf)) },
                    onError = { error -> handleScanResult(ScannerResult.Error(error.message ?: "Unknown")) },
                    onCancel = { handleScanResult(ScannerResult.Canceled) }
                )
            } catch (e: Exception) {
                handleScanError(e) 
            }
        }
    }

    private fun handleScanResult(result: ScannerResult) {
        viewModelScope.launch {
            when (result) {
                is ScannerResult.Success -> {
                    _uiState.update { it.copy(
                        status = ScannerStatus.Ready,
                        scannedPages = result.pages,
                        generatedPdf = result.pdf
                    ) }
                    _scannerState.value = ScannerState.SUCCESS
                    _documentName.value = "Documento_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}"
                    _showSaveDialog.value = true
                }
                is ScannerResult.Canceled -> {
                    _uiState.update { it.copy(status = ScannerStatus.Ready) }
                    _scannerState.value = ScannerState.READY
                }
                is ScannerResult.Error -> {
                    handleError(Exception(result.error))
                }
            }
        }
    }

    private fun handleScanError(error: Exception) {
        handleError(error)
    }
    
    private fun handleError(e: Exception) {
        val errorMessage = when {
            e.message?.contains("camera", true) == true -> UiText.StringResource(R.string.error_camera)
            e.message?.contains("permis", true) == true -> UiText.StringResource(R.string.error_permissions)
            else -> UiText.DynamicString(e.message ?: "Error desconocido")
        }
        _uiState.update { it.copy(status = ScannerStatus.Error(errorMessage)) }
        _scannerState.value = ScannerState.ERROR
    }

    fun clearError() {
        _uiState.update { it.copy(status = ScannerStatus.Ready) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
    
    fun retryLastOperation() {
        when (_scannerState.value) {
            ScannerState.ERROR -> {
                clearError()
                startScanning()
            }
            else -> {
                _uiState.update { it.copy(status = ScannerStatus.Error(UiText.StringResource(R.string.no_retry_operation))) }
            }
        }
    }
    
    private fun setLoading(isLoading: Boolean, message: UiText = UiText.StringResource(R.string.processing_msg)) {
        if (isLoading) {
            _uiState.update { it.copy(status = ScannerStatus.Processing(0f, message)) }
        } else {
            _uiState.update { it.copy(status = ScannerStatus.Ready) }
        }
    }

    fun clearResults() {
        _uiState.update {
            it.copy(
                status = ScannerStatus.Ready,
                scannedPages = emptyList(),
                generatedPdf = null,
                successMessage = null
            )
        }
        _scannerState.value = ScannerState.READY
    }

    fun updateDocumentName(name: String) {
        _documentName.value = name
    }

    fun dismissSaveDialog() {
        _showSaveDialog.value = false
        clearResults() 
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveDocument(name: String) {
        viewModelScope.launch {
            try {
                _showSaveDialog.value = false
                _uiState.update { it.copy(status = ScannerStatus.Saving) }
                
                val pdfUri = _uiState.value.generatedPdf?.uri
                val pages = _uiState.value.scannedPages
                
                if (pdfUri == null && pages.isEmpty()) {
                    throw Exception("No hay documentos para guardar")
                }

                // Simular progreso de guardado
                for (i in 1..5) {
                    val progress = i * 0.2f
                    _uiState.update { it.copy(status = ScannerStatus.Processing(progress, UiText.StringResource(R.string.saving_files))) }
                    kotlinx.coroutines.delay(200)
                }

                val savedPdfPath = pdfUri?.let { fileService.savePdfFile(it, name) } ?: ""
                
                val jpgPaths = mutableListOf<String>()
                pages.forEachIndexed { index, page ->
                    val path = fileService.saveJpgFile(page.imageUri, "${name}_page_${index + 1}")
                    jpgPaths.add(path)
                }

                val document = DocumentEntity(
                    name = name,
                    jpgPath = jpgPaths.joinToString(","),
                    pdfPath = savedPdfPath,
                    createdAt = Date(),
                    thumbnailPath = if (jpgPaths.isNotEmpty()) jpgPaths[0] else ""
                )
                
                repository.insertDocument(document)
                
                _uiState.update { it.copy(
                    status = ScannerStatus.Ready,
                    successMessage = UiText.StringResource(R.string.save_success)
                ) }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch {
            try {
                var filesDeletedSuccessfully = true
                if (document.pdfPath.isNotBlank()) {
                    if (!fileService.deleteFile(document.pdfPath)) {
                        filesDeletedSuccessfully = false
                    }
                }
                document.getJpgPaths().forEach { jpgPath ->
                    if (jpgPath.isNotBlank()) {
                        if (!fileService.deleteFile(jpgPath)) {
                            filesDeletedSuccessfully = false
                        }
                    }
                }
                
                val currentThumbnailPath = document.thumbnailPath
                val jpgPaths = document.getJpgPaths()
                if (currentThumbnailPath != null && currentThumbnailPath.isNotBlank() && !jpgPaths.contains(currentThumbnailPath)) {
                     if (!fileService.deleteFile(currentThumbnailPath)) {
                     }
                }

                repository.deleteDocument(document)

                if (filesDeletedSuccessfully) {
                    _uiState.update { it.copy(successMessage = UiText.StringResource(R.string.delete_success_toast, document.name)) }
                } else {
                    _uiState.update { it.copy(status = ScannerStatus.Error(UiText.StringResource(R.string.delete_partial_error, document.name))) }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(status = ScannerStatus.Error(UiText.StringResource(R.string.delete_error_toast, document.name, e.message ?: "Unknown"))) }
            }
        }
    }

    fun viewFile(context: Context, filePath: String, mimeType: String) {
        viewModelScope.launch {
            try {
                val intent = fileService.createViewIntent(context, filePath, mimeType)
                if (intent != null) {
                    context.startActivity(intent)
                } else {
                     _uiState.update { it.copy(status = ScannerStatus.Error(UiText.StringResource(R.string.action_creation_error))) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = ScannerStatus.Error(UiText.StringResource(R.string.view_error, e.message ?: "Unknown"))) }
            }
        }
    }

    fun shareFile(context: Context, filePath: String, mimeType: String) {
        viewModelScope.launch {
            try {
                val intent = fileService.createShareIntent(context, filePath, mimeType)
                 if (intent != null) {
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_doc_content_description)))
                } else {
                     _uiState.update { it.copy(status = ScannerStatus.Error(UiText.StringResource(R.string.action_creation_error))) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = ScannerStatus.Error(UiText.StringResource(R.string.share_error, e.message ?: "Unknown"))) }
            }
        }
    }

    fun shareDocumentPage(context: Context, document: DocumentEntity, pageIndex: Int) {
        viewModelScope.launch {
            try {
                val jpgPaths = document.getJpgPaths()
                if (pageIndex >= 0 && pageIndex < jpgPaths.size) {
                    val jpgPath = jpgPaths[pageIndex]
                    shareFile(context, jpgPath, "image/jpeg")
                } else {
                    _uiState.update { it.copy(status = ScannerStatus.Error(UiText.StringResource(R.string.invalid_page, pageIndex + 1))) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = ScannerStatus.Error(UiText.StringResource(R.string.share_error, e.message ?: "Unknown"))) }
            }
        }
    }
    
    fun viewDocumentPage(context: Context, document: DocumentEntity, pageIndex: Int) {
        viewModelScope.launch {
            try {
                val jpgPaths = document.getJpgPaths()
                if (pageIndex >= 0 && pageIndex < jpgPaths.size) {
                    val jpgPath = jpgPaths[pageIndex]
                    viewFile(context, jpgPath, "image/jpeg")
                } else {
                    _uiState.update { it.copy(status = ScannerStatus.Error(UiText.StringResource(R.string.invalid_page, pageIndex + 1))) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = ScannerStatus.Error(UiText.StringResource(R.string.view_error, e.message ?: "Unknown"))) }
            }
        }
    }

    @Deprecated("Usar shareFile(context, filePath, mimeType) en su lugar después de la selección del usuario")
    fun shareDocument(context: Context, documentId: Long) {
        viewModelScope.launch {
            try {
                val document = repository.getDocumentById(documentId)
                document?.let {
                    val (path, mime) = when {
                        it.pdfPath.isNotBlank() -> it.pdfPath to "application/pdf"
                        it.getFirstJpgPath() != null -> it.getFirstJpgPath()!! to "image/jpeg"
                        else -> null to null
                    }

                    if (path != null && mime != null) {
                        val intent = fileService.createShareIntent(context, path, mime)
                         if (intent != null) {
                            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_doc_content_description)))
                        } else {
                            _uiState.update { it.copy(status = ScannerStatus.Error(UiText.StringResource(R.string.action_creation_error))) }
                        }
                    } else {
                        _uiState.update { it.copy(status = ScannerStatus.Error(UiText.StringResource(R.string.no_files_available))) }
                    }
                } ?: _uiState.update { it.copy(status = ScannerStatus.Error(UiText.StringResource(R.string.doc_not_found))) }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = ScannerStatus.Error(UiText.StringResource(R.string.share_error, e.message ?: "Unknown"))) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        documentScanner?.cleanup()
    }

    class Factory(
        private val repository: DocumentRepository,
        private val fileService: FileService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
                return ScannerViewModel(repository, fileService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

enum class ScannerState {
    IDLE,      
    READY,     
    SCANNING,  
    PROCESSING,
    SUCCESS,   
    ERROR      
}

sealed interface ScannerStatus {
    object Idle : ScannerStatus
    object Ready : ScannerStatus
    object Scanning : ScannerStatus
    data class Processing(val progress: Float, val message: UiText) : ScannerStatus
    object Saving : ScannerStatus
    data class Error(val message: UiText) : ScannerStatus
}

data class ScannerUiState(
    val status: ScannerStatus = ScannerStatus.Idle,
    val scannedPages: List<DocumentPage> = emptyList(),
    val generatedPdf: DocumentPdf? = null,
    val successMessage: UiText? = null // Optional persistent success message
)

sealed class ScannerResult {
    data class Success(val pages: List<DocumentPage>, val pdf: DocumentPdf?) : ScannerResult()
    object Canceled : ScannerResult()
    data class Error(val error: String) : ScannerResult()
}
