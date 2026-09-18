/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.team

import dev.staticvar.vlr.remotesource.network.RemotePayload

interface TeamDataSource {
  suspend fun details(id: String): Result<RemotePayload<TeamDetailsDto>>
}
