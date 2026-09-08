/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.selection.PrismCheckbox
import dev.staticvar.designsystem.component.sheet.PrismFabSheet
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.settings.MatchDetailsPreferences
import dev.staticvar.vlr.domain.model.MatchDetails

@Composable
internal fun MatchDetailOptionsSheet(
  match: MatchDetails,
  preferences: MatchDetailsPreferences,
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  onPreferencesChange: (MatchDetailsPreferences) -> Unit,
) {
  PrismFabSheet(
    expanded = expanded,
    onExpandedChange = onExpandedChange,
    icon = MatchOptionsIcon,
    contentDescription = "Match options",
    sheetTitle = "Match options",
    header = {
      Text("Match options", style = Prism.typography.sectionTitle, color = Prism.color.titleColor)
      Text("Choose what you see in match details.", style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
    },
    footer = {
      PrismButton(onClick = { onExpandedChange(false) }, style = PrismButtonStyle.Secondary) {
        Text("Done")
      }
    },
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      if (match.matchData.isNotEmpty()) {
        MatchSectionOption(
          title = "Breakdown",
          description = "Match stats and player table",
          checked = preferences.showBreakdown,
          onCheckedChange = { onPreferencesChange(preferences.copy(showBreakdown = it)) },
        )
      }
      if (match.videos.streams.isNotEmpty() || match.videos.vods.isNotEmpty()) {
        MatchSectionOption(
          title = "Media",
          description = "Streams and VODs",
          checked = preferences.showMedia,
          onCheckedChange = { onPreferencesChange(preferences.copy(showMedia = it)) },
        )
      }
      if (match.head2head.isNotEmpty()) {
        MatchSectionOption(
          title = "Head to head",
          description = "Previous meetings",
          checked = preferences.showHeadToHead,
          onCheckedChange = { onPreferencesChange(preferences.copy(showHeadToHead = it)) },
        )
      }
    }
  }
}

@Composable
private fun MatchSectionOption(
  title: String,
  description: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().toggleable(
      value = checked,
      role = Role.Checkbox,
      onValueChange = onCheckedChange,
    ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = Prism.typography.bodyLarge, color = Prism.color.titleColor)
      Text(description, style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
    }
    PrismCheckbox(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.clearAndSetSemantics {},
    )
  }
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
