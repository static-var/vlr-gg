/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurehome.usecase

import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.repository.MatchRepository
import dev.staticvar.vlr.featurehome.presentation.HomeFeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Clock
import kotlin.time.Instant

public class ObserveHomeFeedUseCase(
  private val favoritesRepository: FavoritesRepository,
  private val matchRepository: MatchRepository,
  private val eventRepository: EventRepository,
) {
  public operator fun invoke(): Flow<HomeFeed> = combine(
    favoritesRepository.observeDirectFavorites(),
    matchRepository.getMatches(),
    eventRepository.getEvents(),
    ::buildHomeFeed,
  )
}

internal fun buildHomeFeed(
  directFavorites: DirectFavoriteSnapshot,
  matches: List<MatchPreview>,
  events: List<EventPreview>,
): HomeFeed {
  val relatedMatches = matches.filter { it.favoriteReasons.isNotEmpty() }
  val personalizedMatches = relatedMatches
    .asSequence()
    .filter { it.isCurrent }
    .distinctBy(MatchPreview::id)
    .sortedWith(homeMatchComparator)
    .toList()

  val relatedEventIds = buildSet {
    directFavorites.events.mapTo(this) { favorite -> favorite.id }
    relatedMatches
      .map(MatchPreview::eventId)
      .filter(String::isNotBlank)
      .toCollection(this)
  }
  val personalizedEvents = events
    .asSequence()
    .filter { event -> event.id in relatedEventIds && event.isCurrent }
    .distinctBy(EventPreview::id)
    .sortedWith(homeEventComparator)
    .toList()

  val savedMatches = directFavorites.matches.associateBy { it.id }
  val savedEvents = directFavorites.events.associateBy { it.id }
  return HomeFeed(
    hasDirectFavorites = directFavorites.hasAny,
    directFavorites = directFavorites.copy(
      matches = matches.filter { it.isCurrent }.distinctBy { it.id }
        .sortedWith(homeMatchComparator).mapNotNull { savedMatches[it.id] },
      events = events.filter { it.isCurrent }.distinctBy { it.id }
        .sortedWith(homeEventComparator).mapNotNull { savedEvents[it.id] },
    ),
    personalizedMatches = personalizedMatches,
    personalizedEvents = personalizedEvents,
  )
}

private val MatchPreview.isCurrent: Boolean
  get() = status == MatchStatus.LIVE || status == MatchStatus.UPCOMING

private val EventPreview.isCurrent: Boolean
  get() = status == EventStatus.ONGOING || status == EventStatus.UPCOMING

private val homeMatchComparator: Comparator<MatchPreview> = Comparator { first, second ->
  compareNullableAscending(first.time.asEpochMillis(), second.time.asEpochMillis())
    .takeUnless { it == 0 } ?: first.id.compareTo(second.id)
}

private fun String?.asEpochMillis(): Long? = this
  ?.takeIf(String::isNotBlank)
  ?.let { value -> runCatching { Instant.parse(value).toEpochMilliseconds() }.getOrNull() }

private fun compareNullableAscending(first: Long?, second: Long?): Int = when {
  first == null && second == null -> 0
  first == null -> 1
  second == null -> -1
  else -> first.compareTo(second)
}

private val homeEventComparator: Comparator<EventPreview> = Comparator { first, second ->
  compareNullableAscending(first.dates.eventDateValues().firstOrNull(), second.dates.eventDateValues().firstOrNull())
    .takeUnless { it == 0 } ?: first.id.compareTo(second.id)
}

private fun String.eventDateValues(): List<Long> {
  val isoValues = isoDate.findAll(this).mapNotNull { match ->
    val year = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
    val month = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
    val day = match.groupValues[3].toLongOrNull() ?: return@mapNotNull null
    year * 10_000 + month * 100 + day
  }.toList()
  if (isoValues.isNotEmpty()) return isoValues

  val sharedYear = year.find(this)?.value?.toLongOrNull()
    ?: Clock.System.now().toString().take(4).toLong()
  var precedingMonth: Long? = null
  return split(namedDateRangeSeparator).mapNotNull { part ->
    val tokens = part.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    val month = tokens.firstNotNullOfOrNull(String::monthNumber) ?: precedingMonth
    tokens.firstNotNullOfOrNull(String::monthNumber)?.let { precedingMonth = it }
    val day = tokens.firstNotNullOfOrNull { token ->
      token.trim(',', '.').toLongOrNull()?.takeIf { number -> number in 1..31 }
    }
    if (month == null || day == null) null else sharedYear * 10_000 + month * 100 + day
  }
}

private fun String.monthNumber(): Long? = when (lowercase().trim('.', ',')) {
  "jan", "january" -> 1
  "feb", "february" -> 2
  "mar", "march" -> 3
  "apr", "april" -> 4
  "may" -> 5
  "jun", "june" -> 6
  "jul", "july" -> 7
  "aug", "august" -> 8
  "sep", "sept", "september" -> 9
  "oct", "october" -> 10
  "nov", "november" -> 11
  "dec", "december" -> 12
  else -> null
}

private val isoDate: Regex = Regex("(\\d{4})-(\\d{2})-(\\d{2})")
private val year: Regex = Regex("\\b\\d{4}\\b")
private val namedDateRangeSeparator: Regex = Regex("\\s*(?:-|–|—)\\s*")
