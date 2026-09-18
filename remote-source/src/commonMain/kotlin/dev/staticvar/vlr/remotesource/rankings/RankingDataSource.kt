/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.rankings

import dev.staticvar.vlr.remotesource.network.RemotePayload

interface RankingsDataSource {
  suspend fun list(): Result<RemotePayload<List<RankingDto>>>
}
