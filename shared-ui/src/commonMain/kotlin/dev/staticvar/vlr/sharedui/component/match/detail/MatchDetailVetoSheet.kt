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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.divider.PrismDividerStyle
import dev.staticvar.designsystem.component.sheet.PrismModalSheet
import dev.staticvar.designsystem.prism.Prism
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.match_event_close
import vlr.shared_ui.generated.resources.match_event_map_veto
import vlr.shared_ui.generated.resources.match_event_map_veto_description

/** Shows the map veto steps in the order supplied by the match source. */
@Composable
public fun MatchDetailVetoSheet(entries: List<String>, visible: Boolean, onDismissRequest: () -> Unit) {
  PrismModalSheet(
    visible = visible && entries.isNotEmpty(),
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
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      entries.forEachIndexed { index, entry ->
        if (index > 0) PrismDivider(style = PrismDividerStyle.Hairline)
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = Prism.dimens.spacingXs),
          horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
        ) {
          Text(
            text = "${index + 1}.",
            style = Prism.typography.bodyLarge,
            color = Prism.color.labelColor,
          )
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
