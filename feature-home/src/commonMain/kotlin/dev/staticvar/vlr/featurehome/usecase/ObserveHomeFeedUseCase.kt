/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurehome.usecase

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchFavoriteReason
import dev.staticvar.vlr.domain.model.MatchFavoriteSource
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.repository.FavoriteMatchesRepository
import dev.staticvar.vlr.domain.repository.MatchRepository
import dev.staticvar.vlr.featurehome.presentation.HomeFeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

public class ObserveHomeFeedUseCase(
  private val favoritesRepository: FavoritesRepository,
  private val matchRepository: MatchRepository,
  private val eventRepository: EventRepository,
  private val favoriteMatchesRepository: FavoriteMatchesRepository,
  private val dispatchers: DispatcherProvider,
  private val clock: Clock = Clock.System,
) {
  public operator fun invoke(): Flow<HomeFeed> = combine(
    favoritesRepository.observeDirectFavorites(),
    matchRepository.getMatches(),
    eventRepository.getEvents(),
    favoriteMatchesRepository.homeMatches,
  ) { favorites, matches, events, serverFeed ->
    buildHomeFeed(favorites, matches, events, clock.now(), serverFeed?.takeIf { it.selection == favorites }?.matches)
  }.flowOn(dispatchers.default)
}

internal fun buildHomeFeed(
  directFavorites: DirectFavoriteSnapshot,
  matches: List<MatchPreview>,
  events: List<EventPreview>,
  now: Instant = Clock.System.now(),
  serverMatches: List<MatchPreview>? = null,
): HomeFeed {
  val earliestMatchTime = (now - 24.hours).toEpochMilliseconds()
  val remoteMatches = serverMatches?.map { match -> match.withFavoriteReasons(directFavorites) }
  val relatedMatches = remoteMatches ?: matches.filter { it.favoriteReasons.isNotEmpty() }
  val personalizedMatches = if (remoteMatches != null) {
    remoteMatches.distinctBy(MatchPreview::id).sortedWith(homeMatchComparator)
  } else {
    relatedMatches.asSequence()
      .filter { it.isVisibleSince(earliestMatchTime) }
      .distinctBy(MatchPreview::id)
      .sortedWith(homeMatchComparator)
      .toList()
  }

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
      matches = (remoteMatches ?: matches).filter { it.isVisibleSince(earliestMatchTime) }.distinctBy { it.id }
        .sortedWith(homeMatchComparator).mapNotNull { savedMatches[it.id] },
      events = events.filter { it.isCurrent }.distinctBy { it.id }
        .sortedWith(homeEventComparator).mapNotNull { savedEvents[it.id] },
    ),
    personalizedMatches = personalizedMatches,
    personalizedEvents = personalizedEvents,
  )
}

private fun MatchPreview.withFavoriteReasons(favorites: DirectFavoriteSnapshot): MatchPreview {
  val teamIds = setOfNotNull(team1.id, team2.id)
  val reasons = buildList {
    favorites.matches.filter { it.id == id }.forEach { add(MatchFavoriteReason(MatchFavoriteSource.MATCH, it.id, it.title)) }
    favorites.teams.filter { it.id in teamIds }.forEach { add(MatchFavoriteReason(MatchFavoriteSource.TEAM, it.id, it.title)) }
    favorites.players.filter { it.currentTeamId?.let(teamIds::contains) == true }
      .forEach { add(MatchFavoriteReason(MatchFavoriteSource.PLAYER, it.id, it.title)) }
    favorites.events.filter { it.id == eventId }.forEach { add(MatchFavoriteReason(MatchFavoriteSource.EVENT, it.id, it.title)) }
  }
  return copy(isFavorite = true, isDirectFavorite = favorites.matches.any { it.id == id }, favoriteReasons = reasons)
}

private fun MatchPreview.isVisibleSince(earliestTime: Long): Boolean {
  if (status == MatchStatus.LIVE) return true
  val startTime = time.asEpochMillis() ?: return status == MatchStatus.UPCOMING
  return startTime >= earliestTime
}

private val EventPreview.isCurrent: Boolean
  get() = status == EventStatus.ONGOING || status == EventStatus.UPCOMING

private val homeMatchComparator: Comparator<MatchPreview> = Comparator { first, second ->
  first.status.homeSortOrder.compareTo(second.status.homeSortOrder)
    .takeUnless { it == 0 }
    ?: compareNullableAscending(first.time.asEpochMillis(), second.time.asEpochMillis())
    .takeUnless { it == 0 } ?: first.id.compareTo(second.id)
}

private val MatchStatus.homeSortOrder: Int
  get() = when (this) {
    MatchStatus.COMPLETED -> 0
    MatchStatus.LIVE -> 1
    MatchStatus.UPCOMING -> 2
    MatchStatus.UNKNOWN -> 3
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
