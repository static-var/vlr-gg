/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

/**
 * Domain model for VCT Circuit standings by year.
 * Contains all regional circuit standings for a specific year.
 */
data class CircuitStandings(val year: Int, val circuits: List<CircuitRegion>)

/**
 * Regional circuit standings (e.g., "Americas Championship", "China Championship").
 */
data class CircuitRegion(val circuitName: String, val region: String, val teams: List<CircuitTeam>)

/**
 * Team entry in circuit standings.
 */
data class CircuitTeam(
  val id: String,
  val name: String,
  val logo: String,
  val rank: Int,
  val points: Int,
  val country: String,
)
