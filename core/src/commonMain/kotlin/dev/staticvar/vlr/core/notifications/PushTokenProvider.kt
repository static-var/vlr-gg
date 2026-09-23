/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.notifications

/** Identifies the platform that issued a push token. */
public enum class PushPlatform {
  Ios,
  Android,
}

/** Delivers native push tokens and their changes while observation is active. */
public interface PushTokenProvider {
  public val platform: PushPlatform

  public fun start(onToken: (String) -> Unit)

  public fun stop()
}
