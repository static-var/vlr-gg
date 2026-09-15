/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.vlr.domain.model.MatchFavoriteReason
import dev.staticvar.vlr.domain.model.MatchFavoriteSource
import dev.staticvar.vlr.sharedui.component.common.FavoriteReasonTags
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.format_reason_event
import vlr.shared_ui.generated.resources.format_reason_match
import vlr.shared_ui.generated.resources.format_reason_player
import vlr.shared_ui.generated.resources.format_reason_team

@Composable
public fun MatchFavoriteReasons(reasons: List<MatchFavoriteReason>, modifier: Modifier = Modifier) {
  FavoriteReasonTags(labels = matchFavoriteReasonLabels(reasons), modifier = modifier)
}

@Composable
internal fun matchFavoriteReasonLabels(reasons: List<MatchFavoriteReason>): List<String> = matchFavoriteReasonLabels(
  reasons,
  listOf(
    stringResource(Res.string.format_reason_match),
    stringResource(Res.string.format_reason_team),
    stringResource(Res.string.format_reason_player),
    stringResource(Res.string.format_reason_event),
  ),
)

internal fun matchFavoriteReasonLabels(reasons: List<MatchFavoriteReason>, sourceLabels: List<String>): List<String> =
  reasons
    .map(MatchFavoriteReason::source)
    .distinct()
    .sortedBy(MatchFavoriteSource::ordinal)
    .map { source ->
      when (source) {
        MatchFavoriteSource.MATCH -> sourceLabels[0]
        MatchFavoriteSource.TEAM -> sourceLabels[1]
        MatchFavoriteSource.PLAYER -> sourceLabels[2]
        MatchFavoriteSource.EVENT -> sourceLabels[3]
      }
    }
