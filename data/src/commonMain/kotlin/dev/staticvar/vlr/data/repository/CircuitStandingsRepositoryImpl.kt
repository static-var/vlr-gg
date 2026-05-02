/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.data.mapper.aggregateCircuitRegion
import dev.staticvar.vlr.data.mapper.aggregateCircuitStandings
import dev.staticvar.vlr.data.mapper.toEntity
import dev.staticvar.vlr.domain.model.CircuitRegion
import dev.staticvar.vlr.domain.model.CircuitStandings
import dev.staticvar.vlr.domain.repository.CircuitStandingsRepository
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.standings.StandingsDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class CircuitStandingsRepositoryImpl(
  private val standingsDataSource: StandingsDataSource,
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
) : CircuitStandingsRepository {

  private val rankingsQueries = database.rankingsQueries

  override fun getStandingsByYear(year: Int): Flow<CircuitStandings?> = rankingsQueries
    .getStandingsByYear(year.toLong())
    .asFlow()
    .mapToList(dispatchers.io)
    .map { standings ->
      if (standings.isEmpty()) {
        null
      } else {
        aggregateCircuitStandings(year, standings)
      }
    }

  override fun getStandingsByYearAndCircuit(year: Int, circuitName: String): Flow<CircuitRegion?> = rankingsQueries
    .getStandingsByYearAndCircuit(year.toLong(), circuitName)
    .asFlow()
    .mapToList(dispatchers.io)
    .map { standings -> aggregateCircuitRegion(circuitName, standings) }

  override suspend fun refreshStandings(year: Int): Result<Unit> = withContext(dispatchers.io) {
    standingsDataSource.byYear(year).mapCatching { dto ->
      val effectiveYear = dto.year.takeIf { it > 0 } ?: year

      database.transaction {
        rankingsQueries.deleteStandingsByYear(effectiveYear.toLong())

        dto.circuits.forEach { circuit ->
          circuit.teams.forEach { team ->
            val entity = team.toEntity(
              year = effectiveYear,
              circuit = circuit.region,
              region = circuit.region,
            )
            rankingsQueries.insertStandingDetails(
              team_id = entity.team_id,
              year = entity.year,
              circuit = entity.circuit,
              region = entity.region,
              team_name = entity.team_name,
              team_logo = entity.team_logo,
              country = entity.country,
              rank = entity.rank,
              points = entity.points,
              last_updated = entity.last_updated,
            )
          }
        }
      }

      Unit
    }
  }
}
