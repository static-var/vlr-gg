package dev.unusedvariable.vlr.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.unusedvariable.vlr.R

val regular = Font(R.font.exo_light, FontWeight.Light)
val medium = Font(R.font.exo_regular, FontWeight.Normal)
val bold = Font(R.font.exo_semibold, FontWeight.SemiBold)

val appFontFamily = FontFamily(
    fonts = listOf(
        regular,
        medium,
        bold
    )
)

val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 30.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp
    ),
    displaySmall = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
)