/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.divider.PrismDividerStyle
import dev.staticvar.designsystem.component.sheet.PrismModalSheet
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MatchVeto
import dev.staticvar.vlr.domain.model.VetoAction
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.match_event_close
import vlr.shared_ui.generated.resources.match_event_map_veto
import vlr.shared_ui.generated.resources.match_event_map_veto_description
import vlr.shared_ui.generated.resources.match_event_veto_ban
import vlr.shared_ui.generated.resources.match_event_veto_pick
import vlr.shared_ui.generated.resources.match_event_veto_remains

/** Shows the map veto steps in the order supplied by the match source. */
@Composable
public fun MatchDetailVetoSheet(
  entries: List<MatchVeto>,
  visible: Boolean,
  onDismissRequest: () -> Unit,
  fallbackEntries: List<String> = emptyList(),
) {
  PrismModalSheet(
    visible = visible && (entries.isNotEmpty() || fallbackEntries.isNotEmpty()),
    onDismissRequest = onDismissRequest,
    paneTitle = stringResource(Res.string.match_event_map_veto),
    header = {
      Text(
        text = stringResource(Res.string.match_event_map_veto),
        style = Prism.typography.sectionTitle,
        color = Prism.color.titleColor,
      )
      Text(
        text = stringResource(Res.string.match_event_map_veto_description),
        modifier = Modifier.padding(top = Prism.dimens.spacingXs),
        style = Prism.typography.bodySmall,
        color = Prism.color.labelColor,
      )
    },
    footer = {
      PrismButton(onClick = onDismissRequest, style = PrismButtonStyle.Tertiary) {
        Text(stringResource(Res.string.match_event_close))
      }
    },
  ) {
    val numberStyle = Prism.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
    val stepCount = if (entries.isNotEmpty()) entries.size else fallbackEntries.size
    val numberWidth = stepNumberWidth(count = stepCount, style = numberStyle)
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      if (entries.isNotEmpty()) {
        entries.forEachIndexed { index, entry ->
          VetoStep(index = index, showDivider = index > 0, numberStyle = numberStyle, numberWidth = numberWidth) {
            MatchVetoContent(entry)
          }
        }
      } else {
        fallbackEntries.forEachIndexed { index, entry ->
          VetoStep(index = index, showDivider = index > 0, numberStyle = numberStyle, numberWidth = numberWidth) {
            Text(
              text = entry,
              modifier = Modifier.weight(1f),
              style = Prism.typography.bodyLarge,
              color = Prism.color.bodyColor,
            )
          }
        }
      }
    }
  }
}

/** Measures the last step's label, which is the widest because tabular figures give every digit one width. */
@Composable
private fun stepNumberWidth(count: Int, style: TextStyle): Dp {
  val measurer = rememberTextMeasurer()
  val density = LocalDensity.current
  return remember(count, style, measurer) {
    with(density) { measurer.measure(stepNumber(count.coerceAtLeast(1) - 1), style).size.width.toDp() }
  }
}

private fun stepNumber(index: Int): String = "${index + 1}."

@Composable
private fun VetoStep(
  index: Int,
  showDivider: Boolean,
  numberStyle: TextStyle,
  numberWidth: Dp,
  content: @Composable RowScope.() -> Unit,
) {
  if (showDivider) PrismDivider(style = PrismDividerStyle.Hairline)
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = Prism.dimens.spacingXs),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // One column width for every number, right-aligned, so "1." and "10." rows start their content at the same x.
    Text(
      text = stepNumber(index),
      modifier = Modifier.widthIn(min = numberWidth),
      style = numberStyle,
      textAlign = TextAlign.End,
      color = Prism.color.labelColor,
    )
    content()
  }
}

@Composable
private fun RowScope.MatchVetoContent(entry: MatchVeto) {
  if (entry.action == VetoAction.UNKNOWN) {
    Text(
      text = entry.map,
      modifier = Modifier.weight(1f),
      style = Prism.typography.bodyLarge,
      color = Prism.color.bodyColor,
    )
    return
  }

  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
    entry.team?.takeIf(String::isNotBlank)?.let { team ->
      Text(text = team, style = Prism.typography.label, color = Prism.color.labelColor)
    }
    Row(
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      PrismTag(text = stringResource(requireNotNull(entry.action.labelResource)), style = entry.action.tagStyle)
      Text(text = entry.map, style = Prism.typography.bodyLarge, color = Prism.color.bodyColor)
    }
  }
}

private val VetoAction.tagStyle: PrismTagStyle
  get() = when (this) {
    VetoAction.BAN -> PrismTagStyle.Danger
    VetoAction.PICK -> PrismTagStyle.Success
    VetoAction.REMAINS, VetoAction.UNKNOWN -> PrismTagStyle.Neutral
  }

private val VetoAction.labelResource: StringResource?
  get() = when (this) {
    VetoAction.BAN -> Res.string.match_event_veto_ban
    VetoAction.PICK -> Res.string.match_event_veto_pick
    VetoAction.REMAINS -> Res.string.match_event_veto_remains
    VetoAction.UNKNOWN -> null
  }
