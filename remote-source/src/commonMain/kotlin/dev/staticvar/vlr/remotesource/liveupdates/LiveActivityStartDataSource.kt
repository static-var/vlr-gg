/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.liveupdates

/** Whether the server accepted, rejected, or could not confirm a Live Activity start. */
public enum class LiveActivityStartResult {
  Started,
  Rejected,
  Unknown,
}

/** Requests a Live Activity for a client and match. */
public interface LiveActivityStartDataSource {
  public suspend fun start(clientId: String, matchId: String): LiveActivityStartResult
}
