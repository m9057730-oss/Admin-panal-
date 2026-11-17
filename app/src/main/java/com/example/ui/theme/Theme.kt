package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ExecutiveDarkColorScheme = darkColorScheme(
    primary = SaffronPrimary,
    onPrimary = Obsidian950,
    primaryContainer = SaffronContainerDark,
    onPrimaryContainer = SaffronBright,
    secondary = EmeraldAccent,
    onSecondary = Obsidian950,
    secondaryContainer = EmeraldContainerDark,
    onSecondaryContainer = EmeraldBright,
    tertiary = IndigoAccent,
    onTertiary = Color.White,
    tertiaryContainer = IndigoContainerDark,
    onTertiaryContainer = Color(0xFFC7D2FE),
    background = Obsidian900,
    onBackground = Slate50,
    surface = Obsidian850,
    onSurface = Slate50,
    surfaceVariant = Obsidian800,
    onSurfaceVariant = Slate400,
    surfaceContainerLowest = Obsidian950,
    surfaceContainerLow = Obsidian900,
    surfaceContainer = Obsidian850,
    surfaceContainerHigh = Obsidian800,
    surfaceContainerHighest = Obsidian700,
    outline = Color(0xFF2D3B5E),
    outlineVariant = Color(0xFF1F2A46),
    error = RoseError,
    onError = Color.White,
    errorContainer = RoseContainerDark,
    onErrorContainer = Color(0xFFFCA5A5)
)

private val ExecutiveLightColorScheme = lightColorScheme(
    primary = SaffronDark,
    onPrimary = Color.White,
    primaryContainer = SaffronContainerLight,
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainerLight,
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = IndigoAccent,
    onTertiary = Color.White,
    tertiaryContainer = IndigoContainerLight,
    onTertiaryContainer = Color(0xFF312E81),
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Slate50,
    surfaceContainer = Slate100,
    surfaceContainerHigh = Slate200,
    surfaceContainerHighest = Color(0xFFCBD5E1),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Slate200,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = RoseContainerLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

val RestroShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RestroAdminTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ExecutiveDarkColorScheme else ExecutiveLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = RestroTypography,
        shapes = RestroShapes,
        content = content
    )
}
