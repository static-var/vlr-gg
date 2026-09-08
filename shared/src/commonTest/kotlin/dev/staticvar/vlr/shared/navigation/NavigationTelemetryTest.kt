/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import dev.staticvar.vlr.core.telemetry.NoOpTelemetryReporter
import dev.staticvar.vlr.core.telemetry.TelemetryReporter
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationTelemetryTest {
  @Test
  fun routeChangesAreRecordedOnceWithoutDetailIds() {
    val breadcrumbs = mutableListOf<String>()
    val telemetry = NavigationTelemetry(object : TelemetryReporter by NoOpTelemetryReporter {
      override fun breadcrumb(category: String, message: String) {
        breadcrumbs += "$category $message"
      }
    })

    telemetry.show(null)
    telemetry.show(AppRoute.News)
    telemetry.show(AppRoute.News)
    telemetry.show(AppRoute.PlayerDetails("private-id-1"))
    telemetry.show(AppRoute.PlayerDetails("private-id-1"))
    telemetry.show(AppRoute.PlayerDetails("private-id-2"))
    telemetry.show(AppRoute.News)

    assertEquals(
      listOf("navigation News", "navigation PlayerDetails", "navigation PlayerDetails", "navigation News"),
      breadcrumbs,
    )
  }

  @Test
  fun everyRouteHasAStableScreenName() {
    val routes = listOf(
      AppRoute.News, AppRoute.Matches, AppRoute.Events, AppRoute.Rankings, AppRoute.About, AppRoute.Settings,
      AppRoute.MatchDetails("secret"), AppRoute.EventDetails("secret"), AppRoute.NewsArticle("secret"),
      AppRoute.TeamDetails("secret"), AppRoute.PlayerDetails("secret"),
    )
    assertEquals(
      listOf(
        "News", "Matches", "Events", "Rankings", "About", "Settings", "MatchDetails", "EventDetails",
        "NewsArticle", "TeamDetails", "PlayerDetails",
      ),
      routes.map { it.telemetryScreenName },
    )
  }
}
