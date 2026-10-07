/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.sp
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.RankingCircuit
import dev.staticvar.vlr.domain.model.RankingMetric
import dev.staticvar.vlr.domain.model.RankingOrder
import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.model.RankingsQuery
import org.jetbrains.compose.resources.stringResource
import vlr.feature_rankings.generated.resources.Res
import vlr.feature_rankings.generated.resources.rankings_circuit_all
import vlr.feature_rankings.generated.resources.rankings_circuit_collegiate
import vlr.feature_rankings.generated.resources.rankings_circuit_gc
import vlr.feature_rankings.generated.resources.rankings_circuit_offseason
import vlr.feature_rankings.generated.resources.rankings_circuit_other
import vlr.feature_rankings.generated.resources.rankings_circuit_tier3
import vlr.feature_rankings.generated.resources.rankings_circuit_vcl
import vlr.feature_rankings.generated.resources.rankings_circuit_vct
import vlr.feature_rankings.generated.resources.rankings_metric_elo
import vlr.feature_rankings.generated.resources.rankings_metric_map_elo
import vlr.feature_rankings.generated.resources.rankings_metric_matches
import vlr.feature_rankings.generated.resources.rankings_metric_win_rate
import vlr.feature_rankings.generated.resources.rankings_order_asc
import vlr.feature_rankings.generated.resources.rankings_order_desc
import vlr.feature_rankings.generated.resources.rankings_sentence
import vlr.feature_rankings.generated.resources.rankings_worldwide
import vlr.feature_rankings.generated.resources.region_americas
import vlr.feature_rankings.generated.resources.region_china
import vlr.feature_rankings.generated.resources.region_emea
import vlr.feature_rankings.generated.resources.region_pacific

internal enum class RankingSheet { Metric, Order, Circuit, Region }

@Composable
internal fun RankingsQuerySentence(query: RankingsQuery, onSelection: (RankingSheet) -> Unit) {
  val phrases = mapOf(
    "metric" to (RankingSheet.Metric to metricLabel(query.metric)),
    "order" to (RankingSheet.Order to orderLabel(query.order)),
    "circuit" to (RankingSheet.Circuit to circuitLabel(query.circuit)),
    "region" to (RankingSheet.Region to (query.region?.let { regionLabel(it) }
      ?: stringResource(Res.string.rankings_worldwide))),
  )
  val template = stringResource(Res.string.rankings_sentence)
  val links = TextLinkStyles(style = SpanStyle(color = Prism.color.accent, textDecoration = TextDecoration.Underline))
  val sentence = buildAnnotatedString {
    var start = 0
    Regex("\\{(metric|order|circuit|region)\\}").findAll(template).forEach { match ->
      append(template.substring(start, match.range.first))
      val (field, label) = phrases.getValue(match.groupValues[1])
      withLink(LinkAnnotation.Clickable(field.name, links) { onSelection(field) }) { append(label) }
      start = match.range.last + 1
    }
    append(template.substring(start))
  }
  Text(
    text = sentence,
    modifier = Modifier.testTag("rankings_query_sentence"),
    style = Prism.typography.sectionTitle.copy(fontSize = 24.sp, lineHeight = 36.sp),
    color = Prism.color.bodyColor,
  )
}

@Composable
internal fun metricLabel(metric: RankingMetric): String = stringResource(
  when (metric) {
    RankingMetric.Elo -> Res.string.rankings_metric_elo
    RankingMetric.MapElo -> Res.string.rankings_metric_map_elo
    RankingMetric.Matches -> Res.string.rankings_metric_matches
    RankingMetric.WinRate -> Res.string.rankings_metric_win_rate
  },
)

@Composable
internal fun orderLabel(order: RankingOrder): String = stringResource(
  if (order == RankingOrder.Desc) Res.string.rankings_order_desc else Res.string.rankings_order_asc,
)

@Composable
internal fun circuitLabel(circuit: RankingCircuit): String = stringResource(
  when (circuit) {
    RankingCircuit.All -> Res.string.rankings_circuit_all
    RankingCircuit.Vct -> Res.string.rankings_circuit_vct
    RankingCircuit.Vcl -> Res.string.rankings_circuit_vcl
    RankingCircuit.Tier3 -> Res.string.rankings_circuit_tier3
    RankingCircuit.GameChangers -> Res.string.rankings_circuit_gc
    RankingCircuit.Collegiate -> Res.string.rankings_circuit_collegiate
    RankingCircuit.Offseason -> Res.string.rankings_circuit_offseason
    RankingCircuit.Other -> Res.string.rankings_circuit_other
  },
)

@Composable
internal fun regionLabel(region: RankingRegion): String = stringResource(
  when (region) {
    RankingRegion.Americas -> Res.string.region_americas
    RankingRegion.Emea -> Res.string.region_emea
    RankingRegion.Pacific -> Res.string.region_pacific
    RankingRegion.China -> Res.string.region_china
  },
)
