/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

data class TeamSearchResult(
  val teamId: String,
  val teamName: String,
  val teamLogo: String,
  val shortName: String? = null,
)
