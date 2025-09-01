package com.abdapps.scandocs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

/**
 * Clase utilitaria para manejar permisos de la aplicación
 * 
 * Esta clase proporciona métodos para verificar y solicitar permisos
 * necesarios para el funcionamiento del escáner de documentos:
 * - Permiso de cámara
 * - Permisos de almacenamiento (diferentes según la versión de Android)
 */
class PermissionHelper(private val context: Context) {
    
    /**
     * Verifica si se tienen todos los permisos necesarios
     * @return true si todos los permisos están concedidos
     */
    fun hasAllPermissions(): Boolean {
        return hasCameraPermission() && hasStoragePermission()
    }
    
    /**
     * Verifica el permiso de cámara
     * @return true si el permiso está concedido
     */
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Verifica los permisos de almacenamiento según la versión de Android
     * @return true si los permisos están concedidos
     */
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12 y anteriores
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Obtiene la lista de permisos que necesitan ser solicitados
     * @return Lista de permisos que faltan
     */
    fun getMissingPermissions(): List<String> {
        val missingPermissions = mutableListOf<String>()
        
        if (!hasCameraPermission()) {
            missingPermissions.add(Manifest.permission.CAMERA)
        }
        
        if (!hasStoragePermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                missingPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                missingPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        return missingPermissions
    }
}

/**
 * Composable que maneja la solicitud de permisos
 * 
 * Este composable proporciona una forma fácil de solicitar permisos
 * desde cualquier pantalla de Compose. Retorna el estado de los permisos
 * y proporciona un launcher para solicitar permisos faltantes.
 * 
 * @param onPermissionsGranted Callback que se ejecuta cuando todos los permisos están concedidos
 * @return Pair<Boolean, () -> Unit> donde el primer elemento indica si los permisos están concedidos
 *         y el segundo es una función para solicitar permisos faltantes
 */
@Composable
fun rememberPermissionState(
    onPermissionsGranted: () -> Unit = {}
): Pair<Boolean, () -> Unit> {
    var hasPermissions by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Verificar si todos los permisos fueron concedidos
        val allGranted = permissions.values.all { it }
        hasPermissions = allGranted
        
        if (allGranted) {
            onPermissionsGranted()
        }
    }
    
    val requestPermissions = {
        // Lista de permisos que necesitamos solicitar
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        
        permissionLauncher.launch(permissions)
    }
    
    return Pair(hasPermissions, requestPermissions)
}
