/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.detail

import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventMatch

public enum class EventMatchGrouping(public val label: String) {
  Status(label = "Status"),
  Round(label = "Round"),
  Stage(label = "Stage"),
}

internal fun EventDetails.eventDetailMeta(): String = listOf(region, dates, prize)
  .filter(String::isNotBlank)
  .joinToString(separator = " • ")

internal fun EventMatch.eventMatchTitle(): String = teams.take(2)
  .map { team -> team.name }
  .filter(String::isNotBlank)
  .joinToString(separator = " vs ")
  .ifBlank { "Match TBD" }

internal fun EventMatch.eventMatchSchedule(): String = listOfNotNull(
  eta?.takeIf(String::isNotBlank),
  listOf(date, time).filter(String::isNotBlank).joinToString(separator = " ").ifBlank { null },
).joinToString(separator = " • ")

internal fun List<EventMatch>.groupEventMatches(grouping: EventMatchGrouping): Map<String, List<EventMatch>> =
  groupBy { match ->
    when (grouping) {
      EventMatchGrouping.Status -> match.status
      EventMatchGrouping.Round -> match.round
      EventMatchGrouping.Stage -> match.stage
    }.eventGroupLabel()
  }

internal fun String.eventHeroDateStat(): String = substringBefore(delimiter = "-")
  .trim()
  .ifBlank { this }

internal fun String.eventHeroPrizeStat(): String {
  val compactPrize = replace("$", "")
    .replace(",", "")
    .toDoubleOrNull()
    ?.takeIf { value -> value >= 1_000_000.0 }
    ?.let { value -> "$${(value / 1_000_000.0).toCompactDecimal()}M" }

  return compactPrize ?: this
}

private fun String.eventGroupLabel(): String = trim()
  .ifBlank { "Unknown" }
  .replaceFirstChar { char -> char.uppercase() }

private fun Double.toCompactDecimal(): String {
  val rounded = kotlin.math.round(this * 100.0) / 100.0
  return rounded.toString().trimEnd('0').trimEnd('.')
}
