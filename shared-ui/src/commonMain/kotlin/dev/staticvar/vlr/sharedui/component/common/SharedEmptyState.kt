/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import dev.staticvar.vlr.sharedui.illustration.EmptyStateIllustration

@Composable
public fun SharedEmptyState(
  artwork: EmptyStateArtwork,
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  compact: Boolean = false,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  SharedIllustratedState(artwork, title, message, modifier, compact) {
    if (actionLabel != null && onAction != null) {
      PrismButton(onClick = onAction, style = PrismButtonStyle.Secondary) { Text(actionLabel) }
    }
  }
}

@Composable
internal fun SharedIllustratedState(
  artwork: EmptyStateArtwork,
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  compact: Boolean = false,
  actions: @Composable () -> Unit = {},
) {
  BoxWithConstraints(modifier = if (compact) modifier.fillMaxWidth() else modifier.fillMaxSize()) {
    val artworkHeight = if (compact) 160.dp else if (constraints.hasBoundedHeight) {
      (maxHeight * 0.45f).coerceIn(120.dp, 300.dp)
    } else {
      300.dp
    }
    val artworkWidth = minOf(maxWidth, artworkHeight * (360f / 460f))
    val contentModifier = if (compact) Modifier.fillMaxWidth() else {
      Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        .heightIn(min = if (constraints.hasBoundedHeight) maxHeight else 0.dp)
    }
    Column(
      modifier = contentModifier.padding(vertical = Prism.dimens.spacingM),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS, Alignment.CenterVertically),
    ) {
      EmptyStateIllustration(
        artwork = artwork,
        modifier = Modifier.size(width = artworkWidth, height = artworkHeight),
      )
      Text(title, style = Prism.typography.sectionTitle, color = Prism.color.titleColor, textAlign = TextAlign.Center)
      Text(message, style = Prism.typography.bodySmall, color = Prism.color.bodyColor, textAlign = TextAlign.Center)
      actions()
    }
  }
}
