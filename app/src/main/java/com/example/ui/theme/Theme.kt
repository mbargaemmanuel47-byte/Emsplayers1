package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val AuraDarkColorScheme = darkColorScheme(
    primary = GoldBright,
    onPrimary = Obsidian,
    primaryContainer = DeepBlue,
    onPrimaryContainer = GoldLight,
    secondary = GoldMetallic,
    onSecondary = Obsidian,
    secondaryContainer = MidnightSurface,
    onSecondaryContainer = TextSecondary,
    tertiary = RoyalBlueAccent,
    onTertiary = TextPrimary,
    background = Obsidian,
    onBackground = TextPrimary,
    surface = Midnight,
    onSurface = TextPrimary,
    surfaceVariant = CardDarkSurface,
    onSurfaceVariant = TextMuted,
    outline = BorderGold,
    outlineVariant = BorderSubtle,
    error = RoseError,
    onError = Obsidian
)

val AuraShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AuraAITheme(
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        AuraDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AuraTypography,
        shapes = AuraShapes,
        content = content
    )
}
