/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.core.telemetry.traceRefresh
import dev.staticvar.vlr.data.cache.EmptyRegionLabelStore
import dev.staticvar.vlr.data.cache.RegionLabelStore
import dev.staticvar.vlr.data.mapper.aggregateCircuitRegion
import dev.staticvar.vlr.data.mapper.aggregateCircuitStandings
import dev.staticvar.vlr.data.mapper.toEntity
import dev.staticvar.vlr.domain.model.CircuitRegion
import dev.staticvar.vlr.domain.model.CircuitStandings
import dev.staticvar.vlr.domain.repository.CircuitStandingsRepository
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.standings.StandingsDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class CircuitStandingsRepositoryImpl(
  private val standingsDataSource: StandingsDataSource,
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
  private val regionLabels: RegionLabelStore = EmptyRegionLabelStore,
) : CircuitStandingsRepository {

  private val rankingsQueries = database.rankingsQueries

  override fun getStandingsByYear(year: Int): Flow<CircuitStandings?> = rankingsQueries
    .getStandingsByYear(year.toLong())
    .asFlow()
    .mapToList(dispatchers.io)
    .combine(regionLabels.version) { standings, _ ->
      if (standings.isEmpty()) {
        null
      } else {
        aggregateCircuitStandings(year, standings, regionLabels::label)
      }
    }

  override fun getStandingsByYearAndCircuit(year: Int, circuitName: String): Flow<CircuitRegion?> = rankingsQueries
    .getStandingsByYearAndCircuit(year.toLong(), circuitName)
    .asFlow()
    .mapToList(dispatchers.io)
    .combine(regionLabels.version) { standings, _ ->
      aggregateCircuitRegion(circuitName, standings, regionLabels::label)
    }

  override suspend fun refreshStandings(year: Int): Result<Unit> = traceRefresh(dispatchers.io, "refreshStandings") {
    standingsDataSource.byYear(year).mapCatching { payload ->
      val dto = payload.value
      val effectiveYear = dto.year.takeIf { it > 0 } ?: year

      traceDatabase {
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
      }
      regionLabels.put(
        contentLanguage = payload.contentLanguage,
        labels = dto.circuits.associate { it.region to it.regionLabel.orEmpty() },
      )

      Unit
    }
  }
}
