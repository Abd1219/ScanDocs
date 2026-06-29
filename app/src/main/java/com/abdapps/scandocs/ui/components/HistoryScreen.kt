package com.abdapps.scandocs.ui.components

import android.content.Context // Necesario para futuras acciones de ver/compartir
import androidx.compose.foundation.clickable
import com.abdapps.scandocs.R
import com.abdapps.scandocs.ScannerViewModel
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    // Reiniciar el escáner cuando se muestra la pantalla de historial
    LaunchedEffect(Unit) {
        viewModel.clearResults()
    }
    val historyItems by viewModel.allDocuments.collectAsState(initial = emptyList())
    val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val context = LocalContext.current // Obtenemos el contexto aquí

    var showChooseFileDialogForView by remember { mutableStateOf<DocumentEntity?>(null) }
    var showChooseFileDialogForShare by remember { mutableStateOf<DocumentEntity?>(null) }
    var showConfirmDeleteDialog by remember { mutableStateOf<DocumentEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.history_title),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_content_description),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                        text = stringResource(R.string.empty_history),
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
                actionType = stringResource(R.string.action_view),
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
                actionType = stringResource(R.string.action_share),
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
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
                text = stringResource(R.string.saved_on_label, dateFormatter.format(document.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (document.pdfPath.isNotBlank()) {
                Text(
                    text = stringResource(R.string.pdf_label, document.pdfPath.substringAfterLast('/')),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            if (document.jpgPath.isNotBlank()) {
                val pageCount = document.getPageCount()
                val jpgLabel = if (pageCount > 1) {
                    stringResource(R.string.jpg_label_multiple, pageCount)
                } else {
                    stringResource(R.string.jpg_label_single, document.jpgPath.substringAfterLast('/'))
                }
                Text(
                    text = jpgLabel,
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
                        contentDescription = stringResource(R.string.share_doc_content_description),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_doc_content_description),
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
    val jpgPaths = document.getJpgPaths()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_format_title, actionType, document.name)) },
        text = {
            Column {
                Text(stringResource(R.string.choose_format_message, actionType))
                Spacer(modifier = Modifier.height(16.dp))
                
                // Mostrar opción PDF si existe
                if (document.pdfPath.isNotBlank()) {
                    TextButton(onClick = { onFileChosen(document.pdfPath, "application/pdf") }) {
                        Text(stringResource(R.string.format_pdf_full, document.pdfPath.substringAfterLast('/')))
                    }
                    if (jpgPaths.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // Mostrar todas las páginas JPG individuales
                if (jpgPaths.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.individual_pages_header),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    jpgPaths.forEachIndexed { index, jpgPath ->
                        TextButton(onClick = { onFileChosen(jpgPath, "image/jpeg") }) {
                            Text(stringResource(R.string.format_jpg_page, index + 1, jpgPath.substringAfterLast('/')))
                        }
                    }
                }
                
                // Mensaje si no hay archivos
                if (jpgPaths.isEmpty() && document.pdfPath.isBlank()) {
                    Text(stringResource(R.string.no_files_available))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
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
        title = { Text(stringResource(R.string.confirm_delete_title)) },
        text = { Text(stringResource(R.string.confirm_delete_msg, documentName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
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
                        document = doc, actionType = stringResource(R.string.action_view),
                        onDismiss = { showChooseFileDialogForViewP = null },
                        // En Preview, solo cerramos el diálogo, no llamamos al ViewModel
                        onFileChosen = { _, _ -> showChooseFileDialogForViewP = null }
                    )
                }
                showChooseFileDialogForShareP?.let { doc ->
                    ChooseFileDialog(
                        document = doc, actionType = stringResource(R.string.action_share),
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
