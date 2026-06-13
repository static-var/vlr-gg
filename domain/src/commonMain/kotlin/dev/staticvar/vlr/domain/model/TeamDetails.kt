/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Domain model for detailed team information.
 */
data class TeamInfo(
  val id: String,
  val name: String,
  val tag: String,
  val logoUrl: String,
  val region: String,
  val country: String,
  val rank: Int,
  val website: String?,
  val twitter: String?,
  val roster: List<TeamPlayer>,
  val upcomingMatches: List<TeamUpcomingMatch>,
  val completedMatches: List<TeamCompletedMatch>,
  val isFavorite: Boolean = false,
) {
  fun matchesInNext7Days(today: LocalDate = currentLocalDate()): List<TeamUpcomingMatch> =
    upcomingMatches.filter { match ->
      val matchDate = match.date.toUpcomingMatchDate(today = today) ?: return@filter false
      today.daysUntil(matchDate) in 0..7
    }
}

/**
 * Player in a team roster.
 */
data class TeamPlayer(
  val id: String,
  val name: String,
  val alias: String,
  val role: String?,
  val imageUrl: String,
  val country: String,
  val isStandIn: Boolean,
  val isCoach: Boolean,
  val isCurrent: Boolean,
)

/**
 * Upcoming match for a team.
 */
data class TeamUpcomingMatch(
  val matchId: String,
  val eventName: String,
  val eventLogoUrl: String,
  val eventId: String?,
  val stage: String,
  val opponent: String,
  val opponentLogoUrl: String,
  val date: String,
  val eta: String?,
)

/**
 * Completed match for a team.
 */
data class TeamCompletedMatch(
  val matchId: String,
  val eventName: String,
  val eventLogoUrl: String,
  val eventId: String?,
  val stage: String,
  val opponent: String,
  val opponentLogoUrl: String,
  val date: String,
  val result: String,
)

private fun currentLocalDate(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun String.toUpcomingMatchDate(today: LocalDate): LocalDate? {
  val normalized = trim()
  if (normalized.isBlank()) return null

  LocalDate.parseOrNull(normalized)?.let { return it }

  val parts = normalized.split(Regex("\\s+"))
  if (parts.size < 2) return null

  val month = parts[0].toMonthOrNull() ?: return null
  val day = parts[1].trimEnd(',').toIntOrNull() ?: return null
  val candidate = runCatching { LocalDate(year = today.year, month = month, dayOfMonth = day) }.getOrNull() ?: return null

  return if (candidate < today) {
    runCatching { LocalDate(year = today.year + 1, month = month, dayOfMonth = day) }.getOrNull()
  } else {
    candidate
  }
}

private fun LocalDate.Companion.parseOrNull(value: String): LocalDate? = runCatching { parse(value) }.getOrNull()

private fun String.toMonthOrNull(): Month? = when (lowercase().trimEnd('.')) {
  "jan", "january" -> Month.JANUARY
  "feb", "february" -> Month.FEBRUARY
  "mar", "march" -> Month.MARCH
  "apr", "april" -> Month.APRIL
  "may" -> Month.MAY
  "jun", "june" -> Month.JUNE
  "jul", "july" -> Month.JULY
  "aug", "august" -> Month.AUGUST
  "sep", "sept", "september" -> Month.SEPTEMBER
  "oct", "october" -> Month.OCTOBER
  "nov", "november" -> Month.NOVEMBER
  "dec", "december" -> Month.DECEMBER
  else -> null
}
