package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppLanguage

val PlusJakartaSansFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val PlayfairDisplayFamily = FontFamily(Font(R.font.playfair_display, FontWeight.Normal))
val NotoSansTeluguFamily = FontFamily(Font(R.font.noto_sans_telugu, FontWeight.Normal))

fun khataTypographyFor(language: AppLanguage): Typography {
  val bodyFont =
    if (language == AppLanguage.TE) NotoSansTeluguFamily else PlusJakartaSansFamily
  val displayFont =
    if (language == AppLanguage.TE) NotoSansTeluguFamily else PlayfairDisplayFamily

  return Typography(
    displaySmall =
      TextStyle(
        fontFamily = displayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = displayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = bodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = bodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = bodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
      ),
  )
}
