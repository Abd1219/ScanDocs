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

class DocumentScanner(
    private val context: Context,
    private val activity: FragmentActivity
) {
    
    private val defaultOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setPageLimit(10)
        .setResultFormats(
            GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
            GmsDocumentScannerOptions.RESULT_FORMAT_PDF
        )
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .build()
    
    private var scanner: GmsDocumentScanner = GmsDocumentScanning.getClient(defaultOptions)
    
    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    
    private var onScanResult: ((DocumentScanResult) -> Unit)? = null
    private var onError: ((Exception) -> Unit)? = null
    private var onScanCancel: (() -> Unit)? = null // Nuevo callback para cancelación
    
    fun initializeScanner() {
        scannerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleScanResult(result.data)
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                // Usuario canceló el escaneo
                onScanCancel?.invoke() // Llamar al nuevo callback de cancelación
            } else {
                // Otro tipo de resultado no exitoso (considerado error o cancelación inesperada)
                onError?.invoke(Exception("Escaneo finalizado con resultado: ${result.resultCode}"))
            }
        }
    }
    
    fun startScan(
        onResult: (DocumentScanResult) -> Unit,
        onError: (Exception) -> Unit,
        onCancel: () -> Unit // Nuevo parámetro para el callback de cancelación
    ) {
        this.onScanResult = onResult
        this.onError = onError
        this.onScanCancel = onCancel // Guardar el callback de cancelación
        
        try {
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    val request = IntentSenderRequest.Builder(intentSender).build()
                    scannerLauncher.launch(request)
                }
                .addOnFailureListener { exception ->
                    this.onError?.invoke(exception) // Usar el onError guardado
                }
        } catch (e: Exception) {
            this.onError?.invoke(e) // Usar el onError guardado
        }
    }
    
    private fun handleScanResult(data: android.content.Intent?) {
        try {
            val result = GmsDocumentScanningResult.fromActivityResultIntent(data)
            if (result != null) {
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
                onScanResult?.invoke(scanResult)
            } else {
                onError?.invoke(Exception("No se pudo obtener el resultado del escaneo"))
            }
        } catch (e: Exception) {
            onError?.invoke(e)
        }
    }
    
    fun updateOptions(options: GmsDocumentScannerOptions) {
        scanner = GmsDocumentScanning.getClient(options)
    }
    
    fun cleanup() {
        // El cliente de ML Kit se cierra automáticamente, no se necesita acción explícita.
    }
}

data class DocumentPage(
    val imageUri: Uri,
    val pageNumber: Int
)

data class DocumentPdf(
    val uri: Uri,
    val pageCount: Int
)

data class DocumentScanResult(
    val pages: List<DocumentPage>,
    val pdf: DocumentPdf?
)
