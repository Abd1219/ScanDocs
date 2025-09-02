package com.abdapps.scandocs.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abdapps.scandocs.*

/**
 * Componente principal del escáner de documentos
 * 
 * Este composable proporciona la interfaz principal para:
 * - Mostrar el estado del escáner
 * - Botones para iniciar el escaneo
 * - Visualización de resultados
 * - Manejo de errores y mensajes
 * 
 * @param uiState Estado actual de la UI
 * @param scannerState Estado actual del escáner
 * @param onStartScan Callback para iniciar el escaneo
 * @param onClearResults Callback para limpiar resultados
 * @param onClearError Callback para limpiar errores
 * @param onClearSuccess Callback para limpiar mensajes de éxito
 */
@Composable
fun DocumentScannerScreen(
    uiState: ScannerUiState,
    scannerState: ScannerState,
    onStartScan: () -> Unit,
    onClearResults: () -> Unit,
    onClearError: () -> Unit,
    onClearSuccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título de la aplicación
        Text(
            text = "Escáner de Documentos",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Estado del escáner
        ScannerStatusCard(scannerState = scannerState)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Botones de acción
        ActionButtons(
            isScannerReady = uiState.isScannerReady,
            isScanning = uiState.isScanning,
            onStartScan = onStartScan,
            onClearResults = onClearResults
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mensajes de estado
        StatusMessages(
            errorMessage = uiState.errorMessage,
            successMessage = uiState.successMessage,
            onClearError = onClearError,
            onClearSuccess = onClearSuccess
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Resultados del escaneo
        ScanResults(
            scannedPages = uiState.scannedPages,
            generatedPdf = uiState.generatedPdf
        )
    }
}

/**
 * Tarjeta que muestra el estado actual del escáner
 * 
 * @param scannerState Estado actual del escáner
 */
@Composable
fun ScannerStatusCard(scannerState: ScannerState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Estado del Escáner",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = when (scannerState) {
                    ScannerState.IDLE -> "Inicializando..."
                    ScannerState.READY -> "Listo para escanear"
                    ScannerState.SCANNING -> "Escaneando documento..."
                    ScannerState.PROCESSING -> "Procesando documento..." // AÑADIDO
                    ScannerState.SUCCESS -> "Escaneo exitoso"      // CAMBIADO de COMPLETED
                    ScannerState.ERROR -> "Error en el escáner"
                },
                color = when (scannerState) {
                    ScannerState.IDLE -> MaterialTheme.colorScheme.onSurface // AÑADIDO
                    ScannerState.READY -> MaterialTheme.colorScheme.primary
                    ScannerState.SCANNING -> MaterialTheme.colorScheme.secondary
                    ScannerState.PROCESSING -> MaterialTheme.colorScheme.secondary // AÑADIDO (puedes ajustar el color)
                    ScannerState.SUCCESS -> MaterialTheme.colorScheme.primary    // CAMBIADO (puedes usar tertiary o un color verde)
                    ScannerState.ERROR -> MaterialTheme.colorScheme.error
                    // No se necesita 'else' si todos los casos están cubiertos explícitamente
                }
            )
        }
    }
}

/**
 * Botones de acción principales
 * 
 * @param isScannerReady Indica si el escáner está listo
 * @param isScanning Indica si se está escaneando
 * @param onStartScan Callback para iniciar el escaneo
 * @param onClearResults Callback para limpiar resultados
 */
@Composable
fun ActionButtons(
    isScannerReady: Boolean,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onClearResults: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = onStartScan,
            enabled = isScannerReady && !isScanning,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (isScanning) "Escaneando..." else "Escanear Documento",
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        OutlinedButton(
            onClick = onClearResults,
            modifier = Modifier.weight(1f)
        ) {
            Text("Limpiar Resultados")
        }
    }
}

/**
 * Mensajes de estado (errores y éxitos)
 * 
 * @param errorMessage Mensaje de error (opcional)
 * @param successMessage Mensaje de éxito (opcional)
 * @param onClearError Callback para limpiar errores
 * @param onClearSuccess Callback para limpiar mensajes de éxito
 */
@Composable
fun StatusMessages(
    errorMessage: String?,
    successMessage: String?,
    onClearError: () -> Unit,
    onClearSuccess: () -> Unit
) {
    // Mensaje de error
    errorMessage?.let { error ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClearError) {
                    Text("×", fontSize = 20.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    
    // Mensaje de éxito
    successMessage?.let { success ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = success,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClearSuccess) {
                    Text("×", fontSize = 20.sp)
                }
            }
        }
    }
}

/**
 * Visualización de los resultados del escaneo
 * 
 * @param scannedPages Lista de páginas escaneadas
 * @param generatedPdf Información del PDF generado
 */
@Composable
fun ScanResults(
    scannedPages: List<DocumentPage>,
    generatedPdf: DocumentPdf?
) {
    if (scannedPages.isNotEmpty() || generatedPdf != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Resultados del Escaneo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Información del PDF
                generatedPdf?.let { pdf ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "PDF Generado",
                                fontWeight = FontWeight.Medium
                            )
                            Text("Páginas: ${pdf.pageCount}")
                            Text("URI: ${pdf.uri}")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Lista de páginas escaneadas
                if (scannedPages.isNotEmpty()) {
                    Text(
                        text = "Páginas Escaneadas (${scannedPages.size})",
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(scannedPages) { page ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Página ${page.pageNumber}",
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "URI: ${page.imageUri}",
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
