/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.notifications

public enum class PushPlatform {
  Ios,
  Android,
}

public interface PushTokenProvider {
  public val platform: PushPlatform

  public fun start(onToken: (String) -> Unit)

  public fun stop()
}
