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
import dev.staticvar.vlr.featurematches.calendar.rememberMatchCalendarExporter
import dev.staticvar.vlr.featurematches.calendar.toCalendarEvent
import kotlin.time.Clock
import kotlin.time.Instant

internal fun MatchDetails.shouldShowCalendarAction(now: Instant = Clock.System.now()): Boolean =
  event.status.equals("upcoming", ignoreCase = true) &&
    toCalendarEvent()?.start?.let { it > now } == true

@Composable
internal fun MatchCalendarAction(match: MatchDetails) {
  val event = remember(match) { match.toCalendarEvent() }
  val export = rememberMatchCalendarExporter()
  var error by remember(match.id) { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
    PrismButton(
      modifier = Modifier.fillMaxWidth(),
      style = PrismButtonStyle.Primary,
      enabled = event != null,
      onClick = { event?.let { error = export(it).isFailure } },
    ) {
      Text("Add to calendar")
    }
    if (event == null || error) {
      Text(
        text = if (event == null) "Match time hasn't been announced." else "Unable to open the calendar file. Please try again.",
        style = Prism.typography.bodySmall,
        color = Prism.color.bodyColor,
      )
    }
  }
}
