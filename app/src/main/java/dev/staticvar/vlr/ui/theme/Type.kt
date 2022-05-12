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

val appFontFamily =
  FontFamily(
    listOf(
      Font(GoogleFont("IBM Plex Sans"), fontProvider = fontProvider),
    )
  )

val Typography =
  Typography(
    titleLarge =
      TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 30.sp,
      ),
    titleMedium =
      TextStyle(fontFamily = appFontFamily, fontWeight = FontWeight.W600, fontSize = 24.sp),
    titleSmall =
      TextStyle(fontFamily = appFontFamily, fontWeight = FontWeight.Normal, fontSize = 20.sp),
    displaySmall =
      TextStyle(fontFamily = appFontFamily, fontWeight = FontWeight.W600, fontSize = 16.sp),
    bodyMedium =
      TextStyle(fontFamily = appFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
  )
