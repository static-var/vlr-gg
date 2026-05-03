/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.ShieldAltSolid
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismIconPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      PrismIcon(
        imageVector = LineAwesomeIcons.ShieldAltSolid,
        contentDescription = "Bordered icon",
        size = PrismIconSize.Small,
        tint = PrismIconTint.Primary,
      )
      PrismIcon(
        imageVector = LineAwesomeIcons.ShieldAltSolid,
        contentDescription = "Medium icon",
        size = PrismIconSize.Medium,
        tint = PrismIconTint.Alt,
      )
      PrismIcon(
        imageVector = LineAwesomeIcons.ShieldAltSolid,
        contentDescription = "Large icon",
        size = PrismIconSize.Large,
        style = PrismIconStyle.Muted,
        tint = PrismIconTint.Primary,
      )
      PrismIcon(
        imageVector = LineAwesomeIcons.ShieldAltSolid,
        contentDescription = "Extra large icon",
        size = PrismIconSize.ExtraLarge,
        style = PrismIconStyle.Plain,
        tint = PrismIconTint.Alt,
      )
      PrismIcon(
        imageVector = LineAwesomeIcons.ShieldAltSolid,
        contentDescription = "Hero icon",
        size = PrismIconSize.Hero,
        tint = PrismIconTint.Primary,
      )
    }
  }
}
