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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        viewModelScope.launch {
            try {
                setLoading(true, "Inicializando escáner...")
                
                // La inicialización debe hacerse en el hilo principal
                documentScanner = DocumentScanner(context, activity)
                documentScanner?.initializeScanner()
                
                // Pequeña pausa para mostrar el loading (en background)
                withContext(Dispatchers.IO) {
                    kotlinx.coroutines.delay(800)
                }
                
                _scannerState.value = ScannerState.READY
                updateUiState { 
                    copy(
                        isScannerReady = true,
                        isLoading = false,
                        loadingMessage = null
                    ) 
                }
            } catch (e: Exception) {
                _scannerState.value = ScannerState.ERROR
                updateUiState { 
                    copy(
                        isLoading = false,
                        loadingMessage = null,
                        errorMessage = "Error al inicializar el escáner: ${getErrorMessage(e)}",
                        canRetry = true
                    ) 
                }
            }
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
            try {
                // Mostrar estado de procesamiento
                updateUiState {
                    copy(
                        isScanning = false,
                        isProcessing = true,
                        processingProgress = 0.3f,
                        loadingMessage = "Procesando documento escaneado..."
                    )
                }
                
                // Simular procesamiento (en una implementación real aquí iría la optimización de imágenes)
                kotlinx.coroutines.delay(500)
                
                updateUiState {
                    copy(
                        processingProgress = 0.7f,
                        loadingMessage = "Generando vista previa..."
                    )
                }
                
                kotlinx.coroutines.delay(300)
                
                _scannerState.value = ScannerState.SUCCESS
                updateUiState {
                    copy(
                        isProcessing = false,
                        processingProgress = 1f,
                        scannedPages = result.pages,
                        generatedPdf = result.pdf,
                        successMessage = "Documento escaneado: ${result.pages.size} página(s)",
                        loadingMessage = null
                    )
                }
                
                _showSaveDialog.value = true
                val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                _documentName.value = "Scan_${dateFormat.format(Date())}"
                
            } catch (e: Exception) {
                handleScanError(e)
            }
        }
    }

    private fun handleScanError(error: Exception) {
        viewModelScope.launch {
            _scannerState.value = ScannerState.ERROR
            updateUiState {
                copy(
                    isScanning = false,
                    isProcessing = false,
                    isLoading = false,
                    loadingMessage = null,
                    processingProgress = 0f,
                    errorMessage = getErrorMessage(error),
                    canRetry = isRetryableError(error)
                )
            }
        }
    }
    
    private fun getErrorMessage(error: Exception): String {
        return when {
            error.message?.contains("camera", ignoreCase = true) == true -> 
                "Error de cámara. Verifica los permisos y que la cámara no esté siendo usada por otra app."
            error.message?.contains("permission", ignoreCase = true) == true -> 
                "Permisos insuficientes. Verifica los permisos de cámara y almacenamiento."
            error.message?.contains("network", ignoreCase = true) == true -> 
                "Error de conexión. Verifica tu conexión a internet."
            error.message?.contains("storage", ignoreCase = true) == true -> 
                "Error de almacenamiento. Verifica que tengas espacio suficiente."
            else -> "Error durante el escaneo: ${error.message ?: "Error desconocido"}"
        }
    }
    
    private fun isRetryableError(error: Exception): Boolean {
        return when {
            error.message?.contains("network", ignoreCase = true) == true -> true
            error.message?.contains("timeout", ignoreCase = true) == true -> true
            error.message?.contains("temporary", ignoreCase = true) == true -> true
            else -> false
        }
    }

    fun clearError() {
        updateUiState { 
            copy(
                errorMessage = null, 
                canRetry = false
            ) 
        }
    }

    fun clearSuccess() {
        updateUiState { copy(successMessage = null) }
    }
    
    fun retryLastOperation() {
        when (_scannerState.value) {
            ScannerState.ERROR -> {
                clearError()
                startScanning()
            }
            else -> {
                // No hay operación para reintentar
                updateUiState { 
                    copy(errorMessage = "No hay operación para reintentar") 
                }
            }
        }
    }
    
    fun setLoading(isLoading: Boolean, message: String? = null) {
        updateUiState { 
            copy(
                isLoading = isLoading,
                loadingMessage = if (isLoading) message else null
            ) 
        }
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
        // Limpiar el nombre de caracteres problemáticos y espacios extra
        val cleanedName = name.trim()
            .replace(Regex("[\\r\\n\\t]"), "") // Remover saltos de línea y tabs
            .replace(Regex("[<>:\"/\\\\|?*]"), "_") // Reemplazar caracteres no válidos para nombres de archivo
            .take(50) // Limitar longitud
        
        _documentName.value = cleanedName
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
                // Mostrar progreso de guardado
                updateUiState {
                    copy(
                        isProcessing = true,
                        processingProgress = 0.1f,
                        loadingMessage = "Guardando documento..."
                    )
                }

                val jpgPaths = mutableListOf<String>()
                val totalPages = currentState.scannedPages.size
                
                // Guardar imágenes con progreso
                currentState.scannedPages.forEachIndexed { index, page ->
                    updateUiState {
                        copy(
                            processingProgress = 0.1f + (0.6f * (index + 1) / totalPages),
                            loadingMessage = "Guardando página ${index + 1} de $totalPages..."
                        )
                    }
                    
                    val jpgPath = withContext(Dispatchers.IO) {
                        fileService.saveJpgFile(page.imageUri, "${name}_page${index + 1}")
                    }
                    jpgPaths.add(jpgPath)
                }

                // Guardar PDF
                updateUiState {
                    copy(
                        processingProgress = 0.8f,
                        loadingMessage = "Generando PDF..."
                    )
                }

                val pdfPath = currentState.generatedPdf?.let {
                    withContext(Dispatchers.IO) {
                        fileService.savePdfFile(it.uri, name)
                    }
                } ?: ""

                // Guardar en base de datos
                updateUiState {
                    copy(
                        processingProgress = 0.9f,
                        loadingMessage = "Finalizando..."
                    )
                }

                val document = DocumentEntity(
                    name = name,
                    jpgPath = jpgPaths.joinToString(","), // Guardar todas las rutas separadas por comas
                    pdfPath = pdfPath,
                    createdAt = Date(),
                    thumbnailPath = jpgPaths.firstOrNull() ?: ""
                )

                withContext(Dispatchers.IO) {
                    repository.insertDocument(document)
                }

                updateUiState {
                    copy(
                        isProcessing = false,
                        processingProgress = 1f,
                        loadingMessage = null,
                        successMessage = "Documento '$name' guardado exitosamente"
                    )
                }
                
                // Pequeña pausa para mostrar el éxito antes de limpiar
                kotlinx.coroutines.delay(1000)
                dismissSaveDialog()
                
            } catch (e: Exception) {
                updateUiState {
                    copy(
                        isProcessing = false,
                        processingProgress = 0f,
                        loadingMessage = null,
                        errorMessage = "Error al guardar el documento: ${getErrorMessage(e)}",
                        canRetry = true
                    )
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
                // Eliminar todos los archivos JPG
                document.getJpgPaths().forEach { jpgPath ->
                    if (jpgPath.isNotBlank()) {
                        if (!fileService.deleteFile(jpgPath)) {
                            filesDeletedSuccessfully = false
                        }
                    }
                }
                
                // Eliminar thumbnail si es diferente de las imágenes JPG principales
                val currentThumbnailPath = document.thumbnailPath
                val jpgPaths = document.getJpgPaths()
                if (currentThumbnailPath != null && currentThumbnailPath.isNotBlank() && !jpgPaths.contains(currentThumbnailPath)) {
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

    /**
     * Comparte una página específica de un documento
     */
    fun shareDocumentPage(context: Context, document: DocumentEntity, pageIndex: Int) {
        viewModelScope.launch {
            try {
                val jpgPaths = document.getJpgPaths()
                if (pageIndex >= 0 && pageIndex < jpgPaths.size) {
                    val jpgPath = jpgPaths[pageIndex]
                    shareFile(context, jpgPath, "image/jpeg")
                } else {
                    updateUiState { copy(errorMessage = "Página no válida: ${pageIndex + 1}") }
                }
            } catch (e: Exception) {
                updateUiState { copy(errorMessage = "Error al compartir página: ${e.message ?: "Error desconocido"}") }
            }
        }
    }
    
    /**
     * Ve una página específica de un documento
     */
    fun viewDocumentPage(context: Context, document: DocumentEntity, pageIndex: Int) {
        viewModelScope.launch {
            try {
                val jpgPaths = document.getJpgPaths()
                if (pageIndex >= 0 && pageIndex < jpgPaths.size) {
                    val jpgPath = jpgPaths[pageIndex]
                    viewFile(context, jpgPath, "image/jpeg")
                } else {
                    updateUiState { copy(errorMessage = "Página no válida: ${pageIndex + 1}") }
                }
            } catch (e: Exception) {
                updateUiState { copy(errorMessage = "Error al ver página: ${e.message ?: "Error desconocido"}") }
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
    val isProcessing: Boolean = false,
    val processingProgress: Float = 0f,
    val scannedPages: List<DocumentPage> = emptyList(),
    val generatedPdf: DocumentPdf? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val canRetry: Boolean = false,
    val isLoading: Boolean = false,
    val loadingMessage: String? = null
)
