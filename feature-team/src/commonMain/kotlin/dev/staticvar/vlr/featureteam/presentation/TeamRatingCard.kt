/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureteam.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.RankingCircuit
import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.model.TeamRankingProfile
import dev.staticvar.vlr.domain.model.TeamRankingRecord
import dev.staticvar.vlr.domain.model.TeamRankingResult
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerHiddenIcon
import dev.staticvar.vlr.sharedui.spoilers.SpoilerScore
import org.jetbrains.compose.resources.stringResource
import vlr.feature_team.generated.resources.Res
import vlr.feature_team.generated.resources.team_rating_beta
import vlr.feature_team.generated.resources.team_rating_circuit
import vlr.feature_team.generated.resources.team_rating_circuit_collegiate
import vlr.feature_team.generated.resources.team_rating_circuit_gc
import vlr.feature_team.generated.resources.team_rating_circuit_offseason
import vlr.feature_team.generated.resources.team_rating_circuit_other
import vlr.feature_team.generated.resources.team_rating_circuit_tier3
import vlr.feature_team.generated.resources.team_rating_circuit_vcl
import vlr.feature_team.generated.resources.team_rating_circuit_vct
import vlr.feature_team.generated.resources.team_rating_form
import vlr.feature_team.generated.resources.team_rating_form_description
import vlr.feature_team.generated.resources.team_rating_form_hidden
import vlr.feature_team.generated.resources.team_rating_form_record
import vlr.feature_team.generated.resources.team_rating_hide_matches
import vlr.feature_team.generated.resources.team_rating_how
import vlr.feature_team.generated.resources.team_rating_inactive
import vlr.feature_team.generated.resources.team_rating_loading
import vlr.feature_team.generated.resources.team_rating_loss
import vlr.feature_team.generated.resources.team_rating_loss_description
import vlr.feature_team.generated.resources.team_rating_map_elo
import vlr.feature_team.generated.resources.team_rating_maps
import vlr.feature_team.generated.resources.team_rating_matches
import vlr.feature_team.generated.resources.team_rating_no_rank
import vlr.feature_team.generated.resources.team_rating_rank
import vlr.feature_team.generated.resources.team_rating_record
import vlr.feature_team.generated.resources.team_rating_record_note
import vlr.feature_team.generated.resources.team_rating_region
import vlr.feature_team.generated.resources.team_rating_region_americas
import vlr.feature_team.generated.resources.team_rating_region_china
import vlr.feature_team.generated.resources.team_rating_region_emea
import vlr.feature_team.generated.resources.team_rating_region_pacific
import vlr.feature_team.generated.resources.team_rating_score
import vlr.feature_team.generated.resources.team_rating_series_elo
import vlr.feature_team.generated.resources.team_rating_show_matches
import vlr.feature_team.generated.resources.team_rating_title
import vlr.feature_team.generated.resources.team_rating_unavailable_body
import vlr.feature_team.generated.resources.team_rating_unavailable_title
import vlr.feature_team.generated.resources.team_rating_unranked_body
import vlr.feature_team.generated.resources.team_rating_unranked_title
import vlr.feature_team.generated.resources.team_rating_win
import vlr.feature_team.generated.resources.team_rating_win_description
import vlr.feature_team.generated.resources.team_rating_win_rate
import vlr.feature_team.generated.resources.team_rating_worldwide
import kotlin.math.roundToInt

/** Team rankings and result history that expand within the surrounding screen's scroll content. */
@Composable
internal fun TeamRatingCard(state: TeamRatingState, onMatchSelected: (String) -> Unit, modifier: Modifier = Modifier) {
  var infoVisible by rememberSaveable { mutableStateOf(false) }
  PrismCard(modifier = modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      TeamRatingHeader(onInfoSelected = { infoVisible = true })
      when (state) {
        TeamRatingState.Loading ->
          Text(
            text = stringResource(Res.string.team_rating_loading),
            style = Prism.typography.bodySmall,
            color = Prism.color.labelColor,
          )

        TeamRatingState.Unavailable ->
          TeamRatingMessage(
            title = stringResource(Res.string.team_rating_unavailable_title),
            body = stringResource(Res.string.team_rating_unavailable_body),
          )

        is TeamRatingState.Available ->
          if (state.profile.matches.played == 0) {
            TeamRatingMessage(
              title = stringResource(Res.string.team_rating_unranked_title),
              body = stringResource(Res.string.team_rating_unranked_body),
            )
          } else {
            TeamRatingContent(profile = state.profile, onMatchSelected = onMatchSelected)
          }
      }
    }
  }
  TeamRatingInfoSheet(visible = infoVisible, onDismiss = { infoVisible = false })
}

