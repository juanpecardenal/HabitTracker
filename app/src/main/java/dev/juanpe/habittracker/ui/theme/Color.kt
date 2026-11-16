package dev.juanpe.habittracker.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Paleta de la app: fondo crema cálido en claro y tinta azulada en oscuro,
 * con violeta como color de marca. Los colores vivos se reservan para las
 * áreas y para las recompensas (llama, oro), que es donde debe ir la vista.
 */

// --- Tema claro ---------------------------------------------------------------
val LightColors = lightColorScheme(
    primary = Color(0xFF5B47E0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E2FF),
    onPrimaryContainer = Color(0xFF1C1164),
    secondary = Color(0xFF6B5E8F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECE6FA),
    onSecondaryContainer = Color(0xFF251B45),
    tertiary = Color(0xFFB26A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE6BF),
    onTertiaryContainer = Color(0xFF3A2200),
    background = Color(0xFFFBF7F1),
    onBackground = Color(0xFF1F1B2E),
    surface = Color(0xFFFBF7F1),
    onSurface = Color(0xFF1F1B2E),
    surfaceVariant = Color(0xFFEFE9E0),
    onSurfaceVariant = Color(0xFF635D72),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF5F0E8),
    surfaceContainerHigh = Color(0xFFEFE9E0),
    surfaceContainerHighest = Color(0xFFE9E2D8),
    outline = Color(0xFFB9B0A6),
    outlineVariant = Color(0xFFE4DCD0),
    error = Color(0xFFC4384B),
    onError = Color.White,
    errorContainer = Color(0xFFFFDADD),
    onErrorContainer = Color(0xFF410009),
    scrim = Color(0xFF0D0B16),
)

// --- Tema oscuro ----------------------------------------------------------------
val DarkColors = darkColorScheme(
    primary = Color(0xFFB3A6FF),
    onPrimary = Color(0xFF1E1270),
    primaryContainer = Color(0xFF3B2FA6),
    onPrimaryContainer = Color(0xFFE7E2FF),
    secondary = Color(0xFFCDC2EE),
    onSecondary = Color(0xFF33295A),
    secondaryContainer = Color(0xFF352D52),
    onSecondaryContainer = Color(0xFFE9E1FF),
    tertiary = Color(0xFFFFC46B),
    onTertiary = Color(0xFF432A00),
    tertiaryContainer = Color(0xFF5E3D00),
    onTertiaryContainer = Color(0xFFFFE6BF),
    background = Color(0xFF12111B),
    onBackground = Color(0xFFECE9F5),
    surface = Color(0xFF12111B),
    onSurface = Color(0xFFECE9F5),
    surfaceVariant = Color(0xFF2A2839),
    onSurfaceVariant = Color(0xFFABA6BD),
    surfaceContainerLowest = Color(0xFF0D0C14),
    surfaceContainerLow = Color(0xFF1B1A28),
    surfaceContainer = Color(0xFF201F2E),
    surfaceContainerHigh = Color(0xFF2A2839),
    surfaceContainerHighest = Color(0xFF343245),
    outline = Color(0xFF5A566E),
    outlineVariant = Color(0xFF33313F),
    error = Color(0xFFFFB3B9),
    onError = Color(0xFF680016),
    errorContainer = Color(0xFF8E1A2D),
    onErrorContainer = Color(0xFFFFDADD),
    scrim = Color(0xFF000000),
)

/**
 * Colores propios de la app que Material no contempla: uno por área y los de
 * las recompensas. Se reparten con un CompositionLocal igual que MaterialTheme.
 */
@Immutable
data class AppColors(
    val diet: Color,
    val training: Color,
    val study: Color,
    val relations: Color,
    val gold: Color,
    val flameOuter: Color,
    val flameMiddle: Color,
    val flameCore: Color,
    val freeze: Color,
    val bronze: Color,
    val silver: Color,
)

val LightAppColors = AppColors(
    diet = Color(0xFF1FA968),
    training = Color(0xFFEF5B3A),
    study = Color(0xFF2F80ED),
    relations = Color(0xFFDB4A93),
    gold = Color(0xFFF0A720),
    flameOuter = Color(0xFFFF5A2B),
    flameMiddle = Color(0xFFFF9A1F),
    flameCore = Color(0xFFFFE27A),
    freeze = Color(0xFF29A9D6),
    bronze = Color(0xFFC57B48),
    silver = Color(0xFF8E9AAF),
)

val DarkAppColors = AppColors(
    diet = Color(0xFF3FD08A),
    training = Color(0xFFFF7A5C),
    study = Color(0xFF5AA2FF),
    relations = Color(0xFFF46BAE),
    gold = Color(0xFFFFC14D),
    flameOuter = Color(0xFFFF6A3D),
    flameMiddle = Color(0xFFFFA733),
    flameCore = Color(0xFFFFEB99),
    freeze = Color(0xFF55C7EE),
    bronze = Color(0xFFDB9563),
    silver = Color(0xFFB4BFD1),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
