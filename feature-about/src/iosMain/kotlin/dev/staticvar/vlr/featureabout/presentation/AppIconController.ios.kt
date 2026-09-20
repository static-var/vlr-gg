/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.UIKit.UIApplication
import platform.UIKit.alternateIconName
import platform.UIKit.setAlternateIconName
import platform.UIKit.supportsAlternateIcons
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val ArcadeIconName = "AppIconArcade"
private const val MidnightIconName = "AppIconMidnight"
private const val MintIconName = "AppIconMint"
private const val AmethystIconName = "AppIconAmethyst"
private const val TicketIconName = "AppIconTicket"

@Composable
internal actual fun rememberAppIconController(): AppIconController = remember {
  IosAppIconController(UIApplication.sharedApplication)
}

private class IosAppIconController(
  private val application: UIApplication,
) : AppIconController {
  override val supported: Boolean
    get() = application.supportsAlternateIcons

  override fun current(): AppIcon = when (application.alternateIconName) {
    ArcadeIconName -> AppIcon.Arcade
    MidnightIconName -> AppIcon.Midnight
    MintIconName -> AppIcon.Mint
    AmethystIconName -> AppIcon.Amethyst
    TicketIconName -> AppIcon.Ticket
    else -> AppIcon.Default
  }

  override suspend fun select(icon: AppIcon) {
    val alternateIconName = when (icon) {
      AppIcon.Default -> null
      AppIcon.Arcade -> ArcadeIconName
      AppIcon.Midnight -> MidnightIconName
      AppIcon.Mint -> MintIconName
      AppIcon.Amethyst -> AmethystIconName
      AppIcon.Ticket -> TicketIconName
    }
    withContext(Dispatchers.Main) {
      if (application.alternateIconName == alternateIconName) return@withContext
      suspendCancellableCoroutine { continuation ->
        application.setAlternateIconName(alternateIconName) { error ->
          if (!continuation.isActive) return@setAlternateIconName
          if (error == null) {
            continuation.resume(Unit)
          } else {
            continuation.resumeWithException(IllegalStateException(error.localizedDescription))
          }
        }
      }
    }
  }
}
