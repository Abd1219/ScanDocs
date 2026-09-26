package com.abdapps.scandocs.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abdapps.scandocs.ScannerViewModel
import com.abdapps.scandocs.data.entity.DocumentEntity
import com.abdapps.scandocs.ui.theme.ScanDocsTheme
import java.util.Date

// ==========================================
// Pantalla de Historial Completo
// Muestra TODOS los documentos guardados
// con búsqueda en tiempo real
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val allDocuments by viewModel.allDocuments.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Notificaciones de error y éxito
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

    // Estado de búsqueda
    var searchQuery by remember { mutableStateOf("") }

    // Filtrado reactivo en memoria (sin hits a Room)
    val filteredDocuments = remember(allDocuments, searchQuery) {
        if (searchQuery.isBlank()) allDocuments
        else allDocuments.filter { doc ->
            doc.name.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    // Estados para diálogos
    var showViewDialog by remember { mutableStateOf<DocumentEntity?>(null) }
    var showShareDialog by remember { mutableStateOf<DocumentEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<DocumentEntity?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Todos los documentos",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (allDocuments.isNotEmpty()) {
                            Text(
                                text = "${allDocuments.size} documento${if (allDocuments.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ─── BARRA DE BÚSQUEDA ────────────────────────────────
            if (allDocuments.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Buscar documentos...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            }

            // ─── LISTA DE DOCUMENTOS ──────────────────────────────
            when {
                allDocuments.isEmpty() -> {
                    // Estado vacío total
                    HistoryEmptyState(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                filteredDocuments.isEmpty() -> {
                    // Estado vacío de búsqueda
                    SearchEmptyState(
                        query = searchQuery,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredDocuments,
                            key = { it.id }
                        ) { document ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 5 }
                            ) {
                                DocumentItemCard(
                                    document = document,
                                    onViewClick = { showViewDialog = document },
                                    onShareClick = { showShareDialog = document },
                                    onDeleteClick = { showDeleteDialog = document }
                                )
                            }
                        }
                        // Espacio final para evitar que el último item quede tapado
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }

        // ─── DIÁLOGO: Ver documento ───────────────────────────────
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

        // ─── DIÁLOGO: Compartir ───────────────────────────────────
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

        // ─── DIÁLOGO: Confirmar eliminación ──────────────────────
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
    }
}

// ==========================================
// Estado vacío: Sin documentos guardados
// ==========================================
@Composable
private fun HistoryEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sin documentos guardados",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Los documentos que escanees aparecerán aquí organizados por fecha.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ==========================================
// Estado vacío: Sin resultados de búsqueda
// ==========================================
@Composable
private fun SearchEmptyState(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sin resultados",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No se encontraron documentos para \"$query\".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ==========================================
// Diálogo: Elegir formato para Ver/Compartir
// ==========================================
@Composable
fun ChooseFileDialog(
    document: DocumentEntity,
    actionType: String,
    onDismiss: () -> Unit,
    onFileChosen: (filePath: String, mimeType: String) -> Unit,
    onJpgsChosen: ((paths: List<String>, documentName: String) -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "$actionType documento",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Selecciona el formato:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (document.pdfPath.isNotBlank()) {
                    TextButton(
                        onClick = { onFileChosen(document.pdfPath, "application/pdf") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "PDF",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (document.allJpgPaths.isNotEmpty()) {
                    TextButton(
                        onClick = { onFileChosen(document.allJpgPaths.first(), "image/jpeg") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (document.allJpgPaths.size > 1) "Primera imagen JPG" else "JPG / Imagen",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (actionType == "Compartir" && document.allJpgPaths.size > 1 && onJpgsChosen != null) {
                    TextButton(
                        onClick = { onJpgsChosen(document.allJpgPaths, document.name) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Todas las imágenes JPG (${document.allJpgPaths.size})",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (document.allJpgPaths.isEmpty() && document.pdfPath.isBlank()) {
                    Text(
                        "No hay archivos disponibles para este documento.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// ==========================================
// Diálogo: Confirmar eliminación
// ==========================================
@Composable
fun ConfirmDeleteDialog(
    documentName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Eliminar documento",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = "¿Deseas eliminar \"$documentName\"?\n\nEsta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// ==========================================
// Previews para desarrollo (Claro y Oscuro)
// ==========================================
@Preview(name = "History Screen - Light", showBackground = true)
@Preview(name = "History Screen - Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HistoryScreenContentPreview() {
    val sampleDocs = listOf(
        DocumentEntity(1, "Ekt Motos Martínez De La Torre", "path.jpg", "path.pdf", Date(), "thumb.jpg"),
        DocumentEntity(2, "Instituto Científico De Educación Superior", "path.jpg", "path.pdf", Date(), "thumb.jpg"),
        DocumentEntity(3, "Guamúchilito Documento", "path.jpg", "", Date(), "thumb.jpg"),
        DocumentEntity(4, "Operadora De Hospitales Angeles", "path.jpg", "path.pdf", Date(), "thumb.jpg")
    )
    ScanDocsTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Barra de búsqueda simulada
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Buscar documentos...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sampleDocs, key = { it.id }) { doc ->
                        DocumentItemCard(
                            document = doc,
                            onViewClick = {},
                            onShareClick = {},
                            onDeleteClick = {}
                        )
                    }
                }
            }
        }
    }
}
