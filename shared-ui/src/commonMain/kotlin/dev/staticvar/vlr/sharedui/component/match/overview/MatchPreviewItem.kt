/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.overview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.divider.PrismDividerStyle
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.CurrentMatchMap
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.sharedui.component.common.FavoriteTicketCardBox
import dev.staticvar.vlr.sharedui.component.common.formatMatchPreviewTime
import dev.staticvar.vlr.sharedui.component.match.MatchFavoriteReasons
import dev.staticvar.vlr.sharedui.component.match.MatchSharedContent
import dev.staticvar.vlr.sharedui.component.match.matchSharedBounds
import dev.staticvar.vlr.sharedui.component.match.rememberMatchSharedText
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerContent
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.match_event_live
import vlr.shared_ui.generated.resources.match_live_map_score_description

@Composable
public fun MatchPreviewItem(
  modifier: Modifier = Modifier,
  matchPreview: MatchPreview,
  isSharing: Boolean = false,
  footerAction: (@Composable () -> Unit)? = null,
  onLongClick: (() -> Unit)? = null,
  onClick: (() -> Unit)? = null,
) {
  val selectionAnimation = Prism.anim.selection
  val liveMap = matchPreview.currentMap.takeUnless { LocalSpoilerMode.current.enabled }
  FavoriteTicketCardBox(
    selected = matchPreview.isFavorite,
    modifier = modifier,
  ) {
    PrismCard(
      modifier = Modifier.fillMaxWidth().matchSharedBounds(matchPreview.id, MatchSharedContent.Card),
      style = if (matchPreview.isFavorite) PrismCardStyle.Outlined else PrismCardStyle.Filled,
      onClick = onClick,
      onLongClick = onLongClick,
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      ) {
        Row(Modifier.weight(1f).padding(Prism.dimens.spacingXs)) {
          Text("// ", style = Prism.typography.labelAlt, color = Prism.color.labelColor)
          val eventTransition = rememberMatchSharedText(
            matchId = matchPreview.id,
            content = MatchSharedContent.Event,
            color = Prism.color.labelColor,
            fontSize = Prism.typography.labelAlt.fontSize,
          )
          Text(
            text = matchPreview.event,
            modifier = eventTransition.modifier,
            style = Prism.typography.cardTitle.copy(fontSize = Prism.typography.labelAlt.fontSize),
            color = eventTransition.color,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
        }
        val time = formatMatchPreviewTime(isoUtcTime = matchPreview.time)
        when (matchPreview.status) {
          MatchStatus.LIVE -> PrismTag(
            text = stringResource(Res.string.match_event_live),
            style = PrismTagStyle.Accent,
          )

          MatchStatus.UPCOMING,
          MatchStatus.COMPLETED,
          -> time?.let { formattedTime ->
            PrismTag(
              text = formattedTime,
            )
          }

          MatchStatus.UNKNOWN -> Unit
        }
        liveMap?.let { map ->
          Text(
            text = map.name,
            modifier = Modifier
              .padding(start = Prism.dimens.spacingS + LiveMapRule.thickness)
              .width(liveMapColumnWidth(map.name)),
            style = Prism.typography.labelAlt,
            color = Prism.color.contentSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
          )
        }
      }
      ScoreBox(
        matchId = matchPreview.id,
        team1 = matchPreview.team1,
        team2 = matchPreview.team2,
        state = matchPreview.status,
        liveMap = liveMap,
      )
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = Prism.dimens.spacingS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      ) {
        Text(
          text = matchPreview.series,
          modifier = Modifier.weight(1f),
          style = Prism.typography.label,
          color = Prism.color.labelColor,
          textAlign = TextAlign.Start,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        AnimatedContent(
          targetState = isSharing,
          transitionSpec = {
            fadeIn(selectionAnimation.floatSpec()) togetherWith fadeOut(selectionAnimation.floatSpec())
          },
          contentAlignment = Alignment.CenterEnd,
          label = "match_footer_share_transition",
        ) { sharing ->
          if (sharing) {
            footerAction?.invoke()
          } else {
            MatchFavoriteReasons(reasons = matchPreview.favoriteReasons)
          }
        }
      }
    }
  }
}

