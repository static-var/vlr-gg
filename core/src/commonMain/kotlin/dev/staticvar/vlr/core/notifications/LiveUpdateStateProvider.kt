/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.notifications

import kotlinx.coroutines.flow.StateFlow

/** Reports whether live updates can start and which matches the device has already observed. */
public interface LiveUpdateStateProvider {
  public val platform: PushPlatform

  public fun canRequestStart(): Boolean

  public fun observedMatchIds(): List<String>
}

/** Android-only recovery of notifications deliberately dismissed by the user. */
public interface DismissedLiveUpdateProvider : LiveUpdateStateProvider {
  public val dismissedMatchIds: StateFlow<Set<String>>

  /** Allows one equal-timestamp snapshot again, without accepting older or terminal state. */
  public fun restoreDismissedMatch(matchId: String): Boolean

  /** Restores suppression when the server explicitly rejects a requested restart. */
  public fun keepMatchDismissed(matchId: String)
}
