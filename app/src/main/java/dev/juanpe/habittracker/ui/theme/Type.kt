package dev.juanpe.habittracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.juanpe.habittracker.R

/**
 * Nunito (licencia OFL) es una fuente "variable": un único archivo contiene
 * todos los grosores, y aquí se declara cada peso que usa la app.
 */
private fun nunito(weight: FontWeight) = Font(
    resId = R.font.nunito,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Nunito = FontFamily(
    nunito(FontWeight.Normal),
    nunito(FontWeight.Medium),
    nunito(FontWeight.SemiBold),
    nunito(FontWeight.Bold),
    nunito(FontWeight.ExtraBold),
    nunito(FontWeight.Black),
)

private fun style(size: Int, lineHeight: Int, weight: FontWeight, spacing: Double = 0.0) = TextStyle(
    fontFamily = Nunito,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = spacing.sp,
)

/** Títulos gruesos y redondeados; cuerpo algo más pesado de lo normal para que se lea bien. */
val AppTypography = Typography(
    displayLarge = style(52, 58, FontWeight.Black, -0.5),
    displayMedium = style(42, 48, FontWeight.Black, -0.5),
    displaySmall = style(34, 40, FontWeight.Black),
    headlineLarge = style(30, 36, FontWeight.ExtraBold),
    headlineMedium = style(26, 32, FontWeight.ExtraBold),
    headlineSmall = style(22, 28, FontWeight.ExtraBold),
    titleLarge = style(20, 26, FontWeight.ExtraBold),
    titleMedium = style(17, 23, FontWeight.Bold),
    titleSmall = style(15, 20, FontWeight.Bold),
    bodyLarge = style(16, 23, FontWeight.Medium),
    bodyMedium = style(14, 20, FontWeight.Medium),
    bodySmall = style(12, 16, FontWeight.Medium),
    labelLarge = style(15, 20, FontWeight.Bold),
    labelMedium = style(13, 16, FontWeight.Bold),
    labelSmall = style(11, 14, FontWeight.Bold, 0.3),
)
