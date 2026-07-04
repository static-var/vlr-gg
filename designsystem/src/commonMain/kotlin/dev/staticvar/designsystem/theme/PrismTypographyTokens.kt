/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.staticvar.designsystem.prism.typography.PrismFontFamilies
import dev.staticvar.designsystem.prism.typography.PrismTypography

internal object PrismTypographyTokens {
  @Suppress("LongMethod")
  fun createTokens(fonts: PrismFontFamilies): PrismTypography = PrismTypography(
    display =
    TextStyle(
      fontFamily = fonts.display,
      fontWeight = FontWeight.Normal,
      fontSize = 36.sp,
      letterSpacing = (-0.25).sp,
    ),
    headline =
    TextStyle(
      fontFamily = fonts.display,
      fontWeight = FontWeight.Normal,
      fontSize = 28.sp,
      letterSpacing = 0.sp,
    ),
    sectionTitle =
    TextStyle(
      fontFamily = fonts.title,
      fontWeight = FontWeight.Normal,
      fontSize = 24.sp,
      letterSpacing = 0.sp,
    ),
    cardTitle =
    TextStyle(
      fontFamily = fonts.title,
      fontWeight = FontWeight.Normal,
      fontSize = 20.sp,
      letterSpacing = 0.sp,
    ),
    bodyLarge =
    TextStyle(
      fontFamily = fonts.body,
      fontWeight = FontWeight.Normal,
      fontSize = 16.sp,
      letterSpacing = 0.5.sp,
    ),
    bodySmall =
    TextStyle(
      fontFamily = fonts.body,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      letterSpacing = 0.25.sp,
    ),
    label =
    TextStyle(
      fontFamily = fonts.label,
      fontWeight = FontWeight.Normal,
      fontSize = 12.sp,
      letterSpacing = 0.5.sp,
    ),
    labelAlt =
    TextStyle(
      fontFamily = fonts.labelAlt,
      fontWeight = FontWeight.Normal,
      fontSize = 12.sp,
      letterSpacing = 0.5.sp,
    ),
    caption =
    TextStyle(
      fontFamily = fonts.caption,
      fontWeight = FontWeight.Normal,
      fontSize = 11.sp,
      letterSpacing = 0.5.sp,
    ),
    button =
    TextStyle(
      fontFamily = fonts.button,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      letterSpacing = 0.1.sp,
    ),
    numericPrimary =
    TextStyle(
      fontFamily = fonts.numeric,
      fontWeight = FontWeight.Normal,
      fontSize = 28.sp,
      letterSpacing = (-0.25).sp,
    ),
    numericSecondary =
    TextStyle(
      fontFamily = fonts.numeric,
      fontWeight = FontWeight.Normal,
      fontSize = 16.sp,
      letterSpacing = 0.sp,
    ),
    overline =
    TextStyle(
      fontFamily = fonts.caption,
      fontWeight = FontWeight.Normal,
      fontSize = 11.sp,
      letterSpacing = 0.8.sp,
    ),
  )

  fun createMaterial(tokens: PrismTypography): Typography = Typography(
    displayLarge = tokens.display,
    displayMedium = tokens.headline,
    displaySmall = tokens.sectionTitle.resize(30, 36, 0f),
    headlineLarge = tokens.sectionTitle,
    headlineMedium = tokens.cardTitle.resize(24, 30, 0f),
    headlineSmall = tokens.cardTitle,
    titleLarge = tokens.cardTitle,
    titleMedium = tokens.cardTitle.resize(16, 20, 0.15f),
    titleSmall = tokens.cardTitle.resize(14, 18, 0.1f),
    bodyLarge = tokens.bodyLarge,
    bodyMedium = tokens.bodySmall,
    bodySmall = tokens.bodySmall.resize(12, 15, 0.4f),
    labelLarge = tokens.button,
    labelMedium = tokens.label,
    labelSmall = tokens.caption,
  )

  private fun TextStyle.resize(fontSize: Int, lineHeight: Int, letterSpacing: Float): TextStyle = copy(
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
  )
}
