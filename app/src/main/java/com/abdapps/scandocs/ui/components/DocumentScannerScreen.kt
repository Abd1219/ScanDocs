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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.abdapps.scandocs.R
import com.abdapps.scandocs.ScannerState
import com.abdapps.scandocs.ScannerStatus
import com.abdapps.scandocs.ScannerUiState
import com.abdapps.scandocs.ScannerViewModel
import com.abdapps.scandocs.data.entity.DocumentEntity
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerScreen(
    viewModel: ScannerViewModel,
    modifier: Modifier = Modifier,
    onNavigateToHistory: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scannerState by viewModel.scannerState.collectAsState()
    val showSaveDialog by viewModel.showSaveDialog.collectAsState()
    val documentName by viewModel.documentName.collectAsState()
    val historyItems by viewModel.allDocuments.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    // Estado para diálogos del historial
    var showChooseFileDialogForView by remember { mutableStateOf<DocumentEntity?>(null) }
    var showChooseFileDialogForShare by remember { mutableStateOf<DocumentEntity?>(null) }
    var showConfirmDeleteDialog by remember { mutableStateOf<DocumentEntity?>(null) }

    // Manejo de mensajes de error
    LaunchedEffect(uiState.status) {
        val status = uiState.status
        if (status is ScannerStatus.Error) {
            val message = status.message.asString(context)
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = context.getString(R.string.retry_button),
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.retryLastOperation()
            }
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { uiText ->
            snackbarHostState.showSnackbar(
                message = uiText.asString(context),
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scandocs_title)) }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.status is ScannerStatus.Ready && (uiState.scannedPages.isEmpty() || scannerState == ScannerState.SUCCESS),
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                FloatingActionButton(
                    onClick = { viewModel.startScanning() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.scan_content_description)
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
            // Contenido principal: estado del escáner + historial
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // --- Sección: Estado del escáner ---
                item {
                    MainContent(
                        uiState = uiState,
                        scannerState = scannerState,
                        onRetry = { viewModel.retryLastOperation() }
                    )
                }

                // --- Sección: Historial ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.history_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (historyItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.empty_history),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(historyItems, key = { it.id }) { document ->
                        DocumentHistoryItem(
                            document = document,
                            dateFormatter = dateFormatter,
                            onViewClick = { showChooseFileDialogForView = document },
                            onShareClick = { showChooseFileDialogForShare = document },
                            onDeleteClick = { showConfirmDeleteDialog = document },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    // Espacio al final para que el FAB no tape el último ítem
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            // Overlay de loading/processing
            val status = uiState.status
            AnimatedVisibility(
                visible = status is ScannerStatus.Processing || status is ScannerStatus.Saving,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                if (status is ScannerStatus.Processing) {
                    LoadingOverlay(
                        isVisible = true,
                        progress = if (status.progress > 0f) status.progress else null,
                        message = status.message.asString()
                    )
                } else if (status is ScannerStatus.Saving) {
                    LoadingOverlay(
                        isVisible = true,
                        message = stringResource(R.string.saving_files)
                    )
                }
            }
        }
    }

    // --- Diálogos del historial ---
    showChooseFileDialogForView?.let { doc ->
        ChooseFileDialog(
            document = doc,
            actionType = stringResource(R.string.action_view),
            onDismiss = { showChooseFileDialogForView = null },
            onFileChosen = { filePath, mimeType ->
                viewModel.viewFile(context, filePath, mimeType)
                showChooseFileDialogForView = null
            }
        )
    }

    showChooseFileDialogForShare?.let { doc ->
        ChooseFileDialog(
            document = doc,
            actionType = stringResource(R.string.action_share),
            onDismiss = { showChooseFileDialogForShare = null },
            onFileChosen = { filePath, mimeType ->
                viewModel.shareFile(context, filePath, mimeType)
                showChooseFileDialogForShare = null
            }
        )
    }

    showConfirmDeleteDialog?.let { doc ->
        ConfirmDeleteDialog(
            documentName = doc.name,
            onDismiss = { showConfirmDeleteDialog = null },
            onConfirm = {
                viewModel.deleteDocument(doc)
                showConfirmDeleteDialog = null
            }
        )
    }

    // Diálogo para guardar documento tras escaneo
    if (showSaveDialog) {
        SaveDocumentDialog(
            documentName = documentName,
            onNameChange = { viewModel.updateDocumentName(it) },
            onSave = { viewModel.saveDocument(documentName) },
            onDismiss = { viewModel.dismissSaveDialog() },
            isProcessing = uiState.status is ScannerStatus.Saving || uiState.status is ScannerStatus.Processing
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
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
                val errorMessage = if (uiState.status is ScannerStatus.Error) {
                    (uiState.status as ScannerStatus.Error).message.asString()
                } else stringResource(R.string.unknown_error)

                ErrorState(
                    message = errorMessage,
                    canRetry = true,
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
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_scanpdftpe),
            contentDescription = stringResource(R.string.scandocs_title),
            modifier = Modifier
                .size(100.dp)
                .padding(bottom = 16.dp)
        )
        Text(
            text = stringResource(R.string.ready_to_scan),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.scan_instruction),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScanningState() {
    ScanningLoader(
        message = stringResource(R.string.scanning_message)
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
                text = stringResource(R.string.error_title),
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
                    Text(stringResource(R.string.retry_button))
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
                text = stringResource(R.string.scan_success_msg),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.scanned_pages_count, pageCount),
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
                    text = stringResource(R.string.save_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = documentName,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.doc_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing,
                    singleLine = false,
                    maxLines = 1,
                    placeholder = { Text(stringResource(R.string.doc_name_placeholder)) },
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
                            text = stringResource(R.string.save_dialog_supporting_text),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.save_dialog_formats_info),
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
                        Text(stringResource(R.string.cancel_button))
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
                        Text(if (isProcessing) stringResource(R.string.saving_button) else stringResource(R.string.save_button))
                    }
                }
            }
        }
    }
}