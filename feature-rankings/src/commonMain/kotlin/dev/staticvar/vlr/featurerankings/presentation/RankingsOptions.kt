/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.selection.PrismSwitch
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.RankingsQuery
import org.jetbrains.compose.resources.stringResource
import vlr.feature_rankings.generated.resources.Res
import vlr.feature_rankings.generated.resources.rankings_active_context
import vlr.feature_rankings.generated.resources.rankings_apply
import vlr.feature_rankings.generated.resources.rankings_include_inactive
import vlr.feature_rankings.generated.resources.rankings_info_title
import vlr.feature_rankings.generated.resources.rankings_minimum_context
import vlr.feature_rankings.generated.resources.rankings_minimum_error
import vlr.feature_rankings.generated.resources.rankings_minimum_series
import vlr.feature_rankings.generated.resources.rankings_open_explore
import vlr.feature_rankings.generated.resources.rankings_refresh
import vlr.feature_rankings.generated.resources.rankings_reset

@Composable
internal fun RankingsOptions(
  query: RankingsQuery,
  regional: Boolean,
  onApply: (RankingsQuery) -> Unit,
  onExplore: () -> Unit,
  onInfo: () -> Unit,
  onRefresh: () -> Unit,
  refreshing: Boolean,
) {
  var minimum by rememberSaveable(query) { mutableStateOf(query.minMatches.toString()) }
  var includeInactive by rememberSaveable(query) { mutableStateOf(query.includeInactive) }
  val minimumValue = minimum.toIntOrNull()?.takeIf { it in 0..1000 }
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM)) {
    if (regional) {
      PrismButton(onClick = onExplore, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.rankings_open_explore))
      }
    } else {
      val minimumLabel = stringResource(Res.string.rankings_minimum_series)
      Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
          Text(minimumLabel, style = Prism.typography.cardTitle, color = Prism.color.titleColor)
          Text(
            stringResource(Res.string.rankings_minimum_context),
            style = Prism.typography.bodySmall,
            color = Prism.color.labelColor,
          )
        }
        PrismSurface(
          color = Prism.color.surface,
          border = BorderStroke(Prism.dimens.strokeDefault, Prism.color.stroke),
          shape = Prism.shapes.small,
        ) {
          BasicTextField(
            value = minimum,
            onValueChange = { value -> if (value.length <= 4 && value.all(Char::isDigit)) minimum = value },
            modifier = Modifier.fillMaxWidth().padding(Prism.dimens.spacingM)
              .semantics { contentDescription = minimumLabel },
            textStyle = Prism.typography.bodyLarge.copy(color = Prism.color.titleColor),
            cursorBrush = SolidColor(Prism.color.accent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          )
        }
        if (minimumValue == null) {
          Text(
            stringResource(Res.string.rankings_minimum_error),
            style = Prism.typography.bodySmall,
            color = Prism.color.labelColor,
          )
        }
      }
      val inactiveLabel = stringResource(Res.string.rankings_include_inactive)
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
          Text(inactiveLabel, style = Prism.typography.cardTitle, color = Prism.color.titleColor)
          Text(
            stringResource(Res.string.rankings_active_context),
            style = Prism.typography.bodySmall,
            color = Prism.color.labelColor,
          )
        }
        PrismSwitch(includeInactive, { includeInactive = it }, Modifier.semantics { contentDescription = inactiveLabel })
      }
      Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        PrismButton(
          onClick = { onApply(RankingsQuery()) },
          modifier = Modifier.weight(1f),
          style = PrismButtonStyle.Secondary,
        ) { Text(stringResource(Res.string.rankings_reset)) }
        PrismButton(
          onClick = { minimumValue?.let { onApply(query.copy(minMatches = it, includeInactive = includeInactive)) } },
          enabled = minimumValue != null,
          modifier = Modifier.weight(1f),
        ) { Text(stringResource(Res.string.rankings_apply)) }
      }
    }
    PrismButton(onClick = onRefresh, enabled = !refreshing, style = PrismButtonStyle.Secondary, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(Res.string.rankings_refresh))
    }
    PrismButton(onClick = onInfo, style = PrismButtonStyle.Secondary, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(Res.string.rankings_info_title))
    }
  }
}
