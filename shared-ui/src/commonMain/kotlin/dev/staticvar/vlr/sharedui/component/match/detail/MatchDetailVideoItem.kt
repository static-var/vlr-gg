/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.vlr.domain.model.VideoReference

/**
 * Compact stream or VOD action for match detail media sections.
 */
@Composable
public fun MatchDetailVideoItem(
  video: VideoReference,
  typeLabel: String,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  PrismButton(
    onClick = onClick ?: {},
    modifier = modifier,
    style = PrismButtonStyle.Tertiary,
    enabled = onClick != null,
  ) {
    Text(
      text = video.matchDetailVideoLabel(typeLabel = typeLabel),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

private fun VideoReference.matchDetailVideoLabel(typeLabel: String): String {
  val title = name.ifBlank { url }.ifBlank { typeLabel }
  return "${typeLabel.uppercase()}: $title"
}
