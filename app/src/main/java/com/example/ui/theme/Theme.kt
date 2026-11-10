package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PayWiseDarkColorScheme = darkColorScheme(
    primary = PngBrightEmerald,
    onPrimary = Color(0xFF042111),
    primaryContainer = PngForestGreen,
    onPrimaryContainer = PngMintLight,
    secondary = PngGold,
    onSecondary = Color(0xFF1F1600),
    secondaryContainer = Color(0xFF3E3008),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF042619),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = ExpenseRedDark,
    onError = Color(0xFF3B0606),
    outline = Color(0xFF2B533D)
)

private val PayWiseLightColorScheme = lightColorScheme(
    primary = PngForestGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = PngDeepGreen,
    secondary = Color(0xFFB48608),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF453003),
    tertiary = PngEmerald,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = ExpenseRed,
    onError = Color.White,
    outline = Color(0xFFB5CEC0)
)

val PayWiseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PayWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PayWiseDarkColorScheme else PayWiseLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PayWiseShapes,
        content = content
    )
}
