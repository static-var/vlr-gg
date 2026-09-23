/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.notifications

public interface LiveUpdateStateProvider {
  public val platform: PushPlatform

  public fun canRequestStart(): Boolean

  public fun observedMatchIds(): List<String>
}
