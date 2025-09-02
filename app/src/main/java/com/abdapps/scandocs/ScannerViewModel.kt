package com.abdapps.scandocs

import android.content.Context
import android.content.Intent // Necesario para iniciar actividades
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

    fun startScanning() {
        if (_scannerState.value != ScannerState.READY) {
            updateUiState { copy(errorMessage = "El escáner no está listo") }
            return
        }
        
        viewModelScope.launch {
            try {
                _scannerState.value = ScannerState.SCANNING
                updateUiState { copy(isScanning = true) }
                
                documentScanner?.startScan(
                    onResult = { result ->
                        handleScanResult(result)
                    },
                    onError = { error ->
                        handleScanError(error)
                    }
                )
            } catch (e: Exception) {
                handleScanError(e) // e.message se maneja en handleScanError
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
                    successMessage = "Documento escaneado exitosamente: ${result.pages.size} páginas"
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
                    errorMessage = "Error al escanear: ${error.message ?: "Error desconocido"}"
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
    }

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
                    thumbnailPath = jpgPaths.firstOrNull() ?: ""
                )

                repository.insertDocument(document)

                updateUiState {
                    copy(successMessage = "Documento '$name' guardado.")
                }
                dismissSaveDialog()
                _scannerState.value = ScannerState.READY
            } catch (e: Exception) {
                updateUiState {
                    copy(errorMessage = "Error al guardar el documento: ${e.message ?: "Error desconocido"}")
                }
            }
        }
    }

    // --- NUEVAS FUNCIONES PARA EL HISTORIAL ---

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
                if (document.thumbnailPath!!.isNotBlank() && document.thumbnailPath != document.jpgPath) {
                     if (!fileService.deleteFile(document.thumbnailPath)) {
                        // filesDeletedSuccessfully = false; // Menos crítico
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
                val document = repository.getDocumentById(documentId) // Asume que es suspend fun
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

/**
 * Estados posibles del escáner
 */
enum class ScannerState {
    IDLE,       // Estado inicial
    READY,      // Escáner listo para usar
    SCANNING,   // Escaneando documento
    PROCESSING, // AÑADIDO: Procesando el documento después del escaneo
    SUCCESS,    // CAMBIADO: Escaneo y procesamiento exitosos (antes COMPLETED)
    ERROR       // Error en el escaneo
}

/**
 * Estado de la UI del escáner
 */
data class ScannerUiState(
    val isScannerReady: Boolean = false,
    val isScanning: Boolean = false,
    val scannedPages: List<DocumentPage> = emptyList(),
    val generatedPdf: DocumentPdf? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
