package com.abdapps.scandocs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.net.Uri
import java.time.LocalDateTime

/**
 * ViewModel para manejar la lógica de negocio del escáner de documentos
 * 
 * Este ViewModel coordina entre la UI y la lógica del escáner:
 * - Maneja el estado de la aplicación
 * - Coordina las operaciones del escáner
 * - Gestiona los resultados del escaneo
 * - Proporciona métodos para la UI
 */
class ScannerViewModel : ViewModel() {
    
    // Estado de la aplicación
    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()
    
    // Instancia del escáner (se inicializa cuando se necesita)
    private var documentScanner: DocumentScanner? = null
    
    // Estado del escáner
    private val _scannerState = MutableStateFlow(ScannerState.IDLE)
    val scannerState: StateFlow<ScannerState> = _scannerState.asStateFlow()
    
    /**
     * Inicializa el escáner de documentos
     * 
     * @param context Contexto de la aplicación
     * @param activity Actividad que maneja el escáner
     */
    fun initializeScanner(context: Context, activity: androidx.fragment.app.FragmentActivity) {
        try {
            documentScanner = DocumentScanner(context, activity)
            documentScanner?.initializeScanner()
            _scannerState.value = ScannerState.READY
            updateUiState { copy(isScannerReady = true) }
        } catch (e: Exception) {
            _scannerState.value = ScannerState.ERROR
            updateUiState { copy(errorMessage = "Error al inicializar el escáner: ${e.message}") }
        }
    }
    
    /**
     * Inicia el proceso de escaneo de documentos
     */
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
                handleScanError(e)
            }
        }
    }
    
    /**
     * Maneja el resultado exitoso del escaneo
     * 
     * @param result Resultado del escaneo
     */
    private fun handleScanResult(result: DocumentScanResult) {
        viewModelScope.launch {
            _scannerState.value = ScannerState.COMPLETED
            updateUiState {
                copy(
                    isScanning = false,
                    scannedPages = result.pages,
                    generatedPdf = result.pdf,
                    successMessage = "Documento escaneado exitosamente: ${result.pages.size} páginas"
                )
            }
        }
    }
    
    /**
     * Maneja los errores del escaneo
     * 
     * @param error Excepción que ocurrió
     */
    private fun handleScanError(error: Exception) {
        viewModelScope.launch {
            _scannerState.value = ScannerState.ERROR
            updateUiState {
                copy(
                    isScanning = false,
                    errorMessage = "Error al escanear: ${error.message}"
                )
            }
        }
    }
    
    /**
     * Limpia el mensaje de error
     */
    fun clearError() {
        updateUiState { copy(errorMessage = null) }
    }
    
    /**
     * Limpia el mensaje de éxito
     */
    fun clearSuccess() {
        updateUiState { copy(successMessage = null) }
    }
    
    /**
     * Limpia los resultados del escaneo
     */
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
    
    /**
     * Actualiza el estado de la UI de manera inmutable
     * 
     * @param update Lambda que modifica el estado actual
     */
    private fun updateUiState(update: ScannerUiState.() -> ScannerUiState) {
        _uiState.value = _uiState.value.update()
    }
    
    /**
     * Libera recursos del escáner
     */
    override fun onCleared() {
        super.onCleared()
        documentScanner?.cleanup()
    }
}

/**
 * Estados posibles del escáner
 */
enum class ScannerState {
    IDLE,       // Estado inicial
    READY,      // Escáner listo para usar
    SCANNING,   // Escaneando documento
    COMPLETED,  // Escaneo completado
    ERROR       // Error en el escaneo
}

/**
 * Estado de la UI del escáner
 * 
 * @param isScannerReady Indica si el escáner está listo para usar
 * @param isScanning Indica si se está escaneando un documento
 * @param scannedPages Lista de páginas escaneadas
 * @param generatedPdf Información del PDF generado
 * @param errorMessage Mensaje de error (si existe)
 * @param successMessage Mensaje de éxito (si existe)
 */
data class ScannerUiState(
    val isScannerReady: Boolean = false,
    val isScanning: Boolean = false,
    val scannedPages: List<DocumentPage> = emptyList(),
    val generatedPdf: DocumentPdf? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
