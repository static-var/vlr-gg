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
    telemetry.show(AppRoute.Rankings)
    telemetry.show(AppRoute.Rankings)
    telemetry.show(AppRoute.PlayerDetails("private-id-1"))
    telemetry.show(AppRoute.PlayerDetails("private-id-1"))
    telemetry.show(AppRoute.PlayerDetails("private-id-2"))
    telemetry.show(AppRoute.Rankings)

    assertEquals(
      listOf("navigation Rankings", "navigation PlayerDetails", "navigation PlayerDetails", "navigation Rankings"),
      breadcrumbs,
    )
  }

  @Test
  fun everyRouteHasAStableScreenName() {
    val routes = listOf(
      AppRoute.Home, AppRoute.Matches, AppRoute.Events, AppRoute.Rankings, AppRoute.About, AppRoute.Settings, AppRoute.WhatsNew, AppRoute.DeveloperNote,
      AppRoute.MatchDetails("secret"), AppRoute.EventDetails("secret"),
      AppRoute.TeamDetails("secret"), AppRoute.PlayerDetails("secret"),
    )
    assertEquals(
      listOf(
        "Home", "Matches", "Events", "Rankings", "About", "Settings", "WhatsNew", "DeveloperNote", "MatchDetails", "EventDetails",
        "TeamDetails", "PlayerDetails",
      ),
      routes.map { it.telemetryScreenName },
    )
  }
}
