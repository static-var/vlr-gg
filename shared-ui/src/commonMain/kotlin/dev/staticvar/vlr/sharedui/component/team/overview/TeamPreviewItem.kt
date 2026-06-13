/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.team.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIcon
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconSize
import dev.staticvar.designsystem.component.header.PrismHeader
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.TeamInfo
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon

/**
 * Compact team list item for team indexes, favorites, and ranking drill-downs.
 */
@Composable
public fun TeamPreviewItem(team: TeamInfo, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
  val upcomingSoonCount = remember(team.upcomingMatches) { team.matchesInNext7Days().size }

  PrismCard(
    modifier = modifier,
    style = if (team.isFavorite) PrismCardStyle.Outlined else PrismCardStyle.Filled,
    onClick = onClick,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
      ) {
        if (team.isFavorite) {
          PrismFavoriteIcon(selected = true, size = PrismFavoriteIconSize.Small)
        }
        PrismHeader(text = team.region.ifBlank { "team" })
      }
      if (team.rank > 0) {
        PrismTag(text = "#${team.rank}", style = PrismTagStyle.Accent)
      } else {
        PrismTag(text = "unranked")
      }
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = Prism.dimens.spacingS),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      SharedNetworkIcon(
        imageUrl = team.logoUrl,
        contentDescription = team.name,
        size = PrismIconSize.Large,
        style = PrismIconStyle.Bordered,
        tint = if (team.isFavorite) PrismIconTint.Alt else PrismIconTint.Primary,
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = team.name,
          style = Prism.typography.cardTitle,
          color = Prism.color.titleColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = team.subtitle,
          modifier = Modifier.padding(top = Prism.dimens.spacingXs),
          style = Prism.typography.bodySmall,
          color = Prism.color.labelColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    if (team.roster.isNotEmpty() || upcomingSoonCount > 0) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = Prism.dimens.spacingS),
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
      ) {
        PrismTag(text = "${team.roster.size} players", style = PrismTagStyle.Info)
        PrismTag(text = "$upcomingSoonCount next 7d", style = PrismTagStyle.Info)
      }
    }
  }
}

private val TeamInfo.subtitle: String
  get() =
    listOfNotNull(
      tag.takeIf(String::isNotBlank),
      country.takeIf(String::isNotBlank),
    ).joinToString(" • ").ifBlank { "Team profile" }
