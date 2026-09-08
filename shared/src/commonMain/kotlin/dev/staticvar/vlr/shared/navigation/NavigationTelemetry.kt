/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import dev.staticvar.vlr.core.telemetry.AppTelemetry
import dev.staticvar.vlr.core.telemetry.TelemetryReporter

internal class NavigationTelemetry(private val reporter: TelemetryReporter = AppTelemetry) {
  private var previousRoute: AppRoute? = null

  fun show(route: AppRoute?) {
    if (route == null || route == previousRoute) return
    previousRoute = route
    reporter.breadcrumb(category = "navigation", message = route.telemetryScreenName)
  }
}

internal val AppRoute.telemetryScreenName: String
  get() = when (this) {
    AppRoute.News -> "News"
    AppRoute.Matches -> "Matches"
    AppRoute.Events -> "Events"
    AppRoute.Rankings -> "Rankings"
    AppRoute.About -> "About"
    AppRoute.Settings -> "Settings"
    is AppRoute.MatchDetails -> "MatchDetails"
    is AppRoute.EventDetails -> "EventDetails"
    is AppRoute.NewsArticle -> "NewsArticle"
    is AppRoute.TeamDetails -> "TeamDetails"
    is AppRoute.PlayerDetails -> "PlayerDetails"
  }
