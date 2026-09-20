package com.abdapps.scandocs

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.abdapps.scandocs.data.database.AppDatabase
import com.abdapps.scandocs.data.repository.DocumentRepository
import com.abdapps.scandocs.service.FileService
import com.abdapps.scandocs.ui.components.DocumentScannerScreen
import com.abdapps.scandocs.ui.components.HistoryScreen
import com.abdapps.scandocs.ui.theme.ScanDocsTheme

/**
 * Actividad principal de ScanDocs.
 *
 * Integra ML Kit Document Scanner con:
 * - Arquitectura MVVM con Room y Repository
 * - Navegación con Material 3 NavigationBar (Inicio y Documentos)
 * - Tema Material Design 3 refinado (claro y oscuro profesional)
 * - Edge-to-edge para una experiencia visual inmersiva
 */
class MainActivity : androidx.fragment.app.FragmentActivity() {

    private lateinit var scannerViewModel: ScannerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar dependencias
        val database = AppDatabase.getDatabase(this)
        val repository = DocumentRepository(database.documentDao())
        val fileService = FileService(this)

        // Inicializar ViewModel con Factory
        val factory = ScannerViewModel.Factory(repository, fileService)
        scannerViewModel = viewModels<ScannerViewModel> { factory }.value

        setContent {
            ScanDocsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(scannerViewModel = scannerViewModel)
                }
            }
        }

        // Inicializar el escáner de documentos
        initializeScanner()
    }

    private fun initializeScanner() {
        try {
            scannerViewModel.initializeScanner(this, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Pantalla contenedora principal con barra de navegación inferior (Material 3 NavigationBar).
 */
@Composable
fun MainScreen(scannerViewModel: ScannerViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    Scaffold(
        bottomBar = {
            ScanDocsBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // ─── Pantalla de Inicio ────────────────────────────────────
            composable("home") {
                DocumentScannerScreen(
                    viewModel = scannerViewModel,
                    onNavigateToHistory = {
                        navController.navigate("history") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // ─── Pantalla de Historial Completo (Documentos) ──────────
            composable("history") {
                HistoryScreen(
                    viewModel = scannerViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

/**
 * Barra de navegación inferior con Material 3 NavigationBar.
 * Tonal elevation = 0.dp para evitar tintes no deseados, integrándose limpiamente
 * con el fondo carbón de la app y un indicador de selección sutil.
 */
@Composable
fun ScanDocsBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        // 1. Pestaña Inicio
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { onNavigate("home") },
            icon = {
                Icon(
                    imageVector = if (currentRoute == "home") Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Inicio",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    text = "Inicio",
                    style = MaterialTheme.typography.labelMedium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // 2. Pestaña Documentos
        NavigationBarItem(
            selected = currentRoute == "history",
            onClick = { onNavigate("history") },
            icon = {
                Icon(
                    imageVector = if (currentRoute == "history") Icons.Filled.Folder else Icons.Outlined.Folder,
                    contentDescription = "Documentos",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    text = "Documentos",
                    style = MaterialTheme.typography.labelMedium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
