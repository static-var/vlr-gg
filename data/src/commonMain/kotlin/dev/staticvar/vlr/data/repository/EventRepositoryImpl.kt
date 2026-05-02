/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
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
import dev.staticvar.vlr.data.mapper.toPrizeEntities
import dev.staticvar.vlr.data.mapper.toStandingEntities
import dev.staticvar.vlr.data.mapper.toTeamEntities
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.localsource.database.GetEventWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.GetEventsWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.events.EventDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class EventRepositoryImpl(
  private val eventDataSource: EventDataSource,
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
) : EventRepository {

  private val eventsQueries = database.eventsQueries

  override fun getEvents(): Flow<List<EventPreview>> = eventsQueries
    .getEventsWithFavoriteStatus()
    .asFlow()
    .mapToList(dispatchers.io)
    .map { events -> events.map { it.toEventPreview() } }

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
      }
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

  override suspend fun refreshEvents(): Result<Unit> = withContext(dispatchers.io) {
    eventDataSource.list().mapCatching { dtos ->
      database.transaction {
        val existing = eventsQueries
          .getEventsWithFavoriteStatus()
          .executeAsList()
          .associateBy { it.id }
        val remoteIds = mutableSetOf<String>()

        dtos.forEach { dto ->
          val entity = dto.toEntity()
          if (entity.id.isBlank()) return@forEach
          remoteIds += entity.id
          val merged = mergeEventListEntity(entity, existing[entity.id])
          eventsQueries.insertEvent(merged)
        }

        val staleIds = existing.keys - remoteIds
        staleIds.forEach { id -> eventsQueries.deleteEventById(id) }
      }
    }
  }

  override suspend fun refreshEventDetails(eventId: String): Result<Unit> = withContext(dispatchers.io) {
    eventDataSource.details(eventId).mapCatching { dto ->
      val normalizedDto = dto.copy(id = dto.id.ifBlank { eventId })
      database.transaction {
        val eventEntity = normalizedDto.toEventEntity().copy(id = eventId)
        eventsQueries.insertEvent(eventEntity)

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