@Composable
private fun TeamRatingHeader(onInfoSelected: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      modifier = Modifier.weight(1f),
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(Res.string.team_rating_title),
        modifier = Modifier.semantics { heading() },
        style = Prism.typography.cardTitle,
        color = Prism.color.titleColor,
      )
      PrismTag(text = stringResource(Res.string.team_rating_beta), style = PrismTagStyle.Accent)
    }
    Box(
      modifier = Modifier.heightIn(
        min = Prism.dimens.controlHeight,
      ).clickable(role = Role.Button, onClick = onInfoSelected),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = stringResource(Res.string.team_rating_how),
        style = Prism.typography.label,
        color = Prism.color.accent,
        textDecoration = TextDecoration.Underline,
      )
    }
  }
}

@Composable
private fun TeamRatingMessage(title: String, body: String) {
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
    Text(text = title, style = Prism.typography.cardTitle, color = Prism.color.titleColor)
    Text(text = body, style = Prism.typography.bodySmall, color = Prism.color.labelColor)
  }
}

@Composable
private fun TeamRatingContent(profile: TeamRankingProfile, onMatchSelected: (String) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).border(Prism.dimens.strokeDefault, Prism.color.stroke),
  ) {
    TeamRankCell(
      rank = profile.rank,
      label = stringResource(Res.string.team_rating_worldwide),
      modifier = Modifier.weight(1f).fillMaxHeight().background(Prism.color.accentSubtle),
      accented = true,
    )
    Box(Modifier.fillMaxHeight().width(Prism.dimens.strokeDefault).background(Prism.color.stroke))
    TeamRankCell(rank = profile.regionRank, label = teamRegionLabel(profile.region), modifier = Modifier.weight(1f))
    Box(Modifier.fillMaxHeight().width(Prism.dimens.strokeDefault).background(Prism.color.stroke))
    TeamRankCell(
      rank = profile.circuitRank,
      label = teamCircuitLabel(profile.primaryCircuit),
      modifier = Modifier.weight(1f),
    )
  }
  if (!profile.active) {
    Text(
      text = stringResource(Res.string.team_rating_inactive),
      style = Prism.typography.caption,
      color = Prism.color.labelColor,
    )
  }
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
    TeamEloValue(
      value = profile.elo,
      label = stringResource(Res.string.team_rating_series_elo),
      modifier = Modifier.weight(1f),
    )
    TeamEloValue(
      value = profile.mapElo,
      label = stringResource(Res.string.team_rating_map_elo),
      modifier = Modifier.weight(1f),
    )
  }
  if (profile.form.isNotEmpty()) {
    PrismDivider(modifier = Modifier.padding(top = Prism.dimens.spacingS))
    TeamRecentForm(profile = profile, onMatchSelected = onMatchSelected)
  }
  PrismDivider(modifier = Modifier.padding(top = Prism.dimens.spacingS))
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingS),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    TeamRecordColumn(
      label = stringResource(Res.string.team_rating_matches),
      record = profile.matches,
      modifier = Modifier.weight(1f),
    )
    TeamRecordColumn(
      label = stringResource(Res.string.team_rating_maps),
      record = profile.maps,
      modifier = Modifier.weight(1f),
    )
  }
  Text(
    text = stringResource(Res.string.team_rating_record_note),
    style = Prism.typography.caption,
    color = Prism.color.labelColor,
  )
}

