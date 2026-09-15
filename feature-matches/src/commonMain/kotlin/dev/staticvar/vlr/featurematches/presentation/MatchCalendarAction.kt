/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.featurematches.calendar.calendarStart
import dev.staticvar.vlr.featurematches.calendar.rememberMatchCalendarExporter
import dev.staticvar.vlr.featurematches.calendar.toCalendarEvent
import kotlin.time.Clock
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource
import vlr.feature_matches.generated.resources.Res
import vlr.feature_matches.generated.resources.add_to_calendar
import vlr.feature_matches.generated.resources.calendar_versus
import vlr.feature_matches.generated.resources.match_time_hasn_t_been_announced
import vlr.feature_matches.generated.resources.unable_to_open_the_calendar_file_please_try_again

internal fun MatchDetails.shouldShowCalendarAction(now: Instant = Clock.System.now()): Boolean =
  event.status.equals("upcoming", ignoreCase = true) &&
    calendarStart()?.let { it > now } == true

@Composable
internal fun MatchCalendarAction(match: MatchDetails) {
  val versus = stringResource(Res.string.calendar_versus)
  val event = remember(match, versus) { match.toCalendarEvent(versus) }
  val export = rememberMatchCalendarExporter()
  var error by remember(match.id) { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
    PrismButton(
      modifier = Modifier.fillMaxWidth(),
      style = PrismButtonStyle.Primary,
      enabled = event != null,
      onClick = { event?.let { error = export(it).isFailure } },
    ) {
      Text(stringResource(Res.string.add_to_calendar))
    }
    if (event == null || error) {
      Text(
        text = if (event == null) stringResource(Res.string.match_time_hasn_t_been_announced) else stringResource(Res.string.unable_to_open_the_calendar_file_please_try_again),
        style = Prism.typography.bodySmall,
        color = Prism.color.bodyColor,
      )
    }
  }
}
