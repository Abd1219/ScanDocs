package com.abdapps.scandocs.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abdapps.scandocs.R
import com.abdapps.scandocs.ScannerState

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
                text = stringResource(R.string.error_title), // Usar R.string.status_title si existiera
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = when (scannerState) {
                    ScannerState.IDLE -> stringResource(R.string.loading_default)
                    ScannerState.READY -> stringResource(R.string.ready_to_scan)
                    ScannerState.SCANNING -> stringResource(R.string.scanning_message)
                    ScannerState.PROCESSING -> stringResource(R.string.processing_doc)
                    ScannerState.SUCCESS -> stringResource(R.string.scan_success_msg)
                    ScannerState.ERROR -> stringResource(R.string.error_title)
                },
                color = when (scannerState) {
                    ScannerState.IDLE -> MaterialTheme.colorScheme.onSurface
                    ScannerState.READY -> MaterialTheme.colorScheme.primary
                    ScannerState.SCANNING -> MaterialTheme.colorScheme.secondary
                    ScannerState.PROCESSING -> MaterialTheme.colorScheme.secondary
                    ScannerState.SUCCESS -> MaterialTheme.colorScheme.primary
                    ScannerState.ERROR -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

/**
 * Botones de acción principales para el escáner
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
                text = if (isScanning) stringResource(R.string.scanning_message) else stringResource(R.string.scan_content_description),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        OutlinedButton(
            onClick = onClearResults,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.back_content_description)) // Placeholder for "Clear"
        }
    }
}

/**
 * Mensajes de estado (errores y éxitos)
 */
@Composable
fun StatusMessages(
    errorMessage: String?,
    successMessage: String?,
    onClearError: () -> Unit,
    onClearSuccess: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
}
