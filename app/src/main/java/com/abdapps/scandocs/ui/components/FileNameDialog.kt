package com.abdapps.scandocs.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Diálogo para nombrar archivos escaneados
 * 
 * Este composable permite al usuario:
 * - Ingresar un nombre personalizado para el archivo
 * - Ver información del documento escaneado
 * - Confirmar o cancelar la operación
 * 
 * @param isVisible Indica si el diálogo está visible
 * @param pageCount Número de páginas escaneadas
 * @param onConfirm Callback cuando se confirma el nombre
 * @param onDismiss Callback cuando se cancela o cierra el diálogo
 */
@Composable
fun FileNameDialog(
    isVisible: Boolean,
    pageCount: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        var fileName by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }
        
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Título del diálogo con estilo mejorado
                    Text(
                        text = "Guardar Documento",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Información del documento
                    Text(
                        text = "Páginas escaneadas: $pageCount",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    
                    // Campo de entrada para el nombre del archivo
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { 
                            fileName = it
                            isError = false
                        },
                        label = { Text("Nombre del archivo") },
                        placeholder = { Text("Ej: Documento_Importante") },
                        isError = isError,
                        supportingText = {
                            if (isError) {
                                Text("El nombre del archivo no puede estar vacío")
                            } else {
                                Text("Se guardarán archivos JPG y PDF")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Botones de acción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Botón cancelar
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Botón confirmar
                        Button(
                            onClick = {
                                if (fileName.isNotBlank()) {
                                    onConfirm(fileName.trim())
                                } else {
                                    isError = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Diálogo de confirmación para eliminar documentos
 * 
 * @param isVisible Indica si el diálogo está visible
 * @param fileName Nombre del archivo a eliminar
 * @param onConfirm Callback cuando se confirma la eliminación
 * @param onDismiss Callback cuando se cancela
 */
@Composable
fun DeleteConfirmationDialog(
    isVisible: Boolean,
    fileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Eliminar Documento",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que quieres eliminar '$fileName'?\n\nEsta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirm
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Diálogo para editar etiquetas
 * 
 * @param isVisible Indica si el diálogo está visible
 * @param currentTags Etiquetas actuales del documento
 * @param onConfirm Callback cuando se confirman las etiquetas
 * @param onDismiss Callback cuando se cancela
 */
@Composable
fun EditTagsDialog(
    isVisible: Boolean,
    currentTags: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        var tags by remember { mutableStateOf(currentTags) }
        
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Editar Etiquetas",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = "Separa las etiquetas con comas",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Etiquetas") },
                        placeholder = { Text("Ej: trabajo, importante, 2024") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Button(
                            onClick = { onConfirm(tags.trim()) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }
}
