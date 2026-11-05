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

val LocalIsLightTheme = staticCompositionLocalOf { true }

private val ReferenceLightColorScheme = lightColorScheme(
    primary = WhatsAppHeaderGreen,
    onPrimary = Color.White,
    primaryContainer = WhatsAppSoftGreenBg,
    onPrimaryContainer = WhatsAppDarkGreen,
    secondary = WhatsAppActionGreen,
    onSecondary = Color.White,
    secondaryContainer = WhatsAppOutgoingBubble,
    onSecondaryContainer = LightTextPrimary,
    tertiary = WhatsAppAccentGreen,
    background = LightBackgroundWhite,
    onBackground = LightTextPrimary,
    surface = LightCardSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceSecondary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightDividerGray,
    error = TactileCrimson,
    onError = Color.White
)

private val ReferenceDarkColorScheme = darkColorScheme(
    primary = WhatsAppActionGreen,
    onPrimary = Color.White,
    primaryContainer = WhatsAppDarkGreen,
    onPrimaryContainer = Color.White,
    secondary = WhatsAppAccentGreen,
    onSecondary = DeepMatteSlateStart,
    secondaryContainer = Color(0xFF005C4B),
    onSecondaryContainer = Color.White,
    tertiary = WhatsAppAccentGreen,
    background = DeepMatteSlateStart,
    onBackground = Color(0xFFE9EDEF),
    surface = MatteSlateSurface,
    onSurface = Color(0xFFE9EDEF),
    surfaceVariant = Color(0xFF202C33),
    onSurfaceVariant = Color(0xFF8696A0),
    outline = Color(0xFF2A3942),
    error = TactileCrimson,
    onError = Color.White
)

val NeoTactileShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun NeoTactileTheme(
    isLightTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isLightTheme) ReferenceLightColorScheme else ReferenceDarkColorScheme
    CompositionLocalProvider(LocalIsLightTheme provides isLightTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = NeoTactileShapes,
            content = content
        )
    }
}
