package com.abdapps.scandocs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.abdapps.scandocs.ui.components.DocumentScannerScreen
import com.abdapps.scandocs.ui.theme.ScanDocsTheme

/**
 * Actividad principal de la aplicación ScanDocs
 * 
 * Esta actividad integra el escáner de documentos de ML Kit con:
 * - Manejo de permisos automático
 * - Inicialización del escáner
 * - Interfaz de usuario en Compose
 * - Gestión de estado con ViewModel
 * 
 * Características principales:
 * - Escaneo de documentos usando la cámara
 * - Generación automática de PDF
 * - Interfaz moderna con Material Design 3
 * - Manejo robusto de errores y estados
 */
class MainActivity : androidx.fragment.app.FragmentActivity() {
    
    // ViewModel para manejar la lógica de negocio del escáner
    private val scannerViewModel: ScannerViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            ScanDocsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Pantalla principal del escáner
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        scannerViewModel = scannerViewModel
                    )
                }
            }
        }
        
        // Inicializar el escáner cuando se crea la actividad
        initializeScanner()
    }
    
    /**
     * Inicializa el escáner de documentos
     * 
     * Este método configura el escáner y verifica que esté listo para usar.
     * Se llama automáticamente cuando se crea la actividad.
     */
    private fun initializeScanner() {
        try {
            // Inicializar el escáner en el ViewModel
            scannerViewModel.initializeScanner(this, this)
        } catch (e: Exception) {
            // En una implementación real, mostrarías un mensaje de error al usuario
            e.printStackTrace()
        }
    }
}

/**
 * Pantalla principal de la aplicación
 * 
 * Este composable coordina entre el ViewModel y los componentes de UI:
 * - Observa el estado del ViewModel
 * - Maneja los callbacks de la UI
 * - Proporciona la interfaz principal del usuario
 * 
 * @param modifier Modificador de Compose para la pantalla
 * @param scannerViewModel ViewModel que maneja la lógica del escáner
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    scannerViewModel: ScannerViewModel
) {
    // Observar el estado de la UI desde el ViewModel
    val uiState by scannerViewModel.uiState.collectAsState()
    
    // Observar el estado del escáner
    val scannerState by scannerViewModel.scannerState.collectAsState()
    
    // Efecto para manejar la inicialización automática
    LaunchedEffect(Unit) {
        // Este efecto se ejecuta una vez cuando se compone la pantalla
        // Puedes agregar lógica de inicialización adicional aquí si es necesario
    }
    
    // Pantalla principal del escáner
    DocumentScannerScreen(
        uiState = uiState,
        scannerState = scannerState,
        onStartScan = {
            // Iniciar el proceso de escaneo
            scannerViewModel.startScanning()
        },
        onClearResults = {
            // Limpiar los resultados del escaneo
            scannerViewModel.clearResults()
        },
        onClearError = {
            // Limpiar mensajes de error
            scannerViewModel.clearError()
        },
        onClearSuccess = {
            // Limpiar mensajes de éxito
            scannerViewModel.clearSuccess()
        }
    )
}

/**
 * Vista previa de la pantalla principal para desarrollo
 * 
 * Esta vista previa permite a los desarrolladores ver cómo se verá
 * la interfaz sin necesidad de ejecutar la aplicación completa.
 */
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ScanDocsTheme {
        // En la vista previa, mostramos un estado de ejemplo
        // En una implementación real, esto mostraría datos simulados
        DocumentScannerScreen(
            uiState = ScannerUiState(
                isScannerReady = true,
                isScanning = false,
                scannedPages = emptyList(),
                generatedPdf = null
            ),
            scannerState = ScannerState.READY,
            onStartScan = { /* No-op en preview */ },
            onClearResults = { /* No-op en preview */ },
            onClearError = { /* No-op en preview */ },
            onClearSuccess = { /* No-op en preview */ }
        )
    }
}