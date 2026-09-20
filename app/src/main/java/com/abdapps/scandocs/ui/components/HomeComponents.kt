package com.abdapps.scandocs.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abdapps.scandocs.ui.theme.QuickActionDocsBgDark
import com.abdapps.scandocs.ui.theme.QuickActionDocsBgLight
import com.abdapps.scandocs.ui.theme.QuickActionDocsIconDark
import com.abdapps.scandocs.ui.theme.QuickActionDocsIconLight
import com.abdapps.scandocs.ui.theme.QuickActionGalleryBgDark
import com.abdapps.scandocs.ui.theme.QuickActionGalleryBgLight
import com.abdapps.scandocs.ui.theme.QuickActionGalleryIconDark
import com.abdapps.scandocs.ui.theme.QuickActionGalleryIconLight
import com.abdapps.scandocs.ui.theme.QuickActionScanBgDark
import com.abdapps.scandocs.ui.theme.QuickActionScanBgLight
import com.abdapps.scandocs.ui.theme.QuickActionScanIconDark
import com.abdapps.scandocs.ui.theme.QuickActionScanIconLight

// ==========================================
// Barra de búsqueda superior tipo Píldora
// Refinada, ligera y elegante estilo Google Files / Drive
// ==========================================
@Composable
fun HomeSearchBar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Buscar documentos",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Buscar documentos...",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ==========================================
// Sección de Accesos Rápidos Reales
// Enmarcada en un panel cohesionado y equilibrado
// ==========================================
@Composable
fun QuickActionsSection(
    onScanClick: () -> Unit,
    onImportClick: () -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Escanear (Acción Principal Destacada)
            QuickActionItem(
                icon = Icons.Outlined.DocumentScanner,
                label = "Escanear",
                containerColor = if (isDark) QuickActionScanBgDark else QuickActionScanBgLight,
                iconColor = if (isDark) QuickActionScanIconDark else QuickActionScanIconLight,
                isProminent = true,
                onClick = onScanClick
            )

            // 2. Importar imagen (Galería)
            QuickActionItem(
                icon = Icons.Outlined.AddPhotoAlternate,
                label = "Importar",
                containerColor = if (isDark) QuickActionGalleryBgDark else QuickActionGalleryBgLight,
                iconColor = if (isDark) QuickActionGalleryIconDark else QuickActionGalleryIconLight,
                isProminent = false,
                onClick = onImportClick
            )

            // 3. Documentos (Historial completo)
            QuickActionItem(
                icon = Icons.Outlined.FolderOpen,
                label = "Documentos",
                containerColor = if (isDark) QuickActionDocsBgDark else QuickActionDocsBgLight,
                iconColor = if (isDark) QuickActionDocsIconDark else QuickActionDocsIconLight,
                isProminent = false,
                onClick = onViewAllClick
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    iconColor: Color,
    isProminent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .then(
                    if (isProminent) {
                        Modifier.border(
                            width = 1.dp,
                            color = iconColor.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                    } else Modifier
                ),
            shape = CircleShape,
            color = containerColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isProminent) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 12.sp
            ),
            color = if (isProminent) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ==========================================
// Encabezado de la Sección de Recientes
// "Recientes" + Badge Contador + "Ver todo >"
// ==========================================
@Composable
fun RecentSectionHeader(
    count: Int = 0,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Recientes",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            if (count > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onViewAllClick() }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Ver todo",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Ver todo",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

// ==========================================
// Estado Vacío Compacto y Elegante
// ==========================================
@Composable
fun CompactEmptyRecentState(
    onScanNewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val lineColor = if (isDark) Color(0xFF00BFA5).copy(alpha = 0.7f) else Color(0xFF00796B).copy(alpha = 0.7f)
    val pageBgColor = if (isDark) Color(0xFF22262E) else Color(0xFFFFFFFF)
    val pageBorderColor = if (isDark) Color(0xFF383E4B) else Color(0xFFD6DBE1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Ilustración minimalista de documento
        Box(
            modifier = Modifier.size(width = 76.dp, height = 98.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(width = 72.dp, height = 94.dp)
                    .border(
                        width = 1.dp,
                        color = pageBorderColor,
                        shape = RoundedCornerShape(8.dp)
                    ),
                shape = RoundedCornerShape(8.dp),
                color = pageBgColor,
                shadowElevation = if (isDark) 0.dp else 1.dp
            ) {
                Canvas(modifier = Modifier.padding(12.dp)) {
                    val lineStroke = 2.5.dp.toPx()
                    val lineRadius = CornerRadius(lineStroke / 2, lineStroke / 2)

                    drawRoundRect(
                        color = lineColor,
                        topLeft = Offset(size.width * 0.15f, size.height * 0.12f),
                        size = Size(size.width * 0.7f, lineStroke),
                        cornerRadius = lineRadius
                    )
                    drawRoundRect(
                        color = lineColor.copy(alpha = 0.5f),
                        topLeft = Offset(size.width * 0.08f, size.height * 0.32f),
                        size = Size(size.width * 0.84f, lineStroke),
                        cornerRadius = lineRadius
                    )
                    drawRoundRect(
                        color = lineColor.copy(alpha = 0.5f),
                        topLeft = Offset(size.width * 0.08f, size.height * 0.52f),
                        size = Size(size.width * 0.84f, lineStroke),
                        cornerRadius = lineRadius
                    )
                    drawRoundRect(
                        color = lineColor.copy(alpha = 0.7f),
                        topLeft = Offset(size.width * 0.08f, size.height * 0.72f),
                        size = Size(size.width * 0.5f, lineStroke),
                        cornerRadius = lineRadius
                    )
                }
            }
        }

        Text(
            text = "Sin archivo reciente",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(
            onClick = onScanNewClick,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.height(38.dp)
        ) {
            Text(
                text = "Escanear documentos nuevos",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
