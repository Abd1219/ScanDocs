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
import kotlinx.coroutines.launch
import com.abdapps.scandocs.data.entity.DocumentEntity
import com.abdapps.scandocs.data.repository.DocumentRepository
import com.abdapps.scandocs.service.FileService
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
        try {
            documentScanner = DocumentScanner(context, activity)
            documentScanner?.initializeScanner()
            _scannerState.value = ScannerState.READY
            updateUiState { copy(isScannerReady = true) }
        } catch (e: Exception) {
            _scannerState.value = ScannerState.ERROR
            updateUiState { copy(errorMessage = "Error al inicializar el escáner: ${e.message ?: "Error desconocido"}") }
        }
    }

    private fun handleScanCancelled() {
        viewModelScope.launch {
            _scannerState.value = ScannerState.READY
            updateUiState {
                copy(
                    isScanning = false,
                    errorMessage = null // Limpiar cualquier error previo
                )
            }
        }
    }

    fun startScanning() {
        if (_scannerState.value != ScannerState.READY) {
            updateUiState { copy(errorMessage = "El escáner no está listo o está ocupado.") } // Mensaje más específico
            return
        }
        
        viewModelScope.launch {
            try {
                _scannerState.value = ScannerState.SCANNING
                updateUiState { copy(isScanning = true, errorMessage = null) } // Limpiar errores al iniciar
                
                documentScanner?.startScan(
                    onResult = this@ScannerViewModel::handleScanResult,
                    onError = this@ScannerViewModel::handleScanError,
                    onCancel = this@ScannerViewModel::handleScanCancelled // Pasar el nuevo callback
                )
            } catch (e: Exception) {
                handleScanError(e) 
            }
        }
    }

    private fun handleScanResult(result: DocumentScanResult) {
        viewModelScope.launch {
            _scannerState.value = ScannerState.SUCCESS
            updateUiState {
                copy(
                    isScanning = false,
                    scannedPages = result.pages,
                    generatedPdf = result.pdf,
                    successMessage = "Documento escaneado: ${result.pages.size} página(s)"
                )
            }
            _showSaveDialog.value = true
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            _documentName.value = "Scan_${dateFormat.format(Date())}"
        }
    }

    private fun handleScanError(error: Exception) {
        viewModelScope.launch {
            _scannerState.value = ScannerState.ERROR
            updateUiState {
                copy(
                    isScanning = false,
                    errorMessage = "Error durante el escaneo: ${error.message ?: "Error desconocido"}"
                )
            }
        }
    }

    fun clearError() {
        updateUiState { copy(errorMessage = null) }
    }

    fun clearSuccess() {
        updateUiState { copy(successMessage = null) }
    }

    fun clearResults() {
        updateUiState {
            copy(
                scannedPages = emptyList(),
                generatedPdf = null,
                successMessage = null,
                errorMessage = null,
                isScannerReady = true,
                isScanning = false
            )
        }
        _scannerState.value = ScannerState.READY
    }

    fun updateDocumentName(name: String) {
        _documentName.value = name
    }

    fun dismissSaveDialog() {
        _showSaveDialog.value = false
        // Considerar llamar a clearResults() si el usuario cancela el guardado
        // para que la UI vuelva al estado inicial de escaneo.
        clearResults() 
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveDocument() {
        viewModelScope.launch {
            val currentState = uiState.value
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val currentDate = Date()
            val name = documentName.value.ifEmpty { "Scan_${dateFormat.format(currentDate)}" }

            try {
                val jpgPaths = mutableListOf<String>()
                currentState.scannedPages.forEachIndexed { index, page ->
                    val jpgPath = fileService.saveJpgFile(page.imageUri, "${name}_page${index + 1}")
                    jpgPaths.add(jpgPath)
                }

                val pdfPath = currentState.generatedPdf?.let {
                    fileService.savePdfFile(it.uri, name)
                } ?: ""

                val document = DocumentEntity(
                    name = name,
                    jpgPath = jpgPaths.firstOrNull() ?: "",
                    pdfPath = pdfPath,
                    createdAt = Date(),
                    thumbnailPath = jpgPaths.firstOrNull() ?: "" // Asegura que no sea null
                )

                repository.insertDocument(document)

                updateUiState {
                    copy(successMessage = "Documento '$name' guardado.")
                }
                dismissSaveDialog() // Esto ya llama a clearResults()
                // _scannerState.value = ScannerState.READY // clearResults() ya hace esto
            } catch (e: Exception) {
                updateUiState {
                    copy(errorMessage = "Error al guardar el documento: ${e.message ?: "Error desconocido"}")
                }
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
                if (document.jpgPath.isNotBlank()) {
                    if (!fileService.deleteFile(document.jpgPath)) {
                        filesDeletedSuccessfully = false
                    }
                }
                
                // Corrección para thumbnailPath nulo
                val currentThumbnailPath = document.thumbnailPath
                if (currentThumbnailPath != null && currentThumbnailPath.isNotBlank() && currentThumbnailPath != document.jpgPath) {
                     if (!fileService.deleteFile(currentThumbnailPath)) {
                        // filesDeletedSuccessfully = false; // Opcional: considerar si esto es crítico
                     }
                }

                repository.deleteDocument(document)

                if (filesDeletedSuccessfully) {
                    updateUiState { copy(successMessage = "Documento '${document.name}' eliminado.") }
                } else {
                    updateUiState { copy(errorMessage = "Documento '${document.name}' eliminado de la BD, pero algunos archivos no se borraron.") }
                }

            } catch (e: Exception) {
                updateUiState { copy(errorMessage = "Error al eliminar '${document.name}': ${e.message ?: "Error desconocido"}") }
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
                     updateUiState { copy(errorMessage = "No se puede crear la acción para ver el archivo.") }
                }
            } catch (e: Exception) {
                updateUiState { copy(errorMessage = "Error al intentar ver el archivo: ${e.message ?: "Error desconocido"}") }
            }
        }
    }

    fun shareFile(context: Context, filePath: String, mimeType: String) {
        viewModelScope.launch {
            try {
                val intent = fileService.createShareIntent(context, filePath, mimeType)
                 if (intent != null) {
                    context.startActivity(Intent.createChooser(intent, "Compartir ${filePath.substringAfterLast('/')}"))
                } else {
                     updateUiState { copy(errorMessage = "No se puede crear la acción para compartir el archivo.") }
                }
            } catch (e: Exception) {
                updateUiState { copy(errorMessage = "Error al intentar compartir el archivo: ${e.message ?: "Error desconocido"}") }
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
                        it.jpgPath.isNotBlank() -> it.jpgPath to "image/jpeg"
                        else -> null to null
                    }

                    if (path != null && mime != null) {
                        val intent = fileService.createShareIntent(context, path, mime)
                         if (intent != null) {
                            context.startActivity(Intent.createChooser(intent, "Compartir ${it.name}"))
                        } else {
                            updateUiState { copy(errorMessage = "No se pudo crear el intent para compartir.") }
                        }
                    } else {
                        updateUiState { copy(errorMessage = "No hay archivo disponible para compartir para '${it.name}'.") }
                    }
                } ?: updateUiState { copy(errorMessage = "Documento no encontrado para compartir.") }
            } catch (e: Exception) {
                updateUiState { copy(errorMessage = "Error al compartir: ${e.message ?: "Error desconocido"}") }
            }
        }
    }

    private fun updateUiState(update: ScannerUiState.() -> ScannerUiState) {
        _uiState.value = _uiState.value.update()
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

data class ScannerUiState(
    val isScannerReady: Boolean = false,
    val isScanning: Boolean = false,
    val scannedPages: List<DocumentPage> = emptyList(),
    val generatedPdf: DocumentPdf? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
