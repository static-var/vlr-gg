/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.liveupdates

public enum class LiveActivityStartResult {
  Started,
  Rejected,
  Unknown,
}

public interface LiveActivityStartDataSource {
  public suspend fun start(clientId: String, matchId: String): LiveActivityStartResult
}
