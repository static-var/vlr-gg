/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerScore

/**
 * Compact team score row used by match detail hero, map scores, and history rows.
 *
 * Winner rows use accent color for the team name and score. Logo rendering is optional so the same
 * row can serve dense history sections without spending image space. Spoiler mode replaces the
 * score and removes winner emphasis.
 */
@Composable
public fun MatchDetailTeamScoreRow(
  teamName: String,
  score: String,
  modifier: Modifier = Modifier,
  region: String? = null,
  imageUrl: String? = null,
  isWinner: Boolean = false,
  showLogo: Boolean = true,
  onClick: (() -> Unit)? = null,
) {
  val spoilersHidden = LocalSpoilerMode.current.enabled
  val resolvedWinner = isWinner && !spoilersHidden
  val clickModifier = onClick?.let { clickAction ->
    Modifier.clickable(role = Role.Button, onClick = clickAction)
  } ?: Modifier

  Row(
    modifier = modifier
      .fillMaxWidth()
      .then(clickModifier)
      .padding(vertical = Prism.dimens.spacingXs),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    if (showLogo) {
      SharedNetworkIcon(
        imageUrl = imageUrl,
        contentDescription = teamName,
        size = PrismIconSize.Large,
        style = PrismIconStyle.Bordered,
        tint = PrismIconTint.None,
      )
    }
    Text(
      text = teamName.ifBlank { "TBD" },
      modifier = Modifier.weight(1f),
      style = Prism.typography.headline,
      color = if (resolvedWinner) Prism.color.accent else Prism.color.labelColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    region?.takeIf(String::isNotBlank)?.let { regionLabel ->
      Text(
        text = regionLabel,
        style = Prism.typography.caption,
        color = Prism.color.labelColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(modifier = Modifier.weight(0.05f))
    SpoilerScore(
      text = score,
      style = Prism.typography.headline,
      color = if (resolvedWinner) Prism.color.accent else Prism.color.labelColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
