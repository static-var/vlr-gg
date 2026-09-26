/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.CurrentMatchMap
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.match_event_live
import vlr.shared_ui.generated.resources.match_event_map
import vlr.shared_ui.generated.resources.match_event_map_number
import vlr.shared_ui.generated.resources.match_event_round_score
import vlr.shared_ui.generated.resources.match_event_up_next

@Composable
internal fun MatchTicketCurrentMap(map: CurrentMatchMap, modifier: Modifier = Modifier) {
  Column(modifier.fillMaxWidth().testTag("matchDetails:currentMap")) {
    HorizontalDivider(color = Prism.color.surfaceVariant)
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM),
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
        Text(
          text = map.number?.let { stringResource(Res.string.match_event_map_number, it) }
            ?: stringResource(Res.string.match_event_map),
          style = Prism.typography.overline,
          color = Prism.color.contentSecondary,
        )
        Text(
          text = map.name,
          style = Prism.typography.cardTitle,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        PrismTag(
          text = stringResource(if (map.isLive) Res.string.match_event_live else Res.string.match_event_up_next),
          style = if (map.isLive) PrismTagStyle.Danger else PrismTagStyle.Neutral,
        )
      }
      Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
        Text(
          text = "${map.team1Score?.toString() ?: "–"} : ${map.team2Score?.toString() ?: "–"}",
          style = Prism.typography.headline,
          maxLines = 1,
        )
        Text(
          text = stringResource(Res.string.match_event_round_score),
          style = Prism.typography.caption,
          color = Prism.color.contentSecondary,
        )
      }
    }
  }
}
