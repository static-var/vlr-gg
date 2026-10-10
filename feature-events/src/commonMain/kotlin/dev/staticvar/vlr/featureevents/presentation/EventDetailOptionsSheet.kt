/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.button.PrismIconButton
import dev.staticvar.designsystem.component.button.PrismIconButtonSize
import dev.staticvar.designsystem.component.selection.PrismCheckbox
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
import vlr.feature_events.generated.resources.move_section_down
import vlr.feature_events.generated.resources.move_section_up
import vlr.feature_events.generated.resources.participants
import vlr.feature_events.generated.resources.participants_option_description
import vlr.feature_events.generated.resources.prizes
import vlr.feature_events.generated.resources.prizes_option_description
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
  val available = event.availableSections
  val sections = preferences.availableSectionsInOrder(event)
  PrismFabSheet(
    expanded = expanded,
    onExpandedChange = onExpandedChange,
    icon = EventOptionsIcon,
    contentDescription = stringResource(Res.string.event_options),
    sheetTitle = stringResource(Res.string.event_options),
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
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      sections.forEachIndexed { index, section ->
        key(section) {
          EventSectionOption(
            title = stringResource(section.title),
            description = stringResource(section.description),
            checked = preferences.isVisible(section),
            canMoveUp = index > 0,
            canMoveDown = index < sections.lastIndex,
            onCheckedChange = { onPreferencesChange(preferences.withVisibility(section, it)) },
            onMoveUp = { onPreferencesChange(preferences.movedUp(section, available)) },
            onMoveDown = { onPreferencesChange(preferences.movedDown(section, available)) },
          )
        }
      }
    }
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

@Composable
private fun EventSectionOption(
  title: String,
  description: String,
  checked: Boolean,
  canMoveUp: Boolean,
  canMoveDown: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
  ) {
    Row(
      modifier = Modifier.weight(1f).toggleable(
        value = checked,
        role = Role.Checkbox,
        onValueChange = onCheckedChange,
      ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      PrismCheckbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.clearAndSetSemantics {},
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = Prism.typography.bodyLarge, color = Prism.color.titleColor)
        Text(description, style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
      }
    }
    PrismIconButton(
      icon = MoveUpIcon,
      contentDescription = stringResource(Res.string.move_section_up, title),
      onClick = onMoveUp,
      size = PrismIconButtonSize.Small,
      enabled = canMoveUp,
    )
    PrismIconButton(
      icon = MoveDownIcon,
      contentDescription = stringResource(Res.string.move_section_down, title),
      onClick = onMoveDown,
      size = PrismIconButtonSize.Small,
      enabled = canMoveDown,
    )
  }
}

private fun chevronIcon(name: String, pointsUp: Boolean): ImageVector = ImageVector.Builder(
  name = name,
  defaultWidth = 24.dp,
  defaultHeight = 24.dp,
  viewportWidth = 24f,
  viewportHeight = 24f,
).apply {
  path(
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 2f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
  ) {
    val (edge, tip) = if (pointsUp) 15f to 9f else 9f to 15f
    moveTo(6f, edge)
    lineTo(12f, tip)
    lineTo(18f, edge)
  }
}.build()

private val MoveUpIcon: ImageVector = chevronIcon(name = "MoveSectionUp", pointsUp = true)
private val MoveDownIcon: ImageVector = chevronIcon(name = "MoveSectionDown", pointsUp = false)

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
