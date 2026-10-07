/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.sheet.PrismModalSheet
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.RankingCircuit
import dev.staticvar.vlr.domain.model.RankingMetric
import dev.staticvar.vlr.domain.model.RankingOrder
import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.model.RankingsQuery
import org.jetbrains.compose.resources.stringResource
import vlr.feature_rankings.generated.resources.Res
import vlr.feature_rankings.generated.resources.rankings_circuit_context
import vlr.feature_rankings.generated.resources.rankings_info_close
import vlr.feature_rankings.generated.resources.rankings_options
import vlr.feature_rankings.generated.resources.rankings_select_circuit
import vlr.feature_rankings.generated.resources.rankings_select_metric
import vlr.feature_rankings.generated.resources.rankings_select_order
import vlr.feature_rankings.generated.resources.rankings_select_region
import vlr.feature_rankings.generated.resources.rankings_worldwide

@Composable
internal fun RankingsSelectionSheet(
  selection: RankingSheet?,
  query: RankingsQuery,
  regional: Boolean,
  onDismiss: () -> Unit,
  onQueryChanged: (RankingsQuery) -> Unit,
  onExplore: () -> Unit,
  onInfo: () -> Unit,
  onCollapsed: () -> Unit,
  onRefresh: () -> Unit,
  refreshing: Boolean,
) {
  val title = stringResource(
    when (selection) {
      RankingSheet.Metric -> Res.string.rankings_select_metric
      RankingSheet.Order -> Res.string.rankings_select_order
      RankingSheet.Circuit -> Res.string.rankings_select_circuit
      RankingSheet.Region -> Res.string.rankings_select_region
      else -> Res.string.rankings_options
    },
  )
  fun select(updated: RankingsQuery) {
    onQueryChanged(updated)
    onDismiss()
  }
  PrismModalSheet(
    visible = selection != null,
    onDismissRequest = onDismiss,
    paneTitle = title,
    onCollapsed = onCollapsed,
    header = {
      Text(title, Modifier.semantics { heading() }, style = Prism.typography.sectionTitle, color = Prism.color.titleColor)
    },
    footer = if (selection == RankingSheet.Options) null else {
      {
        PrismButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), style = PrismButtonStyle.Secondary) {
          Text(stringResource(Res.string.rankings_info_close))
        }
      }
    },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      when (selection) {
        RankingSheet.Metric -> RankingMetric.entries.forEach { metric ->
          RankingChoice(metricLabel(metric), query.metric == metric) { select(query.copy(metric = metric)) }
        }
        RankingSheet.Order -> RankingOrder.entries.forEach { order ->
          RankingChoice(orderLabel(order), query.order == order) { select(query.copy(order = order)) }
        }
        RankingSheet.Circuit -> {
          Text(
            stringResource(Res.string.rankings_circuit_context),
            style = Prism.typography.bodyLarge,
            color = Prism.color.bodyColor,
          )
          RankingCircuit.entries.forEach { circuit ->
            RankingChoice(circuitLabel(circuit), query.circuit == circuit) { select(query.copy(circuit = circuit)) }
          }
        }
        RankingSheet.Region -> {
          RankingChoice(stringResource(Res.string.rankings_worldwide), query.region == null) {
            select(query.copy(region = null))
          }
          RankingRegion.entries.forEach { region ->
            RankingChoice(regionLabel(region), query.region == region) { select(query.copy(region = region)) }
          }
        }
        RankingSheet.Options -> RankingsOptions(
          query = query,
          regional = regional,
          onApply = ::select,
          onExplore = { onExplore(); onDismiss() },
          onInfo = { onDismiss(); onInfo() },
          onRefresh = { onRefresh(); onDismiss() },
          refreshing = refreshing,
        )
        null -> Unit
      }
    }
  }
}

@Composable
private fun RankingChoice(label: String, isSelected: Boolean, onClick: () -> Unit) {
  PrismButton(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().semantics { selected = isSelected },
    style = if (isSelected) PrismButtonStyle.Alternate else PrismButtonStyle.Secondary,
  ) {
    Text(label)
  }
}
