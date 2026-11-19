package com.abdapps.scandocs.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.abdapps.scandocs.ScannerState
import com.abdapps.scandocs.ScannerViewModel
import com.abdapps.scandocs.data.entity.DocumentEntity

// ==========================================
// Pantalla de Inicio (HomeScreen Optimizada)
// Estructura equilibrada, compacta y profesional
// ==========================================

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
    val recentDocuments by viewModel.recentDocuments.collectAsState(initial = emptyList())

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Diálogos de acción
    var showViewDialog by remember { mutableStateOf<DocumentEntity?>(null) }
    var showShareDialog by remember { mutableStateOf<DocumentEntity?>(null) }
    var showRenameDialog by remember { mutableStateOf<DocumentEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<DocumentEntity?>(null) }

    // Notificaciones de estado (Snackbar)
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (uiState.isScannerReady && !uiState.isScanning) {
                        viewModel.startScanning()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 3.dp,
                    pressedElevation = 6.dp
                ),
                modifier = Modifier
                    .padding(end = 4.dp, bottom = 8.dp)
                    .size(56.dp)
            ) {
                if (scannerState == ScannerState.SCANNING || scannerState == ScannerState.PROCESSING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Escanear documento",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ─── 1. BARRA DE BÚSQUEDA SUPERIOR ──────────────────────
            HomeSearchBar(
                onClick = onNavigateToHistory
            )

            // ─── 2. ACCIONES RÁPIDAS COHESIONADAS ───────────────────
            QuickActionsSection(
                onScanClick = { viewModel.startScanning() },
                onViewAllClick = onNavigateToHistory
            )

            // ─── 3. SECCIÓN DE DOCUMENTOS RECIENTES ─────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Título "Recientes" + Contador + Enlace "Ver todo >"
                RecentSectionHeader(
                    count = recentDocuments.size,
                    onViewAllClick = onNavigateToHistory
                )

                // Estado vacío compacto o lista de hasta 3 documentos
                AnimatedVisibility(
                    visible = recentDocuments.isEmpty(),
                    enter = fadeIn(tween(200))
                ) {
                    CompactEmptyRecentState(
                        onScanNewClick = { viewModel.startScanning() }
                    )
                }

                AnimatedVisibility(
                    visible = recentDocuments.isNotEmpty(),
                    enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 8 }
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentDocuments.take(3).forEach { doc ->
                            DocumentItemCard(
                                document = doc,
                                onViewClick = { showViewDialog = doc },
                                onShareClick = { showShareDialog = doc },
                                onRenameClick = { showRenameDialog = doc },
                                onDeleteClick = { showDeleteDialog = doc }
                            )
                        }
                    }
                }
            }

            // Espacio final natural para que el contenido no quede cortado
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // ─── DIÁLOGOS DE GESTIÓN ──────────────────────────────────────
    if (showSaveDialog) {
        SaveDocumentDialog(
            documentName = documentName,
            onNameChange = { viewModel.updateDocumentName(it) },
            onSave = { viewModel.saveDocument() },
            onDismiss = { viewModel.dismissSaveDialog() }
        )
    }

    showViewDialog?.let { doc ->
        ChooseFileDialog(
            document = doc,
            actionType = "Ver",
            onDismiss = { showViewDialog = null },
            onFileChosen = { filePath, mimeType ->
                viewModel.viewFile(context, filePath, mimeType)
                showViewDialog = null
            }
        )
    }

    showShareDialog?.let { doc ->
        ChooseFileDialog(
            document = doc,
            actionType = "Compartir",
            onDismiss = { showShareDialog = null },
            onFileChosen = { filePath, mimeType ->
                viewModel.shareFile(context, filePath, mimeType)
                showShareDialog = null
            },
            onJpgsChosen = { paths, name ->
                viewModel.shareJpgFiles(context, paths, name)
                showShareDialog = null
            }
        )
    }

    showDeleteDialog?.let { doc ->
        ConfirmDeleteDialog(
            documentName = doc.name,
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                viewModel.deleteDocument(doc)
                showDeleteDialog = null
            }
        )
    }

    showRenameDialog?.let { doc ->
        RenameDocumentDialog(
            currentName = doc.name,
            onDismiss = { showRenameDialog = null },
            onConfirm = { newName ->
                viewModel.renameDocument(doc, newName)
                showRenameDialog = null
            }
        )
    }
}

// ==========================================
// Diálogo para guardar el nombre del documento
// ==========================================
@Composable
fun SaveDocumentDialog(
    documentName: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Guardar documento",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ingresa un nombre para guardar el documento escaneado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(18.dp))
                OutlinedTextField(
                    value = documentName,
                    onValueChange = onNameChange,
                    label = { Text("Nombre del documento") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onSave,
                        enabled = documentName.isNotBlank()
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}
