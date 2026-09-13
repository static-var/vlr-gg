/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.button.PrismIconButton
import dev.staticvar.designsystem.component.button.PrismIconButtonSize
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.prism.Prism

@Composable
public fun WhatsNewBanner(
  onOpen: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  PrismCard(modifier = modifier.fillMaxWidth(), style = PrismCardStyle.Filled) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .heightIn(min = Prism.dimens.touchTargetMin)
          .clickable(
            role = Role.Button,
            onClickLabel = BundledRelease.bannerAction,
            onClick = onOpen,
          ),
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = BundledRelease.bannerTitle,
          style = Prism.typography.cardTitle,
          color = Prism.color.titleColor,
        )
        Text(
          text = BundledRelease.bannerAction,
          style = Prism.typography.label,
          color = Prism.color.accent,
        )
      }
      PrismIconButton(
        icon = WhatsNewCloseIcon,
        contentDescription = "Dismiss what's new",
        onClick = onDismiss,
        size = PrismIconButtonSize.Small,
      )
    }
  }
}

private val WhatsNewCloseIcon: ImageVector = ImageVector.Builder(
  name = "WhatsNewClose",
  defaultWidth = 24.dp,
  defaultHeight = 24.dp,
  viewportWidth = 24f,
  viewportHeight = 24f,
).apply {
  path(
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 2f,
    strokeLineCap = StrokeCap.Round,
  ) {
    moveTo(5f, 5f)
    lineTo(19f, 19f)
    moveTo(19f, 5f)
    lineTo(5f, 19f)
  }
}.build()
