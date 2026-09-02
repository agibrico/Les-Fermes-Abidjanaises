package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FarmGreenPrimary,
    secondary = FarmGoldSecondary,
    tertiary = FarmGreenLight,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = LightSurface,
    onSecondary = DarkBackground,
    onBackground = LightBackground,
    onSurface = LightBackground
)

private val LightColorScheme = lightColorScheme(
    primary = FarmGreenPrimary,
    secondary = FarmGoldSecondary,
    tertiary = FarmGreenDark,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = LightSurface,
    onSecondary = DarkBackground,
    onBackground = DarkBackground,
    onSurface = DarkBackground
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to preserve our beautifully customized colors
    content: @Composable () -> Unit,
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
