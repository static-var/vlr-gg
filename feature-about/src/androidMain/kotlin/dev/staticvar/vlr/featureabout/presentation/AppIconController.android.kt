/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val DefaultAlias = "dev.staticvar.vlr.android.MainActivityDefault"
private const val AmethystAlias = "dev.staticvar.vlr.android.MainActivityAmethyst"
private const val TicketAlias = "dev.staticvar.vlr.android.MainActivityTicket"

@Composable
internal actual fun rememberAppIconController(): AppIconController {
  val applicationContext = LocalContext.current.applicationContext
  return remember(applicationContext) { AndroidAppIconController(applicationContext) }
}

private class AndroidAppIconController(context: Context) : AppIconController {
  private val packageManager = context.packageManager
  private val aliases = mapOf(
    AppIcon.Default to ComponentName(context.packageName, DefaultAlias),
    AppIcon.Amethyst to ComponentName(context.packageName, AmethystAlias),
    AppIcon.Ticket to ComponentName(context.packageName, TicketAlias),
  )

  override val supported: Boolean = true

  override fun current(): AppIcon =
    AppIcon.entries.firstOrNull { icon ->
      packageManager.getComponentEnabledSetting(aliases.getValue(icon)) ==
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } ?: AppIcon.Default

  override suspend fun select(icon: AppIcon) {
    val selected = aliases.getValue(icon)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      packageManager.setComponentEnabledSettings(
        aliases.values.map { alias ->
          PackageManager.ComponentEnabledSetting(
            alias,
            if (alias == selected) {
              PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
              PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            },
            PackageManager.DONT_KILL_APP,
          )
        },
      )
      return
    }

    packageManager.setComponentEnabledSetting(
      selected,
      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
      PackageManager.DONT_KILL_APP,
    )
    aliases.values.filterNot { it == selected }.forEach { alias ->
      packageManager.setComponentEnabledSetting(
        alias,
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP,
      )
    }
  }
}
