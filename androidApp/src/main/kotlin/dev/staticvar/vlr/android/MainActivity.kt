/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.staticvar.vlr.sharedui.icon.syncLauncherSplashTheme
import dev.staticvar.vlr.widget.ScoreWidget
import androidx.glance.appwidget.updateAll
import dev.staticvar.vlr.android.widget.LegacyWidgetRefreshScheduler
import dev.staticvar.vlr.android.widget.FavoriteMatchWidgets
import dev.staticvar.vlr.android.widget.WidgetSnapshotStore
import dev.staticvar.vlr.shared.App
import dev.staticvar.vlr.shared.di.LocalViewModelObserver
import dev.staticvar.vlr.shared.navigation.AppDeepLinkHandler
import dev.staticvar.vlr.sharedui.notifications.LocalNotificationPermissionProvider
import dev.staticvar.vlr.sharedui.notifications.rememberAndroidNotificationPermissionProvider
import dev.staticvar.vlr.sharedui.share.LocalImageSharer
import dev.staticvar.vlr.sharedui.share.rememberAndroidImageSharer
import kotlin.coroutines.cancellation.CancellationException

/**
 * Main activity for VLR Android app.
 * Uses shared Compose UI from the shared module.
 */
class MainActivity : ComponentActivity() {
  private val deepLinkHandler = AppDeepLinkHandler()

  override fun onCreate(savedInstanceState: Bundle?) {
    val splashTheme = syncLauncherSplashTheme()
    if (splashTheme != 0) setTheme(splashTheme)
    installSplashScreen()
    super.onCreate(savedInstanceState)
    normalizeLauncherIntent(intent)
    enableEdgeToEdge()
    LegacyWidgetRefreshScheduler.restore(applicationContext)
    if (savedInstanceState == null) {
      openDeepLink(intent)
    }

    setContent {
      CompositionLocalProvider(
        LocalViewModelObserver provides viewModelLeakObserver,
        LocalImageSharer provides rememberAndroidImageSharer(),
        LocalNotificationPermissionProvider provides rememberAndroidNotificationPermissionProvider(),
      ) {
        App(
          deepLinkHandler = deepLinkHandler,
          pushTokenProvider = (application as VlrApplication).pushTokenProvider,
          onWidgetSnapshotChanged = { snapshotJson ->
            try {
              if (WidgetSnapshotStore.writeIfChanged(applicationContext, snapshotJson)) {
                FavoriteMatchWidgets.updateAll(applicationContext)
                ScoreWidget().updateAll(applicationContext)
              }
            } catch (error: CancellationException) {
              throw error
            } catch (error: Exception) {
              Log.w("UpcomingWidget", "Could not update widget instances", error)
            }
          },
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    normalizeLauncherIntent(intent)
    openDeepLink(intent)
  }

  private fun normalizeLauncherIntent(intent: Intent) {
    if (intent.action != Intent.ACTION_MAIN ||
      !intent.hasCategory(Intent.CATEGORY_LAUNCHER) ||
      intent.component?.className == MainActivity::class.java.name
    ) return
    startActivity(
      Intent(intent).setClass(this, MainActivity::class.java).setFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
      ),
    )
  }

  private fun openDeepLink(intent: Intent) {
    intent.dataString?.let(deepLinkHandler::openUrl)
  }
}