@Composable
private fun TeamRankCell(rank: Int?, label: String, modifier: Modifier = Modifier, accented: Boolean = false) {
  Column(
    modifier = modifier.padding(Prism.dimens.spacingS),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
  ) {
    SpoilerScore(
      text =
      rank?.let { stringResource(Res.string.team_rating_rank, it) } ?: stringResource(Res.string.team_rating_no_rank),
      style = Prism.typography.numericPrimary,
      color = if (accented) Prism.color.accent else Prism.color.titleColor,
    )
    Text(text = label, style = Prism.typography.label, color = Prism.color.labelColor)
  }
}

@Composable
private fun TeamEloValue(value: Double, label: String, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SpoilerScore(text = value.roundToInt().toString(), style = Prism.typography.label, color = Prism.color.bodyColor)
    Text(text = label, style = Prism.typography.label, color = Prism.color.labelColor)
  }
}

@Composable
private fun TeamRecentForm(profile: TeamRankingProfile, onMatchSelected: (String) -> Unit) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  val form = profile.form.take(10)
  val wins = form.count { it }
  val losses = form.size - wins
  val hidden = LocalSpoilerMode.current.enabled
  val description =
    if (hidden) {
      stringResource(Res.string.team_rating_form_hidden, form.size)
    } else {
      stringResource(Res.string.team_rating_form_description, form.size, wins, losses)
    }
  val action =
    stringResource(if (expanded) Res.string.team_rating_hide_matches else Res.string.team_rating_show_matches)
  Column(
    modifier = Modifier.padding(top = Prism.dimens.spacingS),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(Res.string.team_rating_form),
        modifier = Modifier.weight(1f),
        style = Prism.typography.label,
        color = Prism.color.labelColor,
      )
      SpoilerScore(
        text = stringResource(Res.string.team_rating_form_record, wins, losses, form.size),
        style = Prism.typography.caption,
        color = Prism.color.bodyColor,
      )
    }
    Row(
      modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = Prism.dimens.controlHeight)
        .clickable(enabled = profile.recent.isNotEmpty(), role = Role.Button, onClickLabel = action) {
          expanded =
            !expanded
        }
        .semantics(mergeDescendants = true) {
          contentDescription = description
          stateDescription = action
        },
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        form.forEachIndexed { index, won ->
          TeamFormTile(
            won = won,
            newest = index == 0,
            modifier = Modifier.weight(1f).aspectRatio(1f).clearAndSetSemantics {},
          )
        }
        repeat(10 - form.size) { Spacer(modifier = Modifier.weight(1f)) }
      }
      TeamFormChevron(expanded = expanded)
    }
    if (expanded) {
      Column(modifier = Modifier.fillMaxWidth().border(Prism.dimens.strokeDefault, Prism.color.stroke)) {
        profile.recent.take(10).forEachIndexed { index, result ->
          if (index > 0) HorizontalDivider(thickness = Prism.dimens.strokeDefault, color = Prism.color.divider)
          TeamRecentResult(result = result, onMatchSelected = onMatchSelected)
        }
      }
    }
  }
}

@Composable
private fun TeamFormTile(won: Boolean, modifier: Modifier = Modifier, newest: Boolean = false) {
  val hidden = LocalSpoilerMode.current.enabled
  val style = if (hidden) {
    PrismTagStyle.Neutral
  } else if (won) {
    PrismTagStyle.Success
  } else {
    PrismTagStyle.Danger
  }
  val outcome =
    stringResource(if (won) Res.string.team_rating_win_description else Res.string.team_rating_loss_description)
  Box(
    modifier =
    modifier.background(style.containerColor).border(style.border)
      .then(if (newest) Modifier.border(Prism.dimens.strokeThick, Prism.color.accent) else Modifier),
    contentAlignment = Alignment.Center,
  ) {
    if (hidden) {
      SpoilerHiddenIcon(modifier = Modifier.size(Prism.dimens.iconS))
    } else {
      Text(
        text = stringResource(if (won) Res.string.team_rating_win else Res.string.team_rating_loss),
        modifier = Modifier.semantics { contentDescription = outcome },
        style = Prism.typography.label,
        color = style.contentColor,
      )
    }
  }
}

