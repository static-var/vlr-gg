/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.repository

import dev.staticvar.vlr.domain.model.CircuitRegion
import dev.staticvar.vlr.domain.model.CircuitStandings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for VCT Circuit Standings operations.
 * Pure domain contract - no implementation details.
 */
interface CircuitStandingsRepository {
  /**
   * Get all circuit standings for a specific year.
   * Returns Flow for reactive updates when standings data changes.
   */
  fun getStandingsByYear(year: Int): Flow<CircuitStandings?>

  /**
   * Get standings for a specific circuit/region in a year.
   */
  fun getStandingsByYearAndCircuit(year: Int, circuitName: String): Flow<CircuitRegion?>

  /**
   * Refresh circuit standings from remote source for a specific year.
   */
  suspend fun refreshStandings(year: Int): Result<Unit>
}
