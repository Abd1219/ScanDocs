package com.abdapps.scandocs.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ScanPrimaryDark,
    secondary = ScanSecondaryDark,
    tertiary = Teal80,
    background = ScanDarkBackground,
    surface = ScanDarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    error = ScanErrorDark,
    onError = Color.Black,
    errorContainer = ScanErrorDark.copy(alpha = 0.2f),
    onErrorContainer = ScanErrorDark,
    primaryContainer = ScanPrimaryDark.copy(alpha = 0.2f),
    onPrimaryContainer = ScanPrimaryDark,
    surfaceVariant = ScanDarkSurface.copy(alpha = 0.8f),
    onSurfaceVariant = Color.White.copy(alpha = 0.7f)
)

private val LightColorScheme = lightColorScheme(
    primary = ScanPrimary,
    secondary = ScanSecondary,
    tertiary = Teal40,
    background = ScanBackground,
    surface = ScanSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    error = ScanError,
    onError = Color.White,
    errorContainer = ScanError.copy(alpha = 0.1f),
    onErrorContainer = ScanError,
    primaryContainer = ScanPrimary.copy(alpha = 0.1f),
    onPrimaryContainer = ScanPrimary,
    surfaceVariant = ScanSurface.copy(alpha = 0.8f),
    onSurfaceVariant = Color.Black.copy(alpha = 0.6f)
)

@Composable
fun ScanDocsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}