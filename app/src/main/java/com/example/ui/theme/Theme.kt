package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DisciplinaDarkColorScheme = darkColorScheme(
    primary = CarbonText,
    onPrimary = CarbonBlack,
    primaryContainer = CarbonCard,
    onPrimaryContainer = CarbonText,
    secondary = EmeraldMogger,
    onSecondary = CarbonBlack,
    secondaryContainer = EmeraldMoggerDim,
    onSecondaryContainer = EmeraldMogger,
    tertiary = DriftCrimson,
    onTertiary = CarbonText,
    tertiaryContainer = RoseMoggedDim,
    onTertiaryContainer = RoseMogged,
    background = CarbonBlack,
    onBackground = CarbonText,
    surface = CarbonSurface,
    onSurface = CarbonText,
    surfaceVariant = CarbonCard,
    onSurfaceVariant = CarbonSubtext,
    outline = CarbonBorder,
    outlineVariant = CarbonMuted,
    error = RoseMogged,
    onError = CarbonText
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun DisciplinaDriftTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DisciplinaDarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
