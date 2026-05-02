/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.appbar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Brutalist screen title/app bar block with optional navigation and action slots.
 */
@Composable
public fun PrismScreenTitleBar(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  preLabel: String? = null,
  navigationSlot: (@Composable () -> Unit)? = null,
  actions: (@Composable RowScope.() -> Unit)? = null,
) {
  PrismSurface(
    modifier = modifier.fillMaxWidth(),
    color = Prism.color.background,
    shape = Prism.shapes.medium,
    border = BorderStroke(Prism.dimens.strokeDefault, Prism.color.stroke),
  ) {
    Column(
      modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = Prism.dimens.spacingM, vertical = Prism.dimens.spacingS),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
    ) {
      if (preLabel != null) {
        PrismScreenTitlePreLabel(text = preLabel)
      }

      PrismScreenTitleRow(
        title = title,
        navigationSlot = navigationSlot,
        actions = actions,
      )

      if (subtitle != null) {
        PrismScreenTitleSubtitle(text = subtitle)
      }
    }
  }
}

@Composable
private fun PrismScreenTitlePreLabel(text: String) {
  Text(
    text = text.uppercase(),
    style = Prism.typography.caption,
    color = Prism.color.labelColor,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
}

@Composable
private fun PrismScreenTitleRow(
  title: String,
  navigationSlot: (@Composable () -> Unit)?,
  actions: (@Composable RowScope.() -> Unit)?,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    if (navigationSlot != null) {
      PrismNavigationSlot(content = navigationSlot)
    }
    Text(
      text = title,
      modifier = Modifier.weight(1f),
      style = Prism.typography.cardTitle,
      color = Prism.color.titleColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    if (actions != null) {
      PrismTitleActions(content = actions)
    }
  }
}

@Composable
private fun PrismNavigationSlot(content: @Composable () -> Unit) {
  Box(
    modifier = Modifier.size(Prism.dimens.iconM + Prism.dimens.spacingXs),
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}

@Composable
private fun PrismTitleActions(content: @Composable RowScope.() -> Unit) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
    verticalAlignment = Alignment.CenterVertically,
    content = content,
  )
}

@Composable
private fun PrismScreenTitleSubtitle(text: String) {
  Text(
    text = text,
    style = Prism.typography.bodySmall,
    color = Prism.color.labelColor,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
}
