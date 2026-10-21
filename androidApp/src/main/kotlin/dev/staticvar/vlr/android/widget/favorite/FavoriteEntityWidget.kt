/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.state.PreferencesGlanceStateDefinition
import dev.staticvar.vlr.shared.widget.favorite.WidgetKind

internal abstract class FavoriteEntityWidget(val kind: WidgetKind) : GlanceAppWidget() {
  final override val stateDefinition = PreferencesGlanceStateDefinition
}
