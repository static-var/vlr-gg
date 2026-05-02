/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:Suppress("LongParameterList")

package dev.staticvar.designsystem.component.snackbar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.color.contentColorFor

/**
 * Flat, bordered snackbar with optional leading icon and action slot.
 */
@Composable
public fun PrismSnackbar(
  message: String,
  modifier: Modifier = Modifier,
  leadingIcon: (@Composable () -> Unit)? = null,
  action: (@Composable RowScope.() -> Unit)? = null,
  shape: Shape = Prism.shapes.medium,
  color: Color = Prism.color.backgroundElevated,
  contentColor: Color = contentColorFor(color),
) {
  PrismSurface(
    modifier = modifier.fillMaxWidth(),
    color = color,
    contentColor = contentColor,
    shape = shape,
    border = BorderStroke(Prism.dimens.strokeDefault, Prism.color.stroke),
  ) {
    Row(
      modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = Prism.dimens.spacingM, vertical = Prism.dimens.spacingS),
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (leadingIcon != null) {
        Box(
          modifier = Modifier.size(Prism.dimens.iconM),
          contentAlignment = Alignment.Center,
        ) {
          leadingIcon()
        }
      }

      ProvideTextStyle(value = Prism.typography.bodySmall) {
        Text(text = message, modifier = Modifier.weight(1f))
      }

      if (action != null) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
          verticalAlignment = Alignment.CenterVertically,
          content = action,
        )
      }
    }
  }
}
