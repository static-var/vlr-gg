/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.core.telemetry.traceRefresh
import dev.staticvar.vlr.data.EventMatches
import dev.staticvar.vlr.data.EventPrizes
import dev.staticvar.vlr.data.EventStandings
import dev.staticvar.vlr.data.EventTeams
import dev.staticvar.vlr.data.Events
import dev.staticvar.vlr.data.mapper.aggregateEventDetails
import dev.staticvar.vlr.data.mapper.toEntity
import dev.staticvar.vlr.data.mapper.toEventEntity
import dev.staticvar.vlr.data.mapper.toEventMatchLinkEntities
import dev.staticvar.vlr.data.mapper.toEventPreview
import dev.staticvar.vlr.data.mapper.toOverviewEntity
import dev.staticvar.vlr.data.mapper.toPrizeEntities
import dev.staticvar.vlr.data.mapper.toStandingEntities
import dev.staticvar.vlr.data.mapper.toTeamEntities
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventFavoriteReason
import dev.staticvar.vlr.domain.model.EventFavoriteSource
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.localsource.database.GetEventWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.GetEventsWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.GetMatchFavoriteReasons
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.events.EventDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class EventRepositoryImpl(
  private val eventDataSource: EventDataSource,
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
) : EventRepository {

  private val eventsQueries = database.eventsQueries
  private val eventOverviewQueries = database.eventOverviewQueries

  override fun getEvents(): Flow<List<EventPreview>> = eventOverviewQueries
    .getEventOverviewWithFavoriteStatus()
    .asFlow()
    .mapToList(dispatchers.io)
    .combine(observeRelatedFavoriteReasons()) { events, reasonsByEvent ->
      events.map { row ->
        val event = row.toEventPreview()
        event.copy(favoriteReasons = favoriteReasons(event.id, event.title, event.isFavorite, reasonsByEvent))
      }
    }
    .flowOn(dispatchers.default)

  override fun getEventDetails(eventId: String): Flow<EventDetails?> {
    val eventFlow = eventsQueries
      .getEventWithFavoriteStatus(eventId)
      .asFlow()
      .mapToOneOrNull(dispatchers.io)

    val prizesFlow = eventsQueries
      .getEventPrizes(eventId)
      .asFlow()
      .mapToList(dispatchers.io)

    val teamsFlow = eventsQueries
      .getEventTeams(eventId)
      .asFlow()
      .mapToList(dispatchers.io)

    val standingsFlow = eventsQueries
      .getEventStandings(eventId)
      .asFlow()
      .mapToList(dispatchers.io)

    val matchesFlow = eventsQueries
      .getEventMatches(eventId)
      .asFlow()
      .mapToList(dispatchers.io)

    return eventFlow
      .combine(prizesFlow) { event, prizes -> EventDetailSlices(event = event, prizes = prizes) }
      .combine(teamsFlow) { slices, teams -> slices.copy(teams = teams) }
      .combine(standingsFlow) { slices, standings -> slices.copy(standings = standings) }
      .combine(matchesFlow) { slices, matches -> slices.copy(matches = matches) }
      .map { slices ->
        slices.event?.let {
          aggregateEventDetails(
            event = it,
            prizes = slices.prizes,
            teams = slices.teams,
            standings = slices.standings,
            matches = slices.matches,
          )
        }
      }.combine(observeRelatedFavoriteReasons(eventId)) { event, reasonsByEvent ->
        event?.copy(favoriteReasons = favoriteReasons(event.id, event.title, event.isFavorite, reasonsByEvent))
      }
      .flowOn(dispatchers.default)
  }

  private fun observeRelatedFavoriteReasons(
    eventId: String? = null,
  ): Flow<Map<String, List<EventFavoriteReason>>> = eventId?.let { scopedEventId ->
    database.matchesQueries
      .getScopedMatchFavoriteReasons(
        matchId = null,
        eventId = scopedEventId,
        mapper = ::GetMatchFavoriteReasons,
      )
      .asFlow()
      .mapToList(dispatchers.io)
      .map { reasons -> reasons.toEventFavoriteReasons { scopedEventId } }
  } ?: database.matchesQueries
    .getMatchFavoriteReasons()
    .asFlow()
    .mapToList(dispatchers.io)
    .combine(database.matchOverviewQueries.getMatchOverview().asFlow().mapToList(dispatchers.io)) { reasons, matches ->
      val eventIdsByMatch = matches.associate { it.id to it.event_id }
      reasons.toEventFavoriteReasons { reason -> eventIdsByMatch[reason.match_id] }
    }

  private fun List<GetMatchFavoriteReasons>.toEventFavoriteReasons(
    eventId: (GetMatchFavoriteReasons) -> String?,
  ): Map<String, List<EventFavoriteReason>> = filter { it.source != "EVENT" }
    .mapNotNull { reason ->
      val relatedEventId = eventId(reason)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
      relatedEventId to EventFavoriteReason(
        source = EventFavoriteSource.valueOf(reason.source),
        id = reason.entity_id,
        name = reason.entity_name,
      )
    }
    .groupBy({ it.first }, { it.second })
    .mapValues { (_, eventReasons) ->
      eventReasons.distinctBy { it.source to it.id }
        .sortedWith(compareBy({ it.source.ordinal }, { it.name }, { it.id }))
    }

  private fun favoriteReasons(
    eventId: String,
    title: String,
    isFavorite: Boolean,
    reasonsByEvent: Map<String, List<EventFavoriteReason>>,
  ): List<EventFavoriteReason> = reasonsByEvent[eventId].orEmpty() + if (isFavorite) {
    listOf(EventFavoriteReason(EventFavoriteSource.EVENT, eventId, title))
  } else {
    emptyList()
  }

  override suspend fun addToFavorites(eventId: String): Result<Unit> = withContext(dispatchers.io) {
    runCatching {
      eventsQueries.addFavoriteEvent(eventId)
      Unit
    }
  }

  override suspend fun removeFromFavorites(eventId: String): Result<Unit> = withContext(dispatchers.io) {
    runCatching {
      eventsQueries.removeFavoriteEvent(eventId)
      Unit
    }
  }

  override suspend fun refreshEvents(): Result<Unit> = traceRefresh(dispatchers.io, "refreshEvents") {
    eventDataSource.list().mapCatching { dtos ->
      traceDatabase {
        database.transaction {
          val existing = eventsQueries
            .getEventsWithFavoriteStatus()
            .executeAsList()
            .associateBy { it.id }
          val remoteIds = mutableSetOf<String>()
          val overviewIds = eventOverviewQueries.getEventOverviewWithFavoriteStatus().executeAsList().map { it.id }.toSet()

          dtos.forEachIndexed { index, dto ->
            val entity = dto.toEntity()
            if (entity.id.isBlank()) return@forEachIndexed
            remoteIds += entity.id
            eventOverviewQueries.insertEventOverview(dto.toOverviewEntity(index.toLong()))
            val merged = mergeEventListEntity(entity, existing[entity.id])
            persistEvent(merged)
          }

          (overviewIds - remoteIds).forEach { id -> eventOverviewQueries.deleteEventOverviewById(id) }
        }
      }
    }
  }

  override suspend fun refreshEventDetails(eventId: String): Result<Unit> = traceRefresh(dispatchers.io, "refreshEventDetails") {
    eventDataSource.details(eventId).mapCatching { dto ->
      val normalizedDto = dto.copy(id = dto.id.ifBlank { eventId })
      traceDatabase {
        database.transaction {
          val eventEntity = normalizedDto.toEventEntity().copy(id = eventId)
          persistEvent(eventEntity)

          eventsQueries.deleteEventPrizes(eventId)
          eventsQueries.deleteEventTeams(eventId)
          eventsQueries.deleteEventStandings(eventId)
          eventsQueries.deleteEventMatches(eventId)

          normalizedDto.toPrizeEntities().forEach { prize ->
            eventsQueries.insertEventPrize(
              event_id = prize.event_id,
              position = prize.position,
              prize = prize.prize,
              team_id = prize.team_id,
              team_name = prize.team_name,
              team_logo_url = prize.team_logo_url,
              team_country = prize.team_country,
            )
          }

          normalizedDto.toTeamEntities().forEach { team ->
            eventsQueries.insertEventTeam(
              event_id = team.event_id,
              team_id = team.team_id,
              team_name = team.team_name,
              team_logo_url = team.team_logo_url,
              seed = team.seed,
            )
          }

          normalizedDto.toStandingEntities().forEach { standing ->
            eventsQueries.insertEventStanding(
              event_id = standing.event_id,
              team_name = standing.team_name,
              team_logo_url = standing.team_logo_url,
              team_country = standing.team_country,
              group_name = standing.group_name,
              wins = standing.wins,
              losses = standing.losses,
              ties = standing.ties,
              map_difference = standing.map_difference,
              round_difference = standing.round_difference,
              round_delta = standing.round_delta,
            )
          }

          normalizedDto.toEventMatchLinkEntities().forEach { match ->
            eventsQueries.insertEventMatchDetails(
              event_id = match.event_id,
              match_id = match.match_id,
              time = match.time,
              date = match.date,
              eta = match.eta,
              status = match.status,
              team1_name = match.team1_name,
              team1_region = match.team1_region,
              team1_score = match.team1_score,
              team2_name = match.team2_name,
              team2_region = match.team2_region,
              team2_score = match.team2_score,
              round = match.round,
              stage = match.stage,
            )
          }
        }
      }
    }
  }

  private fun persistEvent(entity: Events) {
    if (eventsQueries.getEventWithFavoriteStatus(entity.id).executeAsOneOrNull() == null) {
      eventsQueries.insertEvent(entity)
    } else {
      eventsQueries.updateEvent(
        name = entity.name,
        subtitle = entity.subtitle,
        status = entity.status,
        prizes = entity.prizes,
        dates = entity.dates,
        region = entity.region,
        logo_url = entity.logo_url,
        last_updated = entity.last_updated,
        id = entity.id,
      )
    }
  }

  private fun mergeEventListEntity(entity: Events, current: GetEventsWithFavoriteStatus?): Events {
    if (current == null) return entity
    return entity.copy(
      subtitle = current.subtitle,
      status = entity.status ?: current.status,
      prizes = entity.prizes.ifBlank { current.prizes },
      dates = entity.dates.ifBlank { current.dates },
      region = entity.region ?: current.region,
      logo_url = entity.logo_url.ifBlank { current.logo_url },
      last_updated = entity.last_updated,
    )
  }

  private data class EventDetailSlices(
    val event: GetEventWithFavoriteStatus?,
    val prizes: List<EventPrizes> = emptyList(),
    val teams: List<EventTeams> = emptyList(),
    val standings: List<EventStandings> = emptyList(),
    val matches: List<EventMatches> = emptyList(),
  )
}
