/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.standings

import dev.staticvar.vlr.remotesource.network.RemotePayload

interface StandingsDataSource {
  suspend fun byYear(year: Int): Result<RemotePayload<StandingsDto>>
}
