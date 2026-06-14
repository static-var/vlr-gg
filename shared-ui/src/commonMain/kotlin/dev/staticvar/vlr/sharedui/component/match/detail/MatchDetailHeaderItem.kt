/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:Suppress("LongParameterList")

package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.divider.PrismDividerStyle
import dev.staticvar.designsystem.component.header.PrismHeader
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.TeamDetails
import dev.staticvar.vlr.sharedui.component.common.FavoriteTicketCardBox

/**
 * Match detail summary card for event metadata, current score, participating teams, and optional
 * caller-provided actions.
 */
@Composable
public fun MatchDetailHeaderItem(
  match: MatchDetails,
  modifier: Modifier = Modifier,
  actions: (@Composable RowScope.() -> Unit)? = null,
) {
  FavoriteTicketCardBox(selected = match.isFavorite, modifier = modifier.fillMaxWidth()) {
    PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        PrismHeader(text = match.event.name.ifBlank { "match" })
        PrismTag(text = match.event.status.matchDetailStatusLabel, style = match.event.status.matchDetailStatusTagStyle)
      }

      Text(
        text = match.matchDetailTitle(),
        modifier = Modifier.padding(top = Prism.dimens.spacingS),
        style = Prism.typography.sectionTitle,
        color = Prism.color.titleColor,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      match.matchDetailMeta().takeIf(String::isNotBlank)?.let { meta ->
        Text(
          text = meta,
          modifier = Modifier.padding(top = Prism.dimens.spacingXs),
          style = Prism.typography.bodySmall,
          color = Prism.color.labelColor,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Column(modifier = Modifier.padding(top = Prism.dimens.spacingM)) {
        match.teams.take(2).forEachIndexed { index, team ->
          if (index > 0) {
            PrismDivider(style = PrismDividerStyle.Hairline)
          }
          MatchDetailTeamScoreRow(team = team)
        }
      }

      MatchDetailStatStrip(
        firstValue = match.matchDetailDateStat(),
        firstLabel = "Date",
        secondValue = match.matchDetailPatchStat(),
        secondLabel = "Patch",
        thirdValue = match.matchDetailBanStat(),
        thirdLabel = "Bans",
        modifier = Modifier.padding(top = Prism.dimens.spacingS),
      )

      if (actions != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = Prism.dimens.spacingS),
          horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
          content = actions,
        )
      }
    }
  }
}

@Composable
private fun MatchDetailTeamScoreRow(team: TeamDetails, modifier: Modifier = Modifier) {
  MatchDetailTeamScoreRow(
    teamName = team.name,
    score = team.score?.toString() ?: "-",
    modifier = modifier,
    region = team.region,
    imageUrl = team.img,
    isWinner = team.isWinner == true,
  )
}

@Composable
private fun MatchDetailStatStrip(
  firstValue: String,
  firstLabel: String,
  secondValue: String,
  secondLabel: String,
  thirdValue: String,
  thirdLabel: String,
  modifier: Modifier = Modifier,
) {
  Row(modifier = modifier.fillMaxWidth()) {
    MatchDetailStatCell(value = firstValue, label = firstLabel, modifier = Modifier.weight(1f))
    MatchDetailStatCell(value = secondValue, label = secondLabel, modifier = Modifier.weight(1f))
    MatchDetailStatCell(value = thirdValue, label = thirdLabel, modifier = Modifier.weight(1f))
  }
}

@Composable
private fun MatchDetailStatCell(value: String, label: String, modifier: Modifier = Modifier) {
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
      ) {
        Text(
          text = value,
          modifier = Modifier.fillMaxWidth(),
          style = Prism.typography.cardTitle,
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