@Composable
private fun TeamFormChevron(expanded: Boolean) {
  val color = Prism.color.labelColor
  Canvas(
    modifier = Modifier.size(Prism.dimens.iconS).graphicsLayer { rotationZ = if (expanded) 180f else 0f },
  ) {
    val unit = size.width / 16f
    listOf(2 to 5, 4 to 7, 6 to 9, 8 to 9, 10 to 7, 12 to 5).forEach { (x, y) ->
      drawRect(color = color, topLeft = Offset(x * unit, y * unit), size = Size(unit * 2, unit * 2))
    }
  }
}

@Composable
private fun TeamRecentResult(result: TeamRankingResult, onMatchSelected: (String) -> Unit) {
  val teamScore = result.teamScore
  val opponentScore = result.opponentScore
  Row(
    modifier =
    Modifier.fillMaxWidth().clickable(role = Role.Button) { onMatchSelected(result.matchId) }
      .padding(Prism.dimens.spacingS),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    TeamFormTile(won = result.won, modifier = Modifier.size(28.dp))
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
      Text(text = result.opponentName, style = Prism.typography.label, color = Prism.color.titleColor)
      Text(
        text = listOfNotNull(
          result.playedOn,
          result.event,
          result.stage,
        ).filter(String::isNotBlank).joinToString(" · "),
        style = Prism.typography.caption,
        color = Prism.color.labelColor,
      )
    }
    if (LocalSpoilerMode.current.enabled) {
      SpoilerHiddenIcon()
    } else if (teamScore != null && opponentScore != null) {
      SpoilerScore(
        text = stringResource(Res.string.team_rating_score, teamScore, opponentScore),
        style = Prism.typography.numericSecondary,
        color = Prism.color.bodyColor,
      )
    }
  }
}

@Composable
private fun TeamRecordColumn(label: String, record: TeamRankingRecord, modifier: Modifier = Modifier) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
    Text(text = label, style = Prism.typography.label, color = Prism.color.labelColor)
    if (LocalSpoilerMode.current.enabled) {
      SpoilerHiddenIcon()
    } else {
      Text(
        text = stringResource(Res.string.team_rating_record, record.wins, record.losses),
        style = Prism.typography.numericSecondary,
        color = Prism.color.titleColor,
      )
      Text(
        text = stringResource(
          Res.string.team_rating_win_rate,
          if (record.played > 0) teamWinRate(record.winRate) else stringResource(Res.string.team_rating_no_rank),
        ),
        style = Prism.typography.caption,
        color = Prism.color.labelColor,
      )
    }
  }
}

private fun teamWinRate(rate: Double): String {
  val tenths = (rate * 1000).roundToInt()
  return "${tenths / 10}.${tenths % 10}%"
}

@Composable
private fun teamRegionLabel(region: RankingRegion?): String = stringResource(
  when (region) {
    RankingRegion.Americas -> Res.string.team_rating_region_americas
    RankingRegion.Emea -> Res.string.team_rating_region_emea
    RankingRegion.Pacific -> Res.string.team_rating_region_pacific
    RankingRegion.China -> Res.string.team_rating_region_china
    null -> Res.string.team_rating_region
  },
)

@Composable
private fun teamCircuitLabel(circuit: RankingCircuit?): String = stringResource(
  when (circuit) {
    RankingCircuit.Vct -> Res.string.team_rating_circuit_vct
    RankingCircuit.Vcl -> Res.string.team_rating_circuit_vcl
    RankingCircuit.Tier3 -> Res.string.team_rating_circuit_tier3
    RankingCircuit.GameChangers -> Res.string.team_rating_circuit_gc
    RankingCircuit.Collegiate -> Res.string.team_rating_circuit_collegiate
    RankingCircuit.Offseason -> Res.string.team_rating_circuit_offseason
    RankingCircuit.Other -> Res.string.team_rating_circuit_other
    RankingCircuit.All, null -> Res.string.team_rating_circuit
  },
)
