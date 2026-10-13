/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.reorder.PrismReorderableOption
import dev.staticvar.designsystem.component.reorder.PrismReorderableOptionList
import dev.staticvar.designsystem.component.sheet.PrismFabSheet
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.settings.EventDetailsPreferences
import dev.staticvar.vlr.core.settings.EventDetailsSection
import dev.staticvar.vlr.domain.model.EventDetails
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import vlr.feature_events.generated.resources.Res
import vlr.feature_events.generated.resources.choose_and_order_event_sections
import vlr.feature_events.generated.resources.done
import vlr.feature_events.generated.resources.event_options
import vlr.feature_events.generated.resources.matches_option_description
import vlr.feature_events.generated.resources.participants
import vlr.feature_events.generated.resources.participants_option_description
import vlr.feature_events.generated.resources.prizes
import vlr.feature_events.generated.resources.prizes_option_description
import vlr.feature_events.generated.resources.section_unavailable
import vlr.feature_events.generated.resources.standings
import vlr.feature_events.generated.resources.standings_option_description
import vlr.feature_events.generated.resources.tab_matches

@Composable
internal fun EventDetailOptionsSheet(
  event: EventDetails,
  preferences: EventDetailsPreferences,
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  onPreferencesChange: (EventDetailsPreferences) -> Unit,
) {
  val scrollState = rememberScrollState()
  LaunchedEffect(expanded) { if (expanded) scrollState.scrollTo(0) }
  PrismFabSheet(
    expanded = expanded,
    onExpandedChange = onExpandedChange,
    icon = EventOptionsIcon,
    contentDescription = stringResource(Res.string.event_options),
    sheetTitle = stringResource(Res.string.event_options),
    scrollState = scrollState,
    header = {
      Text(
        stringResource(Res.string.event_options),
        style = Prism.typography.sectionTitle,
        color = Prism.color.titleColor,
      )
      Text(
        stringResource(Res.string.choose_and_order_event_sections),
        style = Prism.typography.bodySmall,
        color = Prism.color.bodyColor,
      )
    },
    footer = {
      PrismButton(onClick = { onExpandedChange(false) }, style = PrismButtonStyle.Secondary) {
        Text(stringResource(Res.string.done))
      }
    },
  ) {
    val sectionsWithData = event.sectionsWithData
    PrismReorderableOptionList(
      options = preferences.orderedSections.map { section ->
        val hasData = section in sectionsWithData
        PrismReorderableOption(
          key = section,
          title = stringResource(section.title),
          description = stringResource(if (hasData) section.description else Res.string.section_unavailable),
          checked = preferences.isVisible(section),
          toggleEnabled = hasData,
        )
      },
      onCheckedChange = { section, visible -> onPreferencesChange(preferences.withVisibility(section, visible)) },
      onMove = { section, index -> onPreferencesChange(preferences.moved(section, index)) },
      scrollState = scrollState,
    )
  }
}

private val EventDetailsSection.title: StringResource
  get() = when (this) {
    EventDetailsSection.Participants -> Res.string.participants
    EventDetailsSection.Prizes -> Res.string.prizes
    EventDetailsSection.Standings -> Res.string.standings
    EventDetailsSection.Matches -> Res.string.tab_matches
  }

private val EventDetailsSection.description: StringResource
  get() = when (this) {
    EventDetailsSection.Participants -> Res.string.participants_option_description
    EventDetailsSection.Prizes -> Res.string.prizes_option_description
    EventDetailsSection.Standings -> Res.string.standings_option_description
    EventDetailsSection.Matches -> Res.string.matches_option_description
  }

private val EventOptionsIcon: ImageVector = ImageVector.Builder(
  name = "EventOptions",
  defaultWidth = 24.dp,
  defaultHeight = 24.dp,
  viewportWidth = 24f,
  viewportHeight = 24f,
).apply {
  path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
    moveTo(4f, 7f)
    lineTo(8f, 7f)
    moveTo(12f, 7f)
    lineTo(20f, 7f)
    moveTo(4f, 17f)
    lineTo(12f, 17f)
    moveTo(16f, 17f)
    lineTo(20f, 17f)
    moveTo(10f, 4f)
    lineTo(10f, 10f)
    moveTo(14f, 14f)
    lineTo(14f, 20f)
  }
}.build()
