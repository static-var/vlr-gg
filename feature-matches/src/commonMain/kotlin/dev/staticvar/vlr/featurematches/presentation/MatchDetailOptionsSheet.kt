/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

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
import dev.staticvar.vlr.core.settings.MatchDetailsPreferences
import dev.staticvar.vlr.core.settings.MatchDetailsSection
import dev.staticvar.vlr.domain.model.MatchDetails
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import vlr.feature_matches.generated.resources.Res
import vlr.feature_matches.generated.resources.breakdown
import vlr.feature_matches.generated.resources.choose_and_order_match_sections
import vlr.feature_matches.generated.resources.done
import vlr.feature_matches.generated.resources.head_to_head
import vlr.feature_matches.generated.resources.match_options
import vlr.feature_matches.generated.resources.match_stats_and_player_table
import vlr.feature_matches.generated.resources.media
import vlr.feature_matches.generated.resources.insights
import vlr.feature_matches.generated.resources.insights_option_description
import vlr.feature_matches.generated.resources.previous_meetings
import vlr.feature_matches.generated.resources.section_unavailable
import vlr.feature_matches.generated.resources.streams_and_vods

@Composable
internal fun MatchDetailOptionsSheet(
  match: MatchDetails,
  preferences: MatchDetailsPreferences,
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  onPreferencesChange: (MatchDetailsPreferences) -> Unit,
) {
  val scrollState = rememberScrollState()
  LaunchedEffect(expanded) { if (expanded) scrollState.scrollTo(0) }
  PrismFabSheet(
    expanded = expanded,
    onExpandedChange = onExpandedChange,
    icon = MatchOptionsIcon,
    contentDescription = stringResource(Res.string.match_options),
    sheetTitle = stringResource(Res.string.match_options),
    scrollState = scrollState,
    header = {
      Text(
        stringResource(Res.string.match_options),
        style = Prism.typography.sectionTitle,
        color = Prism.color.titleColor,
      )
      Text(
        stringResource(Res.string.choose_and_order_match_sections),
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
    val available = match.availableSections
    PrismReorderableOptionList(
      options = preferences.orderedSections.map { section ->
        val isAvailable = section in available
        PrismReorderableOption(
          key = section,
          title = stringResource(section.title),
          description = stringResource(if (isAvailable) section.description else Res.string.section_unavailable),
          checked = preferences.isVisible(section),
          toggleEnabled = isAvailable,
        )
      },
      onCheckedChange = { section, visible -> onPreferencesChange(preferences.withVisibility(section, visible)) },
      onMove = { section, index -> onPreferencesChange(preferences.moved(section, index)) },
      scrollState = scrollState,
    )
  }
}

private val MatchDetailsSection.title: StringResource
  get() = when (this) {
    MatchDetailsSection.Insights -> Res.string.insights
    MatchDetailsSection.Breakdown -> Res.string.breakdown
    MatchDetailsSection.HeadToHead -> Res.string.head_to_head
    MatchDetailsSection.Media -> Res.string.media
  }

private val MatchDetailsSection.description: StringResource
  get() = when (this) {
    MatchDetailsSection.Insights -> Res.string.insights_option_description
    MatchDetailsSection.Breakdown -> Res.string.match_stats_and_player_table
    MatchDetailsSection.HeadToHead -> Res.string.previous_meetings
    MatchDetailsSection.Media -> Res.string.streams_and_vods
  }

private val MatchOptionsIcon: ImageVector = ImageVector.Builder(
  name = "MatchOptions",
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
