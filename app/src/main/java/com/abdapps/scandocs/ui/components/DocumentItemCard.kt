package com.abdapps.scandocs.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abdapps.scandocs.data.entity.DocumentEntity
import com.abdapps.scandocs.ui.theme.JpgBadgeContainer
import com.abdapps.scandocs.ui.theme.JpgBadgeContainerDark
import com.abdapps.scandocs.ui.theme.OnJpgBadge
import com.abdapps.scandocs.ui.theme.OnJpgBadgeDark
import com.abdapps.scandocs.ui.theme.OnPdfBadge
import com.abdapps.scandocs.ui.theme.OnPdfBadgeDark
import com.abdapps.scandocs.ui.theme.PdfBadgeContainer
import com.abdapps.scandocs.ui.theme.PdfBadgeContainerDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Componente reutilizable para mostrar un documento en la pantalla de inicio (recientes)
 * y en la pantalla de historial completo.
 *
 * Diseño compacto, refinado y profesional inspirado en Google Files y Drive:
 * - Icono estilizado con squircle y tono acorde al formato
 * - Título del documento legible con semibold
 * - Línea secundaria limpia: Fecha • Formatos (PDF/JPG) sin exceso de altura vertical
 * - Menú discreto de 3 puntos
 */
@Composable
fun DocumentItemCard(
    document: DocumentEntity,
    onViewClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onViewClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Icono de documento con contenedor squircle tonal
            DocumentIconBadge(hasPdf = document.pdfPath.isNotBlank())

            // 2. Columna central con 2 líneas limpias (Nombre + Fecha/Formatos)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatDocumentDate(document.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (document.pdfPath.isNotBlank() || document.jpgPath.isNotBlank()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }

                    if (document.pdfPath.isNotBlank()) {
                        FormatBadge(label = "PDF", isPdf = true)
                    }
                    if (document.jpgPath.isNotBlank()) {
                        FormatBadge(label = "JPG", isPdf = false)
                    }
                }
            }

            // 3. Menú de acciones (tres puntos)
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Más opciones",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Ver documento") },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Outlined.InsertDriveFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onViewClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Compartir") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onShareClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Eliminar",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Icono de documento con contenedor squircle y color según formato.
 */
@Composable
private fun DocumentIconBadge(hasPdf: Boolean) {
    val isDark = isSystemInDarkTheme()

    val bgColor = if (hasPdf) {
        if (isDark) PdfBadgeContainerDark else PdfBadgeContainer
    } else {
        if (isDark) JpgBadgeContainerDark else JpgBadgeContainer
    }

    val iconColor = if (hasPdf) {
        if (isDark) OnPdfBadgeDark else OnPdfBadge
    } else {
        if (isDark) OnJpgBadgeDark else OnJpgBadge
    }

    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (hasPdf) Icons.Filled.PictureAsPdf else Icons.Outlined.Image,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Badge compacto con fondo de color sutil para indicar el formato del archivo (PDF o JPG).
 */
@Composable
private fun FormatBadge(label: String, isPdf: Boolean) {
    val isDark = isSystemInDarkTheme()

    val bgColor = if (isPdf) {
        if (isDark) PdfBadgeContainerDark else PdfBadgeContainer
    } else {
        if (isDark) JpgBadgeContainerDark else JpgBadgeContainer
    }

    val textColor = if (isPdf) {
        if (isDark) OnPdfBadgeDark else OnPdfBadge
    } else {
        if (isDark) OnJpgBadgeDark else OnJpgBadge
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

/**
 * Formatea la fecha del documento de manera amigable:
 * - Hoy → "Hoy, HH:mm"
 * - Ayer → "Ayer, HH:mm"
 * - Resto → "dd MMM yyyy, HH:mm"
 */
fun formatDocumentDate(date: Date): String {
    val now = Calendar.getInstance()
    val doc = Calendar.getInstance().apply { time = date }

    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val fullFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    return when {
        now.get(Calendar.YEAR) == doc.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == doc.get(Calendar.DAY_OF_YEAR) ->
            "Hoy, ${timeFormatter.format(date)}"
        now.get(Calendar.YEAR) == doc.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - doc.get(Calendar.DAY_OF_YEAR) == 1 ->
            "Ayer, ${timeFormatter.format(date)}"
        else ->
            "${fullFormatter.format(date)}, ${timeFormatter.format(date)}"
    }
}
