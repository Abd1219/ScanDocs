package com.abdapps.scandocs.ui.components

import android.content.Context // Necesario para futuras acciones de ver/compartir
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abdapps.scandocs.ScannerViewModel
import com.abdapps.scandocs.data.entity.DocumentEntity
import com.abdapps.scandocs.ui.theme.ScanDocsTheme
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit
) {
    val historyItems by viewModel.allDocuments.collectAsState(initial = emptyList())
    val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val context = LocalContext.current // Obtenemos el contexto aquí

    var showChooseFileDialogForView by remember { mutableStateOf<DocumentEntity?>(null) }
    var showChooseFileDialogForShare by remember { mutableStateOf<DocumentEntity?>(null) }
    var showConfirmDeleteDialog by remember { mutableStateOf<DocumentEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Escaneos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (historyItems.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No hay documentos en el historial.",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historyItems, key = { it.id }) { document ->
                        DocumentHistoryItem(
                            document = document,
                            dateFormatter = dateFormatter,
                            onViewClick = { showChooseFileDialogForView = document },
                            onShareClick = { showChooseFileDialogForShare = document },
                            onDeleteClick = { showConfirmDeleteDialog = document }
                        )
                    }
                }
            }
        }

        // --- DIÁLOGOS ---
        showChooseFileDialogForView?.let { doc ->
            ChooseFileDialog(
                document = doc,
                actionType = "Ver",
                onDismiss = { showChooseFileDialogForView = null },
                onFileChosen = { filePath, mimeType ->
                    viewModel.viewFile(context, filePath, mimeType) // LLAMADA AL VIEWMODEL
                    showChooseFileDialogForView = null
                }
            )
        }

        showChooseFileDialogForShare?.let { doc ->
            ChooseFileDialog(
                document = doc,
                actionType = "Compartir",
                onDismiss = { showChooseFileDialogForShare = null },
                onFileChosen = { filePath, mimeType ->
                    viewModel.shareFile(context, filePath, mimeType) // LLAMADA AL VIEWMODEL
                    showChooseFileDialogForShare = null
                }
            )
        }

        showConfirmDeleteDialog?.let { doc ->
            ConfirmDeleteDialog(
                documentName = doc.name,
                onDismiss = { showConfirmDeleteDialog = null },
                onConfirm = {
                    viewModel.deleteDocument(doc) // LLAMADA AL VIEWMODEL
                    showConfirmDeleteDialog = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentHistoryItem(
    document: DocumentEntity,
    dateFormatter: SimpleDateFormat,
    onViewClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = document.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Guardado el: ${dateFormatter.format(document.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (document.pdfPath.isNotBlank()) {
                Text(
                    text = "PDF: ${document.pdfPath.substringAfterLast('/')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            if (document.jpgPath.isNotBlank()) {
                 Text(
                    text = "JPG: ${document.jpgPath.substringAfterLast('/')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Compartir documento",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Eliminar documento",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ChooseFileDialog(
    document: DocumentEntity,
    actionType: String,
    onDismiss: () -> Unit,
    onFileChosen: (filePath: String, mimeType: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$actionType Documento: ${document.name}") },
        text = {
            Column {
                Text("¿Qué formato deseas $actionType?")
                Spacer(modifier = Modifier.height(16.dp))
                if (document.pdfPath.isNotBlank()) {
                    TextButton(onClick = { onFileChosen(document.pdfPath, "application/pdf") }) {
                        Text("PDF (${document.pdfPath.substringAfterLast('/')})")
                    }
                }
                if (document.jpgPath.isNotBlank()) {
                    TextButton(onClick = { onFileChosen(document.jpgPath, "image/jpeg") }) {
                        Text("JPG (${document.jpgPath.substringAfterLast('/')})")
                    }
                }
                 if (document.jpgPath.isBlank() && document.pdfPath.isBlank()) {
                    Text("No hay archivos disponibles para este documento.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    documentName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar Eliminación") },
        text = { Text("¿Estás seguro de que deseas eliminar el documento \"$documentName\"? Esta acción no se puede deshacer.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
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


@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    ScanDocsTheme {
        val sampleDate = java.util.Date()
        val sampleDocuments = listOf(
            DocumentEntity(1, "Factura Luz.pdf", "/path/to/jpg1.jpg", "/path/to/pdf1.pdf", sampleDate, "/path/to/thumb1.jpg"),
            DocumentEntity(2, "Contrato Alquiler.jpg", "/path/to/jpg2.jpg", "", sampleDate, "/path/to/thumb2.jpg")
        )
        val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        var showChooseFileDialogForViewP by remember { mutableStateOf<DocumentEntity?>(null) }
        var showChooseFileDialogForShareP by remember { mutableStateOf<DocumentEntity?>(null) }
        var showConfirmDeleteDialogP by remember { mutableStateOf<DocumentEntity?>(null) }
        // Dummy ViewModel para la preview, ya que el ViewModel real requiere dependencias.
        // val dummyViewModel = ScannerViewModel(repository = ..., fileService = ... ) 

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Historial (Preview)") }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                if (sampleDocuments.isEmpty()) {
                    Text("No hay documentos en el historial.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(sampleDocuments) { document ->
                            DocumentHistoryItem(
                                document = document,
                                dateFormatter = dateFormatter,
                                onViewClick = { showChooseFileDialogForViewP = document },
                                onShareClick = { showChooseFileDialogForShareP = document },
                                onDeleteClick = { showConfirmDeleteDialogP = document }
                            )
                        }
                    }
                }
                 showChooseFileDialogForViewP?.let { doc ->
                    ChooseFileDialog(
                        document = doc, actionType = "Ver",
                        onDismiss = { showChooseFileDialogForViewP = null },
                        // En Preview, solo cerramos el diálogo, no llamamos al ViewModel
                        onFileChosen = { _, _ -> showChooseFileDialogForViewP = null }
                    )
                }
                showChooseFileDialogForShareP?.let { doc ->
                    ChooseFileDialog(
                        document = doc, actionType = "Compartir",
                        onDismiss = { showChooseFileDialogForShareP = null },
                        onFileChosen = { _, _ -> showChooseFileDialogForShareP = null }
                    )
                }
                showConfirmDeleteDialogP?.let { doc ->
                    ConfirmDeleteDialog(
                        documentName = doc.name,
                        onDismiss = { showConfirmDeleteDialogP = null },
                        onConfirm = { showConfirmDeleteDialogP = null }
                    )
                }
            }
        }
    }
}
