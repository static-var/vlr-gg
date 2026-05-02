/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.prism.Prism

/**
 * Brutalist section title block with optional mono pre-label and trailing content.
 */
@Composable
public fun PrismSectionTitle(
  title: String,
  modifier: Modifier = Modifier,
  preLabel: String? = null,
  showDivider: Boolean = true,
  trailing: (@Composable RowScope.() -> Unit)? = null,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
  ) {
    if (preLabel != null) {
      Text(
        text = preLabel.uppercase(),
        style = Prism.typography.caption,
        color = Prism.color.labelColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      Text(
        text = title,
        modifier = Modifier.weight(1f),
        style = Prism.typography.sectionTitle,
        color = Prism.color.titleColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      if (trailing != null) {
        trailing()
      }
    }

    if (showDivider) {
      HorizontalDivider(
        thickness = Prism.dimens.strokeDefault,
        color = Prism.color.stroke,
      )
    }
  }
}
