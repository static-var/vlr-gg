/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.shared_favorite_source

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FavoriteReasonTags(
  labels: List<String>,
  modifier: Modifier = Modifier,
  horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(Prism.dimens.spacingXs),
) {
  if (labels.isEmpty()) return
  FlowRow(
    modifier = modifier,
    horizontalArrangement = horizontalArrangement,
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
  ) {
    labels.forEach { label ->
      val favoriteDescription = stringResource(Res.string.shared_favorite_source, label)
      PrismTag(
        text = label,
        style = PrismTagStyle.Accent,
        modifier = Modifier.semantics { contentDescription = favoriteDescription },
      )
    }
  }
}
