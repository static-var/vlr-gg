/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.runtime.Composable

// iOS ships only the default icon to keep the app small. When an update removes
// an alternate icon, iOS moves anyone using it back to the default.
@Composable
internal actual fun rememberAppIconController(): AppIconController = IosAppIconController

private object IosAppIconController : AppIconController {
  override val supported: Boolean = false

  override fun current(): AppIcon = AppIcon.Default

  override suspend fun select(icon: AppIcon) = Unit
}
