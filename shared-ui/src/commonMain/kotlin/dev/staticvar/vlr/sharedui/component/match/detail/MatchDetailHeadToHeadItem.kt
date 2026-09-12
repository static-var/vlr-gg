/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
 * Head-to-head win distribution and previous encounter rows.
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
  val firstColor = Prism.color.accent
  val secondColor = Prism.color.labelColor
  PrismSurface(
    modifier = Modifier.fillMaxWidth().clearAndSetSemantics {
      contentDescription = "${summary.firstTeamName}: ${summary.firstTeamWins} wins. " +
        "${summary.secondTeamName}: ${summary.secondTeamWins} wins. ${summary.totalPlayed} played."
    },
    color = Prism.color.surfaceVariant,
    border = BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(Prism.dimens.spacingS),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
      ) {
        HeadToHeadWinLabel(
          name = summary.firstTeamName,
          wins = summary.firstTeamWins,
          color = firstColor,
          modifier = Modifier.weight(1f),
        )
        HeadToHeadWinLabel(
          name = summary.secondTeamName,
          wins = summary.secondTeamWins,
          color = secondColor,
          modifier = Modifier.weight(1f),
          textAlign = TextAlign.End,
        )
      }
      Row(modifier = Modifier.fillMaxWidth().height(Prism.dimens.spacingS)) {
        if (summary.firstTeamWins > 0) {
          Box(Modifier.weight(summary.firstTeamWins.toFloat()).height(Prism.dimens.spacingS).background(firstColor))
        }
        if (summary.secondTeamWins > 0) {
          Box(Modifier.weight(summary.secondTeamWins.toFloat()).height(Prism.dimens.spacingS).background(secondColor))
        }
      }
      Text(
        text = "${summary.totalPlayed} played",
        modifier = Modifier.fillMaxWidth(),
        style = Prism.typography.caption,
        color = Prism.color.labelColor,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun HeadToHeadWinLabel(
  name: String,
  wins: Int,
  color: Color,
  modifier: Modifier = Modifier,
  textAlign: TextAlign = TextAlign.Start,
) {
  Column(modifier = modifier) {
    Text(
      text = wins.toString(),
      modifier = Modifier.fillMaxWidth(),
      style = Prism.typography.cardTitle,
      color = color,
      textAlign = textAlign,
    )
    Text(
      text = name,
      modifier = Modifier.fillMaxWidth(),
      style = Prism.typography.caption,
      color = color,
      textAlign = textAlign,
      maxLines = 2,
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
