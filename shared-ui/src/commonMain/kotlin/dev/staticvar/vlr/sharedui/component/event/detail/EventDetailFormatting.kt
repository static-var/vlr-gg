/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.detail

import androidx.compose.runtime.Composable
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventMatch
import kotlin.math.round
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.format_groupRound
import vlr.shared_ui.generated.resources.format_groupStage
import vlr.shared_ui.generated.resources.format_groupStatus
import vlr.shared_ui.generated.resources.format_matchTbd
import vlr.shared_ui.generated.resources.format_region
import vlr.shared_ui.generated.resources.format_tbd
import vlr.shared_ui.generated.resources.format_teams
import vlr.shared_ui.generated.resources.format_unknown
import vlr.shared_ui.generated.resources.format_versus

public enum class EventMatchGrouping {
  Status,
  Round,
  Stage,
  ;

  public val label: String
    @Composable
    get() = when (this) {
      Status -> stringResource(Res.string.format_groupStatus)
      Round -> stringResource(Res.string.format_groupRound)
      Stage -> stringResource(Res.string.format_groupStage)
    }
}

internal fun EventDetails.eventDetailMeta(): String = listOf(region, dates, prize)
  .filter(String::isNotBlank)
  .joinToString(separator = " • ")

internal fun EventMatch.eventMatchTitle(labels: EventFormattingLabels): String = teams.take(2)
  .map { team -> team.name }
  .filter(String::isNotBlank)
  .joinToString(separator = labels.versus)
  .ifBlank { labels.matchTbd }

internal fun EventMatch.eventMatchSchedule(): String = listOfNotNull(
  eta?.takeIf(String::isNotBlank),
  listOf(date, time).filter(String::isNotBlank).joinToString(separator = " ").ifBlank { null },
).joinToString(separator = " • ")

public fun List<EventMatch>.groupEventMatches(
  grouping: EventMatchGrouping,
  labels: EventFormattingLabels,
): Map<String, List<EventMatch>> = groupBy { match ->
  when (grouping) {
    EventMatchGrouping.Status -> match.status
    EventMatchGrouping.Round -> match.round
    EventMatchGrouping.Stage -> match.stage
  }.eventGroupLabel(labels)
}

internal fun String.eventHeroDateStat(): String = substringBefore(delimiter = "-")
  .trim()
  .ifBlank { this }

internal fun EventDetails.eventHeroTeamsStat(labels: EventFormattingLabels): String = when (teams.size) {
  0 -> labels.tbd
  else -> labels.teams
}

internal fun String.eventHeroPrizeStat(): String {
  val primaryPrize = primaryCurrencyValue()
  val compactPrize = primaryPrize
    .replace("$", "")
    .replace(",", "")
    .toDoubleOrNull()
    ?.takeIf { value -> value >= 1_000_000.0 }
    ?.let { value -> "$${(value / 1_000_000.0).toCompactDecimal()}M" }

  return compactPrize ?: primaryPrize
}

internal fun String.eventHeroRegionStat(labels: EventFormattingLabels): String = trim().ifBlank {
  labels.region
}.uppercase()

private fun String.primaryCurrencyValue(): String = trim()
  .substringBefore("/")
  .substringBefore("(")
  .substringBefore("•")
  .trim()

private fun String.eventGroupLabel(labels: EventFormattingLabels): String = trim()
  .ifBlank { labels.unknown }
  .replaceFirstChar { char -> char.uppercase() }

private fun Double.toCompactDecimal(): String {
  val rounded = round(this * 100.0) / 100.0
  return rounded.toString().trimEnd('0').trimEnd('.')
}

public data class EventFormattingLabels(
  public val versus: String,
  public val matchTbd: String,
  public val tbd: String,
  public val teams: String,
  public val region: String,
  public val unknown: String,
)

@Composable
public fun eventFormattingLabels(teamCount: Int = 0): EventFormattingLabels = EventFormattingLabels(
  versus = stringResource(Res.string.format_versus),
  matchTbd = stringResource(Res.string.format_matchTbd),
  tbd = stringResource(Res.string.format_tbd),
  teams = pluralStringResource(Res.plurals.format_teams, teamCount, teamCount),
  region = stringResource(Res.string.format_region),
  unknown = stringResource(Res.string.format_unknown),
)
