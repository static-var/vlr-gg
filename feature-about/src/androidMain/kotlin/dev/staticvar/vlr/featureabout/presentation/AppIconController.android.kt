/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.staticvar.vlr.sharedui.icon.syncLauncherSplashTheme

private const val DefaultAlias = "dev.staticvar.vlr.android.MainActivityDefault"
private const val AmethystAlias = "dev.staticvar.vlr.android.MainActivityAmethyst"
private const val TicketAlias = "dev.staticvar.vlr.android.MainActivityTicket"
private const val ArcadeAlias = "dev.staticvar.vlr.android.MainActivityArcade"
private const val MidnightAlias = "dev.staticvar.vlr.android.MainActivityMidnight"
private const val MintAlias = "dev.staticvar.vlr.android.MainActivityMint"

@Composable
internal actual fun rememberAppIconController(): AppIconController {
  val applicationContext = LocalContext.current.applicationContext
  val activity = LocalActivity.current
  return remember(applicationContext, activity) {
    AndroidAppIconController(applicationContext) { activity?.syncLauncherSplashTheme() }
  }
}

private class AndroidAppIconController(
  context: Context,
  private val onIconChanged: () -> Unit,
) : AppIconController {
  private val packageManager = context.packageManager
  private val aliases = mapOf(
    AppIcon.Arcade to ComponentName(context.packageName, ArcadeAlias),
    AppIcon.Midnight to ComponentName(context.packageName, MidnightAlias),
    AppIcon.Mint to ComponentName(context.packageName, MintAlias),
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
      onIconChanged()
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
    onIconChanged()
  }
}
