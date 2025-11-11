package com.university.vtexter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Theme Colors
private val LightPrimary = Color(0xFF6200EE)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFE8DEF8)
private val LightOnPrimaryContainer = Color(0xFF21005D)

private val LightSecondary = Color(0xFF03DAC6)
private val LightOnSecondary = Color(0xFF000000)
private val LightSecondaryContainer = Color(0xFFCCF8F3)
private val LightOnSecondaryContainer = Color(0xFF002019)

// Dark Theme Colors
private val DarkPrimary = Color(0xFFBB86FC)
private val DarkOnPrimary = Color(0xFF3700B3)
private val DarkPrimaryContainer = Color(0xFF4F378B)
private val DarkOnPrimaryContainer = Color(0xFFE8DEF8)

private val DarkSecondary = Color(0xFF03DAC6)
private val DarkOnSecondary = Color(0xFF00332D)
private val DarkSecondaryContainer = Color(0xFF004D43)
private val DarkOnSecondaryContainer = Color(0xFFCCF8F3)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
)

@Composable
fun VTexterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}