/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.typography

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant
import vlr.designsystem.generated.resources.Res
import vlr.designsystem.generated.resources.chakra_petch_regular
import vlr.designsystem.generated.resources.space_grotesk_regular
import org.jetbrains.compose.resources.Font as ResourceFont

@PrismPreview
@Composable
internal fun PrismFontDiagnosticPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  val body = FontFamily(ResourceFont(Res.font.space_grotesk_regular))
  val display = FontFamily(ResourceFont(Res.font.chakra_petch_regular))

  PrismTheme(variant = variant) {
    Column(
      modifier =
      Modifier
        .background(Prism.color.background)
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = "Space Grotesk Regular",
        color = Prism.color.bodyColor,
        style = Prism.typography.caption,
      )
      Text(
        modifier = Modifier.background(Color(0x22FF00FF)),
        text = "iiii WWWW",
        color = Prism.color.titleColor,
        style = TextStyle(
          fontFamily = body,
          fontSize = 44.sp,
        ),
      )
      Text(
        text = "Chakra Petch Regular",
        color = Prism.color.bodyColor,
        style = Prism.typography.caption,
      )
      Text(
        modifier = Modifier.background(Color(0x2200FFFF)),
        text = "iiii WWWW",
        color = Prism.color.titleColor,
        style = TextStyle(
          fontFamily = display,
          fontSize = 44.sp,
        ),
      )
    }
  }
}
