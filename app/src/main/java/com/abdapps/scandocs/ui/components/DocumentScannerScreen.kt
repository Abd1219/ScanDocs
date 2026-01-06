package com.abdapps.scandocs.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.abdapps.scandocs.R
import com.abdapps.scandocs.ScannerState
import com.abdapps.scandocs.ScannerUiState
import com.abdapps.scandocs.ScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerScreen(
    viewModel: ScannerViewModel,
    onNavigateToHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scannerState by viewModel.scannerState.collectAsState()
    val showSaveDialog by viewModel.showSaveDialog.collectAsState()
    val documentName by viewModel.documentName.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Manejo de mensajes de error y éxito
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (uiState.canRetry) "Reintentar" else null,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed && uiState.canRetry) {
                viewModel.retryLastOperation()
            }
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ScanDocs") },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Historial"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = scannerState == ScannerState.READY && !uiState.isLoading,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                FloatingActionButton(
                    onClick = { viewModel.startScanning() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Escanear documento"
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Contenido principal
            MainContent(
                uiState = uiState,
                scannerState = scannerState,
                onRetry = { viewModel.retryLastOperation() }
            )
            
            // Overlay de loading/processing
            AnimatedVisibility(
                visible = uiState.isLoading || uiState.isProcessing,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                LoadingOverlay(
                    isProcessing = uiState.isProcessing,
                    progress = uiState.processingProgress,
                    message = uiState.loadingMessage ?: "Cargando..."
                )
            }
        }
    }
    
    if (showSaveDialog) {
        SaveDocumentDialog(
            documentName = documentName,
            onNameChange = { viewModel.updateDocumentName(it) },
            onSave = { viewModel.saveDocument() },
            onDismiss = { viewModel.dismissSaveDialog() },
            isProcessing = uiState.isProcessing
        )
    }
}

@Composable
private fun MainContent(
    uiState: ScannerUiState,
    scannerState: ScannerState,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (scannerState) {
            ScannerState.IDLE, ScannerState.READY -> {
                ReadyState()
            }
            ScannerState.SCANNING -> {
                ScanningState()
            }
            ScannerState.ERROR -> {
                ErrorState(
                    message = uiState.errorMessage ?: "Error desconocido",
                    canRetry = uiState.canRetry,
                    onRetry = onRetry
                )
            }
            ScannerState.SUCCESS -> {
                SuccessState(pageCount = uiState.scannedPages.size)
            }
            ScannerState.PROCESSING -> {
                // El estado de procesamiento se maneja en el overlay
                Box {}
            }
        }
    }
}

@Composable
private fun ReadyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_scanpdftpe),
            contentDescription = "Logo ScanDocs",
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 24.dp)
        )
        Text(
            text = "Listo para escanear documentos",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Presiona el botón + para comenzar a escanear",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScanningState() {
    ScanningLoader(
        message = "Escaneando documento..."
    )
}

@Composable
private fun ErrorState(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            if (canRetry) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onRetry,
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reintentar")
                }
            }
        }
    }
}

@Composable
private fun SuccessState(pageCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¡Documento escaneado!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Páginas escaneadas: $pageCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun LoadingOverlay(
    isProcessing: Boolean,
    progress: Float,
    message: String
) {
    LoadingOverlay(
        isVisible = true,
        progress = if (isProcessing && progress > 0f) progress else null,
        message = message
    )
}

@Composable
fun SaveDocumentDialog(
    documentName: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    isProcessing: Boolean = false
) {
    Dialog(onDismissRequest = if (isProcessing) { {} } else onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Guardar documento",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = documentName,
                    onValueChange = onNameChange, // Pasar directamente sin filtrar
                    label = { Text("Nombre del documento") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing,
                    singleLine = true,
                    placeholder = { Text("Ej: Mi Documento 2024") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            if (documentName.trim().isNotEmpty() && !isProcessing) {
                                onSave()
                            }
                        }
                    ),
                    supportingText = { 
                        Text(
                            text = "Usa cualquier nombre. Los caracteres especiales se ajustarán automáticamente.",
                            style = MaterialTheme.typography.bodySmall
                        ) 
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "El documento se guardará como imagen JPG y PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isProcessing
                    ) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSave,
                        enabled = documentName.trim().isNotEmpty() && !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isProcessing) "Guardando..." else "Guardar")
                    }
                }
            }
        }
    }
}