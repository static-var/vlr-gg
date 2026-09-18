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
import dev.staticvar.designsystem.component.header.PrismHeader
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.TeamInfo
import dev.staticvar.vlr.sharedui.component.common.FavoriteTicketCardBox
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.shared_team_label
import vlr.shared_ui.generated.resources.shared_team_players
import vlr.shared_ui.generated.resources.shared_team_profile
import vlr.shared_ui.generated.resources.shared_team_unranked
import vlr.shared_ui.generated.resources.shared_team_upcoming

/**
 * Compact team list item for team indexes, favorites, and ranking drill-downs.
 */
@Composable
public fun TeamPreviewItem(team: TeamInfo, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
  val upcomingSoonCount = remember(team.upcomingMatches) { team.matchesInNext7Days().size }

  FavoriteTicketCardBox(selected = team.isFavorite, modifier = modifier) {
    PrismCard(
      modifier = Modifier.fillMaxWidth(),
      style = if (team.isFavorite) PrismCardStyle.Outlined else PrismCardStyle.Filled,
      onClick = onClick,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        PrismHeader(
          text = team.regionLabel.ifBlank { team.region }.ifBlank { stringResource(Res.string.shared_team_label) },
        )
        if (team.rank > 0) {
          PrismTag(text = "#${team.rank}", style = PrismTagStyle.Accent)
        } else {
          PrismTag(text = stringResource(Res.string.shared_team_unranked))
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
          tint = PrismIconTint.None,
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
          PrismTag(
            text = pluralStringResource(Res.plurals.shared_team_players, team.roster.size, team.roster.size),
            style = PrismTagStyle.Info,
          )
          PrismTag(
            text = stringResource(Res.string.shared_team_upcoming, upcomingSoonCount),
            style = PrismTagStyle.Info,
          )
        }
      }
    }
  }
}

private val TeamInfo.subtitle: String
  @Composable
  get() =
    listOfNotNull(
      tag.takeIf(String::isNotBlank),
      country.takeIf(String::isNotBlank),
    ).joinToString(" • ").ifBlank { stringResource(Res.string.shared_team_profile) }
