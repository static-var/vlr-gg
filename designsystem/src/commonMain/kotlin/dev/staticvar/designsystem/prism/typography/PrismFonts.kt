/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:Suppress("MatchingDeclarationName")

package dev.staticvar.designsystem.prism.typography

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import vlr.designsystem.generated.resources.Res
import vlr.designsystem.generated.resources.chakra_petch_regular
import vlr.designsystem.generated.resources.space_grotesk_regular
import vlr.designsystem.generated.resources.vt323_regular
import org.jetbrains.compose.resources.Font as ResourceFont

@Immutable
internal data class PrismFontFamilies(
  val display: FontFamily,
  val numeric: FontFamily,
  val title: FontFamily,
  val body: FontFamily,
  val label: FontFamily,
  val labelAlt: FontFamily,
  val caption: FontFamily,
  val button: FontFamily,
  val pixel: FontFamily = display,
)

@Composable
internal fun rememberPrismFontFamilies(): PrismFontFamilies {
  val displayFont = FontFamily(ResourceFont(Res.font.chakra_petch_regular))
  val bodyFont = FontFamily(ResourceFont(Res.font.space_grotesk_regular))

  val pixelFont = FontFamily(ResourceFont(Res.font.vt323_regular))

  return remember(displayFont, bodyFont, pixelFont) {
    PrismFontFamilies(
      display = displayFont,
      numeric = displayFont,
      title = displayFont,
      body = bodyFont,
      label = bodyFont,
      labelAlt = displayFont,
      caption = bodyFont,
      button = bodyFont,
      pixel = pixelFont,
    )
  }
}
