package dev.staticvar.vlr.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import dev.staticvar.vlr.R

val fontProvider by lazy {
  GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    R.array.com_google_android_gms_fonts_certs
  )
}

val bigFontFamily =
  FontFamily(
    listOf(
      Font(GoogleFont("Inter"), fontProvider = fontProvider),
    )
  )
val smallFontFamily =
  FontFamily(
    listOf(
      Font(GoogleFont("Sora"), fontProvider = fontProvider),
    )
  )

val Typography =
  Typography(
    displayLarge =
      TextStyle(fontFamily = bigFontFamily, fontWeight = FontWeight.W600, fontSize = 57.sp),
    displayMedium =
      TextStyle(fontFamily = bigFontFamily, fontWeight = FontWeight.W600, fontSize = 45.sp),
    displaySmall =
      TextStyle(fontFamily = bigFontFamily, fontWeight = FontWeight.W600, fontSize = 36.sp),
    headlineLarge =
      TextStyle(fontFamily = bigFontFamily, fontWeight = FontWeight.W600, fontSize = 32.sp),
    headlineMedium =
      TextStyle(fontFamily = bigFontFamily, fontWeight = FontWeight.W600, fontSize = 28.sp),
    headlineSmall =
      TextStyle(fontFamily = bigFontFamily, fontWeight = FontWeight.W600, fontSize = 24.sp),
    titleLarge =
      TextStyle(fontFamily = bigFontFamily, fontWeight = FontWeight.W600, fontSize = 22.sp),
    titleMedium =
      TextStyle(fontFamily = bigFontFamily, fontWeight = FontWeight.W600, fontSize = 16.sp),
    titleSmall =
      TextStyle(fontFamily = bigFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodyLarge =
      TextStyle(fontFamily = smallFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium =
      TextStyle(fontFamily = smallFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall =
      TextStyle(fontFamily = smallFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge =
      TextStyle(fontFamily = smallFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelMedium =
      TextStyle(fontFamily = smallFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelSmall =
      TextStyle(fontFamily = smallFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp),
  )
