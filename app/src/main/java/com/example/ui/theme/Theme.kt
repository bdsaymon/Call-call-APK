package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalIsLightTheme = staticCompositionLocalOf { false }

private val EmeraldDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color.White,
    secondary = EmeraldBright,
    onSecondary = DeepMatteSlateStart,
    secondaryContainer = Color(0xFF133629),
    onSecondaryContainer = EmeraldBright,
    tertiary = TactileEmerald,
    background = DeepMatteSlateStart,
    onBackground = TextPrimary,
    surface = MatteSlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF202C33),
    onSurfaceVariant = TextSecondary,
    error = TactileCrimson,
    onError = Color.White
)

private val EmeraldLightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    primaryContainer = EmeraldSoft,
    onPrimaryContainer = EmeraldDark,
    secondary = EmeraldPrimary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9FDD3),
    onSecondaryContainer = LightTextPrimary,
    tertiary = EmeraldBright,
    background = LightBackgroundWhite,
    onBackground = LightTextPrimary,
    surface = LightCardSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceSecondary,
    onSurfaceVariant = LightTextSecondary,
    error = TactileCrimson,
    onError = Color.White
)

val NeoTactileShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun NeoTactileTheme(
    isLightTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isLightTheme) EmeraldLightColorScheme else EmeraldDarkColorScheme
    CompositionLocalProvider(LocalIsLightTheme provides isLightTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = NeoTactileShapes,
            content = content
        )
    }
}
