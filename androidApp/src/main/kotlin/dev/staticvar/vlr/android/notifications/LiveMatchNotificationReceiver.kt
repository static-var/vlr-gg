/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.staticvar.vlr.android.VlrApplication

internal class LiveMatchNotificationReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ActionDismiss && intent.action != ActionUnpin) return
    val matchId = intent.getStringExtra(ExtraMatchId) ?: return
    (context.applicationContext as? VlrApplication)?.liveMatchNotifications?.dismiss(matchId)
  }

  internal companion object {
    const val ActionDismiss = "dev.staticvar.vlr.action.DISMISS_LIVE_MATCH"
    const val ActionUnpin = "dev.staticvar.vlr.action.UNPIN_LIVE_MATCH"
    const val ExtraMatchId = "match_id"
  }
}
