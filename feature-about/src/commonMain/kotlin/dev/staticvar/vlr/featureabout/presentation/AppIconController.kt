/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.runtime.Composable

internal enum class AppIcon {
  Default,
  Amethyst,
  Ticket,
  Arcade,
  Midnight,
  Mint,
}

internal interface AppIconController {
  val supported: Boolean

  fun current(): AppIcon

  suspend fun select(icon: AppIcon)
}

@Composable
internal expect fun rememberAppIconController(): AppIconController
