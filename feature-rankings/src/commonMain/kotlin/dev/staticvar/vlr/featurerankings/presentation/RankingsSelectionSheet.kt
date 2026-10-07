/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import vlr.feature_rankings.generated.resources.Res
import vlr.feature_rankings.generated.resources.rankings_circuit_all_description
import vlr.feature_rankings.generated.resources.rankings_circuit_collegiate_description
import vlr.feature_rankings.generated.resources.rankings_circuit_context
import vlr.feature_rankings.generated.resources.rankings_circuit_gc_description
import vlr.feature_rankings.generated.resources.rankings_circuit_offseason_description
import vlr.feature_rankings.generated.resources.rankings_circuit_other_description
import vlr.feature_rankings.generated.resources.rankings_circuit_tier3_description
import vlr.feature_rankings.generated.resources.rankings_circuit_vcl_description
import vlr.feature_rankings.generated.resources.rankings_circuit_vct_description
import vlr.feature_rankings.generated.resources.rankings_info_close
import vlr.feature_rankings.generated.resources.rankings_metric_context
import vlr.feature_rankings.generated.resources.rankings_metric_elo_description
import vlr.feature_rankings.generated.resources.rankings_metric_map_elo_description
import vlr.feature_rankings.generated.resources.rankings_metric_matches_description
import vlr.feature_rankings.generated.resources.rankings_metric_win_rate_description
import vlr.feature_rankings.generated.resources.rankings_options
import vlr.feature_rankings.generated.resources.rankings_options_context
import vlr.feature_rankings.generated.resources.rankings_order_asc_description
import vlr.feature_rankings.generated.resources.rankings_order_context
import vlr.feature_rankings.generated.resources.rankings_order_desc_description
import vlr.feature_rankings.generated.resources.rankings_region_all_description
import vlr.feature_rankings.generated.resources.rankings_region_americas_description
import vlr.feature_rankings.generated.resources.rankings_region_china_description
import vlr.feature_rankings.generated.resources.rankings_region_context
import vlr.feature_rankings.generated.resources.rankings_region_emea_description
import vlr.feature_rankings.generated.resources.rankings_region_pacific_description
import vlr.feature_rankings.generated.resources.rankings_regional_rules
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
  val description = stringResource(
    when (selection) {
      RankingSheet.Metric -> Res.string.rankings_metric_context
      RankingSheet.Order -> Res.string.rankings_order_context
      RankingSheet.Circuit -> Res.string.rankings_circuit_context
      RankingSheet.Region -> Res.string.rankings_region_context
      else -> if (regional) Res.string.rankings_regional_rules else Res.string.rankings_options_context
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
      Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        Text(title, Modifier.semantics { heading() }, style = Prism.typography.sectionTitle, color = Prism.color.titleColor)
        Text(description, style = Prism.typography.bodySmall, color = Prism.color.labelColor)
      }
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
          RankingChoice(metricLabel(metric), metricDescription(metric), query.metric == metric) { select(query.copy(metric = metric)) }
        }
        RankingSheet.Order -> RankingOrder.entries.forEach { order ->
          RankingChoice(
            orderLabel(order),
            if (order == RankingOrder.Desc) Res.string.rankings_order_desc_description else Res.string.rankings_order_asc_description,
            query.order == order,
          ) { select(query.copy(order = order)) }
        }
        RankingSheet.Circuit -> {
          RankingCircuit.entries.forEach { circuit ->
            RankingChoice(circuitLabel(circuit), circuitDescription(circuit), query.circuit == circuit) { select(query.copy(circuit = circuit)) }
          }
        }
        RankingSheet.Region -> {
          RankingChoice(stringResource(Res.string.rankings_worldwide), Res.string.rankings_region_all_description, query.region == null) {
            select(query.copy(region = null))
          }
          RankingRegion.entries.forEach { region ->
            RankingChoice(regionLabel(region), regionDescription(region), query.region == region) { select(query.copy(region = region)) }
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
private fun RankingChoice(label: String, description: StringResource, isSelected: Boolean, onClick: () -> Unit) {
  PrismButton(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().semantics { selected = isSelected },
    style = if (isSelected) PrismButtonStyle.Alternate else PrismButtonStyle.Secondary,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(vertical = Prism.dimens.spacingXs),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
    ) {
      Text(
        label.replaceFirstChar { it.titlecase() },
        style = Prism.typography.button,
        color = if (isSelected) Prism.color.accent else Prism.color.titleColor,
      )
      Text(stringResource(description), style = Prism.typography.label, color = Prism.color.labelColor)
    }
  }
}

private fun metricDescription(metric: RankingMetric): StringResource = when (metric) {
  RankingMetric.Elo -> Res.string.rankings_metric_elo_description
  RankingMetric.MapElo -> Res.string.rankings_metric_map_elo_description
  RankingMetric.Matches -> Res.string.rankings_metric_matches_description
  RankingMetric.WinRate -> Res.string.rankings_metric_win_rate_description
}

private fun circuitDescription(circuit: RankingCircuit): StringResource = when (circuit) {
  RankingCircuit.All -> Res.string.rankings_circuit_all_description
  RankingCircuit.Vct -> Res.string.rankings_circuit_vct_description
  RankingCircuit.Vcl -> Res.string.rankings_circuit_vcl_description
  RankingCircuit.Tier3 -> Res.string.rankings_circuit_tier3_description
  RankingCircuit.GameChangers -> Res.string.rankings_circuit_gc_description
  RankingCircuit.Collegiate -> Res.string.rankings_circuit_collegiate_description
  RankingCircuit.Offseason -> Res.string.rankings_circuit_offseason_description
  RankingCircuit.Other -> Res.string.rankings_circuit_other_description
}

private fun regionDescription(region: RankingRegion): StringResource = when (region) {
  RankingRegion.Americas -> Res.string.rankings_region_americas_description
  RankingRegion.Emea -> Res.string.rankings_region_emea_description
  RankingRegion.Pacific -> Res.string.rankings_region_pacific_description
  RankingRegion.China -> Res.string.rankings_region_china_description
}
