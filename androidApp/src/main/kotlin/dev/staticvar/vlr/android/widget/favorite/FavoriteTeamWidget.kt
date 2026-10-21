/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import androidx.compose.runtime.Composable
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetFrame
import dev.staticvar.vlr.shared.widget.favorite.WidgetKind

internal class FavoriteTeamWidget : FavoriteEntityWidget(WidgetKind.Team) {
  @Composable
  override fun Content(frame: FavoriteWidgetFrame, logos: WidgetBitmaps) {
    TeamWidgetLayout(frame, logos)
  }
}

internal class FavoriteTeamWidgetReceiver : FavoriteEntityWidgetReceiver() {
  override val glanceAppWidget: FavoriteTeamWidget = FavoriteTeamWidget()
}
