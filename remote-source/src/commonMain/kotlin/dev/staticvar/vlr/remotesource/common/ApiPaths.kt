/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.common

/** Centralized endpoint path constants. */
object ApiPaths {
  const val EVENTS = "/api/v1/events/"
  fun event(id: String) = "/api/v1/events/$id"

  const val MATCHES = "/api/v1/matches/"
  fun match(id: String) = "/api/v1/matches/$id"

  fun player(id: String) = "/api/v1/player/$id"
  fun team(id: String) = "/api/v1/team/$id"
  const val RANKINGS = "/api/v2/rankings/"
  fun standings(year: Int) = "/api/v1/standings/$year"
  const val SEARCH = "/api/v1/search/"
  const val VERSION = "/api/v1/version/"
}
