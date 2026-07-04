/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

@Immutable
public data class DetailStatItem(
  val value: String,
  val label: String,
)

@Composable
public fun DetailStatStrip(items: List<DetailStatItem>, modifier: Modifier = Modifier) {
  Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
    items.forEach { item ->
      DetailStatCell(value = item.value, label = item.label, modifier = Modifier.weight(1f))
    }
  }
}

@Composable
private fun DetailStatCell(value: String, label: String, modifier: Modifier = Modifier) {
  PrismSurface(
    modifier = modifier.heightIn(min = Prism.dimens.controlHeight),
    color = Prism.color.surface,
    border = BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = Prism.dimens.controlHeight)
        .padding(Prism.dimens.spacingS),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = value,
          modifier = Modifier.fillMaxWidth(),
          style = Prism.typography.bodySmall,
          color = Prism.color.titleColor,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = label,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = Prism.dimens.spacingXs),
          style = Prism.typography.caption,
          color = Prism.color.labelColor,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
