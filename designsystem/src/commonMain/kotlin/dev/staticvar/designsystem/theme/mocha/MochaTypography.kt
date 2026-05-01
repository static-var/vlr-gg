package dev.staticvar.designsystem.theme.mocha

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.staticvar.designsystem.prism.color.PrismColorPalette
import dev.staticvar.designsystem.prism.typography.PrismFontFamilies
import dev.staticvar.designsystem.prism.typography.PrismTypography

/**
 * Typography definitions for Mocha theme with harmonized color usage.
 *
 * Larger hierarchy levels use high-contrast [PrismColorPalette.titleColor]; body
 * and supporting styles transition through the medium and low contrast tokens.
 */
internal object MochaTypography {
  fun createTokens(
    palette: PrismColorPalette,
    fonts: PrismFontFamilies,
  ): PrismTypography {
    return PrismTypography(
      display =
        TextStyle(
          fontFamily = fonts.display,
          fontWeight = FontWeight.Bold,
          fontSize = 57.sp,
          lineHeight = 64.sp,
          letterSpacing = (-0.25).sp,
        ),
      headline =
        TextStyle(
          fontFamily = fonts.display,
          fontWeight = FontWeight.Bold,
          fontSize = 45.sp,
          lineHeight = 52.sp,
          letterSpacing = 0.sp,
        ),
      sectionTitle =
        TextStyle(
          fontFamily = fonts.title,
          fontWeight = FontWeight.SemiBold,
          fontSize = 32.sp,
          lineHeight = 40.sp,
          letterSpacing = 0.sp,
        ),
      cardTitle =
        TextStyle(
          fontFamily = fonts.title,
          fontWeight = FontWeight.Medium,
          fontSize = 22.sp,
          lineHeight = 28.sp,
          letterSpacing = 0.sp,
        ),
      bodyLarge =
        TextStyle(
          fontFamily = fonts.body,
          fontWeight = FontWeight.Normal,
          fontSize = 16.sp,
          lineHeight = 24.sp,
          letterSpacing = 0.5.sp,
        ),
      bodySmall =
        TextStyle(
          fontFamily = fonts.body,
          fontWeight = FontWeight.Normal,
          fontSize = 14.sp,
          lineHeight = 20.sp,
          letterSpacing = 0.25.sp,
        ),
      label =
        TextStyle(
          fontFamily = fonts.label,
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp,
          lineHeight = 16.sp,
          letterSpacing = 0.5.sp,
        ),
      caption =
        TextStyle(
          fontFamily = fonts.caption,
          fontWeight = FontWeight.Medium,
          fontSize = 11.sp,
          lineHeight = 16.sp,
          letterSpacing = 0.5.sp,
          fontStyle = FontStyle.Italic,
        ),
      button =
        TextStyle(
          fontFamily = fonts.button,
          fontWeight = FontWeight.Medium,
          fontSize = 14.sp,
          lineHeight = 20.sp,
          letterSpacing = 0.1.sp,
        ),
      numericPrimary =
        TextStyle(
          fontFamily = fonts.numeric,
          fontWeight = FontWeight.SemiBold,
          fontSize = 40.sp,
          lineHeight = 44.sp,
          letterSpacing = (-0.25).sp,
        ),
      numericSecondary =
        TextStyle(
          fontFamily = fonts.numeric,
          fontWeight = FontWeight.Medium,
          fontSize = 18.sp,
          lineHeight = 24.sp,
          letterSpacing = 0.sp,
        ),
      overline =
        TextStyle(
          fontFamily = fonts.caption,
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp,
          lineHeight = 16.sp,
          letterSpacing = 0.8.sp,
        ),
    )
  }

  fun createMaterial(tokens: PrismTypography): Typography =
    Typography(
      displayLarge = tokens.display,
      displayMedium = tokens.headline,
      displaySmall = tokens.sectionTitle.resize(36, 44, 0f),
      headlineLarge = tokens.sectionTitle.resize(32, 40, 0f),
      headlineMedium = tokens.sectionTitle.resize(28, 36, 0f),
      headlineSmall = tokens.cardTitle.resize(24, 32, 0f),
      titleLarge = tokens.cardTitle,
      titleMedium = tokens.cardTitle.resize(16, 24, 0.15f),
      titleSmall = tokens.cardTitle.resize(14, 20, 0.1f),
      bodyLarge = tokens.bodyLarge,
      bodyMedium = tokens.bodySmall,
      bodySmall = tokens.bodySmall.resize(12, 16, 0.4f),
      labelLarge = tokens.button,
      labelMedium = tokens.label,
      labelSmall = tokens.caption,
    )

  private fun TextStyle.resize(
    fontSize: Int,
    lineHeight: Int,
    letterSpacing: Float,
  ): TextStyle =
    copy(
      fontSize = fontSize.sp,
      lineHeight = lineHeight.sp,
      letterSpacing = letterSpacing.sp,
    )
}
