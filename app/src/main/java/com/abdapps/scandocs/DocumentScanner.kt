package com.abdapps.scandocs

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * Clase principal para manejar el escáner de documentos usando ML Kit
 * 
 * Esta clase encapsula toda la funcionalidad del escáner de documentos:
 * - Configuración del escáner
 * - Inicialización del launcher para resultados
 * - Manejo de resultados del escaneo
 * - Configuración de opciones personalizables
 * 
 * Requisitos del sistema:
 * - API 21+ (Android 5.0+)
 * - Mínimo 1.7GB de RAM del dispositivo
 * - Google Play Services instalado
 */
class DocumentScanner(
    private val context: Context,
    private val activity: FragmentActivity
) {
    
    // Configuración por defecto del escáner
    private val defaultOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)           // Permitir importar desde galería
        .setPageLimit(10)                        // Máximo 10 páginas por documento
        .setResultFormats(
            GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,  // Formato de imagen
            GmsDocumentScannerOptions.RESULT_FORMAT_PDF     // Formato PDF
        )
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)  // Modo completo
        .build()
    
    // Cliente del escáner de documentos
    private val scanner: GmsDocumentScanner = GmsDocumentScanning.getClient(defaultOptions)
    
    // Launcher para manejar el resultado del escaneo
    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    
    // Callback para manejar el resultado del escaneo
    private var onScanResult: ((DocumentScanResult) -> Unit)? = null
    
    // Callback para manejar errores
    private var onError: ((Exception) -> Unit)? = null
    
    /**
     * Inicializa el launcher del escáner
     * 
     * Este método debe ser llamado antes de usar el escáner.
     * Configura el launcher que manejará el resultado del escaneo.
     */
    fun initializeScanner() {
        scannerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleScanResult(result.data)
            } else {
                // Usuario canceló el escaneo
                onError?.invoke(Exception("Escaneo cancelado por el usuario"))
            }
        }
    }
    
    /**
     * Inicia el escaneo de documentos
     * 
     * @param onResult Callback que se ejecuta cuando se completa el escaneo
     * @param onError Callback que se ejecuta cuando ocurre un error
     */
    fun startScan(
        onResult: (DocumentScanResult) -> Unit,
        onError: (Exception) -> Unit
    ) {
        this.onScanResult = onResult
        this.onError = onError
        
        try {
            // Obtener el intent para iniciar el escáner
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    // Iniciar la actividad del escáner
                    val request = IntentSenderRequest.Builder(intentSender).build()
                    scannerLauncher.launch(request)
                }
                .addOnFailureListener { exception ->
                    // Manejar errores del escáner
                    onError(exception)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }
    
    /**
     * Maneja el resultado del escaneo
     * 
     * @param data Intent con los datos del resultado
     */
    private fun handleScanResult(data: android.content.Intent?) {
        try {
            // Extraer el resultado del escaneo
            val result = GmsDocumentScanningResult.fromActivityResultIntent(data)
            
            // Verificar que el resultado no sea nulo
            if (result != null) {
                // Crear objeto de resultado personalizado
                val scanResult = DocumentScanResult(
                    pages = result.pages?.mapIndexed { index, page ->
                        DocumentPage(
                            imageUri = page.imageUri,
                            pageNumber = index + 1
                        )
                    } ?: emptyList(),
                    pdf = result.pdf?.let { pdf ->
                        DocumentPdf(
                            uri = pdf.uri,
                            pageCount = pdf.pageCount
                        )
                    }
                )
                
                // Notificar el resultado
                onScanResult?.invoke(scanResult)
            } else {
                onError?.invoke(Exception("No se pudo obtener el resultado del escaneo"))
            }
            
        } catch (e: Exception) {
            onError?.invoke(e)
        }
    }
    
    /**
     * Configura opciones personalizadas del escáner
     * 
     * @param options Opciones personalizadas del escáner
     */
    fun updateOptions(options: GmsDocumentScannerOptions) {
        // Crear nuevo cliente con las opciones actualizadas
        val newScanner = GmsDocumentScanning.getClient(options)
        // Nota: En una implementación real, necesitarías manejar esto de manera más robusta
        // Por ahora, solo creamos el nuevo cliente
    }
    
    /**
     * Libera recursos del escáner
     */
    fun cleanup() {
        try {
            // Nota: El cliente de ML Kit se cierra automáticamente
            // No es necesario llamar a close() manualmente
        } catch (e: Exception) {
            // Ignorar errores al cerrar
        }
    }
}

/**
 * Clase de datos que representa una página escaneada
 * 
 * @param imageUri URI de la imagen de la página
 * @param pageNumber Número de la página
 */
data class DocumentPage(
    val imageUri: Uri,
    val pageNumber: Int
)

/**
 * Clase de datos que representa un PDF generado
 * 
 * @param uri URI del archivo PDF
 * @param pageCount Número total de páginas en el PDF
 */
data class DocumentPdf(
    val uri: Uri,
    val pageCount: Int
)

/**
 * Clase de datos que representa el resultado completo del escaneo
 * 
 * @param pages Lista de páginas escaneadas
 * @param pdf Información del PDF generado (opcional)
 */
data class DocumentScanResult(
    val pages: List<DocumentPage>,
    val pdf: DocumentPdf?
)
