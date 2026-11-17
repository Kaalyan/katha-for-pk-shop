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
import com.example.model.AppLanguage

private val LightKhataColorScheme =
  lightColorScheme(
    primary = KhataForestGreen,
    onPrimary = Color.White,
    primaryContainer = KhataMintSoft,
    onPrimaryContainer = KhataForestGreenDark,
    secondary = KhataSaffronGold,
    onSecondary = Color.White,
    secondaryContainer = KhataAmberSoft,
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = KhataEmeraldPaid,
    onTertiary = Color.White,
    background = KhataCreamBackground,
    onBackground = KhataInkPrimary,
    surface = KhataPaperSurface,
    onSurface = KhataInkPrimary,
    surfaceVariant = KhataSurfaceVariant,
    onSurfaceVariant = KhataInkSecondary,
    outline = KhataLedgerLine,
    error = KhataCrimsonDue,
    onError = Color.White,
    errorContainer = KhataCrimsonSoft,
    onErrorContainer = Color(0xFF7F1D1D),
  )

private val DarkKhataColorScheme =
  darkColorScheme(
    primary = KhataDarkMint,
    onPrimary = KhataForestGreenDark,
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = KhataMintSoft,
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = KhataAmberSoft,
    tertiary = Color(0xFF4ADE80),
    onTertiary = KhataForestGreenDark,
    background = KhataDarkBg,
    onBackground = Color(0xFFF3F4F0),
    surface = KhataDarkSurface,
    onSurface = Color(0xFFF3F4F0),
    surfaceVariant = KhataDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB5C2B8),
    outline = Color(0xFF334739),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = KhataCrimsonSoft,
  )

private val KhataShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
  )

@Composable
fun KiranaKhataTheme(
  language: AppLanguage = AppLanguage.EN,
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkKhataColorScheme else LightKhataColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = khataTypographyFor(language),
    shapes = KhataShapes,
    content = content,
  )
}
