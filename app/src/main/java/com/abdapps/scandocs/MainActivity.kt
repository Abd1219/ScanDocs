package com.abdapps.scandocs

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abdapps.scandocs.data.database.AppDatabase
import com.abdapps.scandocs.data.repository.DocumentRepository
import com.abdapps.scandocs.service.FileService
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
 * - Historial de documentos escaneados
 */
class MainActivity : androidx.fragment.app.FragmentActivity() {
    
    // ViewModel para manejar la lógica de negocio del escáner
    private lateinit var scannerViewModel: ScannerViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Inicializar dependencias
        val database = AppDatabase.getDatabase(this)
        val repository = DocumentRepository(database.documentDao())
        val fileService = FileService(this)
        
        // Inicializar ViewModel con dependencias
        val factory = ScannerViewModel.Factory(repository, fileService)
        scannerViewModel = viewModels<ScannerViewModel> { factory }.value
        
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
 * - Gestiona la navegación entre pantallas
 * 
 * @param modifier Modificador de Compose para la pantalla
 * @param scannerViewModel ViewModel que maneja la lógica del escáner
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    scannerViewModel: ScannerViewModel
) {
    // Pantalla principal con el escáner e historial integrados
    DocumentScannerScreen(
        modifier = modifier,
        viewModel = scannerViewModel
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
        // Vista previa con estado de ejemplo
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ScanDocs Preview",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Listo para escanear documentos",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}