package com.edm.downloadmanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = EdmBlue,
    secondary = EdmTeal,
    tertiary = EdmAmber,
    error = EdmRed,
    background = LightBackground,
    surface = LightSurface
)

private val DarkColors = darkColorScheme(
    primary = EdmBlue,
    secondary = EdmTeal,
    tertiary = EdmAmber,
    error = EdmRed,
    background = DarkBackground,
    surface = DarkSurface
)

enum class EdmThemeMode { LIGHT, DARK, SYSTEM }

@Composable
fun EDMTheme(
    themeMode: EdmThemeMode = EdmThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        EdmThemeMode.LIGHT -> false
        EdmThemeMode.DARK -> true
        EdmThemeMode.SYSTEM -> systemDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDark -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EdmTypography,
        content = content
    )
}