@Composable
private fun ScoreBox(
  matchId: String,
  modifier: Modifier = Modifier,
  team1: TeamPreview,
  team2: TeamPreview,
  state: MatchStatus,
  liveMap: CurrentMatchMap?,
) {
  val spoilersHidden = LocalSpoilerMode.current.enabled
  val (actualTeam1, actualTeam2) = rememberTeamPreview(team1, team2, state, spoilersHidden)
  fun mapScore(team: TeamPreview, map: CurrentMatchMap) = if (team == team1) map.team1Score else map.team2Score
  Row(
    modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    Column(modifier = Modifier.weight(1f)) {
      TeamScoreRow(actualTeam1, matchId = matchId, useAltColor = !spoilersHidden && state != MatchStatus.UPCOMING)
      PrismDivider(
        style = PrismDividerStyle.Hairline,
      )
      TeamScoreRow(actualTeam2, matchId = matchId, useAltColor = !spoilersHidden && state == MatchStatus.LIVE)
    }
    liveMap?.let { map ->
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .width(LiveMapRule.thickness)
          .background(LiveMapRule.color),
      )
      Column(modifier = Modifier.width(liveMapColumnWidth(map.name))) {
        LiveMapScore(mapName = map.name, score = mapScore(actualTeam1, map))
        PrismDivider(style = PrismDividerStyle.Hairline)
        LiveMapScore(mapName = map.name, score = mapScore(actualTeam2, map))
      }
    }
  }
}

/** The rule that sets the live map column apart from the series score. */
private val LiveMapRule = PrismDividerStyle.Default

/** Fits the map name heading the column and a two-digit round score, so the column holds still mid-map. */
@Composable
private fun liveMapColumnWidth(mapName: String): Dp =
  maxOf(textWidth(mapName, Prism.typography.labelAlt), textWidth("00", Prism.typography.headline))

/** One team's round score on the current map, padded like [TeamScoreRow] so the two columns line up. */
@Composable
private fun LiveMapScore(mapName: String, score: Int?) {
  val description = score?.let {
    stringResource(Res.string.match_live_map_score_description, mapName, it.toString())
  }
  Text(
    text = score?.toString() ?: "–",
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = Prism.dimens.spacingXs)
      .then(description?.let { Modifier.semantics { contentDescription = it } } ?: Modifier),
    style = Prism.typography.headline,
    color = Prism.color.contentPrimary,
    textAlign = TextAlign.Center,
    maxLines = 1,
  )
}

@Composable
private fun textWidth(sample: String, style: TextStyle): Dp {
  val measurer = rememberTextMeasurer()
  val density = LocalDensity.current
  return remember(sample, style, measurer, density) {
    with(density) { measurer.measure(sample, style).size.width.toDp() }
  }
}

@Composable
private fun rememberTeamPreview(
  team1: TeamPreview,
  team2: TeamPreview,
  state: MatchStatus,
  spoilersHidden: Boolean,
): Pair<TeamPreview, TeamPreview> = remember(team1, team2, state, spoilersHidden) {
  when {
    spoilersHidden -> team1 to team2
    state == MatchStatus.UPCOMING || state == MatchStatus.LIVE -> team1 to team2
    else -> teamsByScore(team1 = team1, team2 = team2)
  }
}

private fun teamsByScore(team1: TeamPreview, team2: TeamPreview): Pair<TeamPreview, TeamPreview> =
  if (team2.scoreForOrdering() < team1.scoreForOrdering()) {
    team1 to team2
  } else {
    team2 to team1
  }

private fun TeamPreview.scoreForOrdering(): Int = score ?: Int.MIN_VALUE

@Composable
private fun TeamScoreRow(team: TeamPreview, matchId: String, useAltColor: Boolean, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = Prism.dimens.spacingXs),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val color = if (useAltColor) Prism.color.accent else Prism.color.labelColor
    val nameTransition = rememberMatchSharedText(matchId, MatchSharedContent.TeamName, team.id, color)
    Text(
      text = team.name,
      style = Prism.typography.headline,
      color = nameTransition.color,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f).wrapContentSize(Alignment.CenterStart).then(nameTransition.modifier),
    )
    SpoilerContent(modifier = Modifier.padding(start = Prism.dimens.spacingXs)) {
      val scoreTransition = rememberMatchSharedText(matchId, MatchSharedContent.TeamScore, team.id, color)
      Text(
        text = team.score?.toString() ?: "–",
        modifier = scoreTransition.modifier,
        style = Prism.typography.headline,
        color = scoreTransition.color,
        maxLines = 1,
      )
    }
  }
}
