/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.divider.PrismDividerStyle
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.PreviousEncounter
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerScore

/**
 * Compact head-to-head section with centered summary stats and dense previous encounter rows.
 * Spoiler mode removes the win summary, score text, and winner emphasis.
 */
@Composable
public fun MatchDetailHeadToHeadItem(
  encounters: List<PreviousEncounter>,
  modifier: Modifier = Modifier,
  onEncounterSelected: ((String) -> Unit)? = null,
) {
  if (encounters.isEmpty()) return

  val spoilersHidden = LocalSpoilerMode.current.enabled
  val summary = if (spoilersHidden) null else encounters.matchDetailHeadToHeadSummary()

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    PrismSectionTitle(
      title = "Head to head",
      preLabel = "history",
      trailing = {
        PrismTag(
          text = if (spoilersHidden) {
            "Scores hidden"
          } else {
            summary?.matchDetailTagLabel() ?: "${encounters.size} matches"
          },
          style = PrismTagStyle.Neutral,
        )
      },
    )
    summary?.let { headToHeadSummary ->
      MatchDetailHeadToHeadSummaryStrip(summary = headToHeadSummary)
    }
    MatchDetailHeadToHeadRows(
      encounters = encounters,
      onEncounterSelected = onEncounterSelected,
      spoilersHidden = spoilersHidden,
    )
  }
}

@Composable
private fun MatchDetailHeadToHeadSummaryStrip(summary: MatchDetailHeadToHeadSummary) {
  PrismSurface(
    modifier = Modifier.fillMaxWidth(),
    color = Prism.color.surfaceVariant,
    border = BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke),
  ) {
    Row(modifier = Modifier.fillMaxWidth()) {
      MatchDetailHeadToHeadSummaryCell(
        value = summary.firstTeamWins.toString(),
        label = "${summary.firstTeamName} wins",
        modifier = Modifier.weight(1f),
      )
      MatchDetailHeadToHeadSummaryCell(
        value = summary.secondTeamWins.toString(),
        label = "${summary.secondTeamName} wins",
        modifier = Modifier.weight(1f),
      )
      MatchDetailHeadToHeadSummaryCell(
        value = summary.totalPlayed.toString(),
        label = "Played",
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun MatchDetailHeadToHeadSummaryCell(value: String, label: String, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .heightIn(min = Prism.dimens.controlHeight)
      .padding(Prism.dimens.spacingS),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = value,
      style = Prism.typography.cardTitle,
      color = Prism.color.titleColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = label,
      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
      style = Prism.typography.caption,
      color = Prism.color.labelColor,
      textAlign = TextAlign.Center,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun MatchDetailHeadToHeadRows(
  encounters: List<PreviousEncounter>,
  onEncounterSelected: ((String) -> Unit)?,
  spoilersHidden: Boolean,
) {
  PrismSurface(
    modifier = Modifier.fillMaxWidth(),
    color = Prism.color.surfaceVariant,
    border = BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      encounters.forEachIndexed { index, encounter ->
        if (index > 0) {
          PrismDivider(style = PrismDividerStyle.Hairline)
        }
        MatchDetailHeadToHeadRow(
          encounter = encounter,
          onEncounterSelected = onEncounterSelected,
          spoilersHidden = spoilersHidden,
        )
      }
    }
  }
}

@Composable
private fun MatchDetailHeadToHeadRow(
  encounter: PreviousEncounter,
  onEncounterSelected: ((String) -> Unit)?,
  spoilersHidden: Boolean,
) {
  val encounterSelected = onEncounterSelected
  val enabled = encounterSelected != null && encounter.id.isNotBlank()
  val clickModifier = if (enabled && encounterSelected != null) {
    Modifier.clickable(role = Role.Button) { encounterSelected(encounter.id) }
  } else {
    Modifier
  }
  val teams = encounter.teams.take(2)
  val firstTeam = teams.getOrNull(0)
  val secondTeam = teams.getOrNull(1)

  Row(
    modifier = clickModifier
      .fillMaxWidth()
      .heightIn(min = Prism.dimens.touchTargetMin)
      .padding(horizontal = Prism.dimens.spacingS, vertical = Prism.dimens.spacingXs),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    HeadToHeadTeamText(team = firstTeam, spoilersHidden = spoilersHidden, modifier = Modifier.weight(1f))
    SpoilerScore(
      text = "${firstTeam?.matchDetailScoreText() ?: "-"} : ${secondTeam?.matchDetailScoreText() ?: "-"}",
      style = Prism.typography.cardTitle,
      color = Prism.color.titleColor,
      maxLines = 1,
    )
    HeadToHeadTeamText(
      team = secondTeam,
      spoilersHidden = spoilersHidden,
      modifier = Modifier.weight(1f),
      textAlign = TextAlign.End,
    )
  }
}

@Composable
private fun HeadToHeadTeamText(
  team: TeamPreview?,
  spoilersHidden: Boolean,
  modifier: Modifier = Modifier,
  textAlign: TextAlign = TextAlign.Start,
) {
  Text(
    text = team?.name?.ifBlank { "TBD" } ?: "TBD",
    modifier = modifier,
    style = Prism.typography.cardTitle,
    color = if (!spoilersHidden && team?.isWinner == true) Prism.color.accent else Prism.color.labelColor,
    textAlign = textAlign,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
}
