/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppDeepLinkHandlerTest {
  @Test
  fun numericMatchUrlCreatesMatchRequest() {
    val handler = AppDeepLinkHandler()

    assertTrue(handler.openUrl("https://valesports.app/match/11757778"))

    assertEquals(
      AppDeepLinkRequest(
        id = 1L,
        destination = AppDeepLinkDestination.Match(matchId = "11757778"),
      ),
      handler.state.value.pendingRequest,
    )
  }

  @Test
  fun favoriteDetailLinksNavigateFromColdAndWarmState() {
    val destinations = listOf(
      "https://valesports.app/team/12" to AppRoute.TeamDetails("12"),
      "https://valesports.app/event/34?source=spotlight" to AppRoute.EventDetails("34"),
      "https://valesports.app/player/56#details" to AppRoute.PlayerDetails("56"),
      "https://valesports.app/match/78" to AppRoute.MatchDetails("78"),
    )
    for ((url, destination) in destinations) {
      val handler = AppDeepLinkHandler()
      assertTrue(handler.openUrl(url))
      val coldApp = VlrAppState(mutableListOf<NavKey>(AppRoute.Home))
      val coldRequest = handler.state.value.pendingRequest!!
      coldRequest.navigate(coldApp)
      handler.consume(coldRequest.id)
      assertEquals(listOf<NavKey>(AppRoute.Home, destination), coldApp.backStack)
      assertNull(handler.state.value.pendingRequest)

      val warmApp = VlrAppState(mutableListOf<NavKey>(AppRoute.Home, AppRoute.Events))
      assertTrue(handler.openUrl(url))
      handler.state.value.pendingRequest!!.navigate(warmApp)
      assertEquals(listOf<NavKey>(AppRoute.Home, AppRoute.Events, destination), warmApp.backStack)
    }
  }

  @Test
  fun homeUrlCreatesHomeRequest() {
    val handler = AppDeepLinkHandler()

    assertTrue(handler.openUrl("https://valesports.app/"))

    assertEquals(
      AppDeepLinkRequest(id = 1L, destination = AppDeepLinkDestination.Home),
      handler.state.value.pendingRequest,
    )
  }

  @Test
  fun existingSharedLinksStillOpen() {
    val handler = AppDeepLinkHandler()

    assertTrue(handler.openUrl("https://valorantesports.staticvar.dev/match/123"))
    assertEquals(AppDeepLinkDestination.Match("123"), handler.state.value.pendingRequest?.destination)

    assertTrue(handler.openUrl("https://valorantesports.staticvar.dev/"))
    assertEquals(AppDeepLinkDestination.Home, handler.state.value.pendingRequest?.destination)
  }

  @Test
  fun unsupportedUrlsDoNotReplaceTheLatestRequest() {
    val handler = AppDeepLinkHandler()
    handler.openUrl("https://valesports.app/match/11757778")
    val acceptedRequest = handler.state.value.pendingRequest

    val unsupportedUrls = listOf(
      "https://valesports.app/match/not-a-number",
      "https://valesports.app/match/",
      "https://valesports.app/match/11757778/more",
      "https://valesports.app/news/11757778",
      "https://valesports.app/team/not-a-number",
      "https://valesports.app/event/",
      "https://valesports.app/player/123/more",
      "https://valesports.app.evil.example/match/123",
      "https://valorantesports.staticvar.dev.evil.example/match/123",
      "http://valesports.app/match/123",
      "vlr://match/123",
      "https://vlr.gg/11757778",
    )

    for (url in unsupportedUrls) {
      assertFalse(handler.openUrl(url))
      assertEquals(acceptedRequest, handler.state.value.pendingRequest)
    }
  }

  @Test
  fun repeatedMatchUrlCreatesAFreshRequestAndNavigatesAgain() {
    val handler = AppDeepLinkHandler()
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home))

    handler.openUrl("https://valesports.app/match/11757778")
    val firstRequest = handler.state.value.pendingRequest
    firstRequest?.navigate(appState)
    firstRequest?.let { handler.consume(it.id) }
    assertEquals(AppRoute.MatchDetails("11757778"), appState.backStack.last())

    appState.navigateUp()
    assertEquals(AppRoute.Home, appState.backStack.last())
    assertNull(handler.state.value.pendingRequest)

    handler.openUrl("https://valesports.app/match/11757778")
    val repeatedRequest = handler.state.value.pendingRequest
    repeatedRequest?.navigate(appState)

    assertEquals(2L, repeatedRequest?.id)
    assertEquals(AppRoute.MatchDetails("11757778"), appState.backStack.last())
  }

  @Test
  fun matchRequestPreservesTheSelectedRoot() {
    val handler = AppDeepLinkHandler()
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home, AppRoute.Events))

    handler.openUrl("https://valesports.app/match/11757778")
    handler.state.value.pendingRequest?.navigate(appState)

    assertEquals(
      listOf<NavKey>(AppRoute.Home, AppRoute.Events, AppRoute.MatchDetails("11757778")),
      appState.backStack,
    )
  }

  @Test
  fun consumedRequestDoesNotReplayInARecreatedConsumer() {
    val handler = AppDeepLinkHandler()
    handler.openUrl("https://valesports.app/match/11757778")
    val request = handler.state.value.pendingRequest
    val firstAppState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home))
    request?.navigate(firstAppState)
    request?.let { handler.consume(it.id) }

    val recreatedAppState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home))
    handler.state.value.pendingRequest?.navigate(recreatedAppState)

    assertEquals(listOf<NavKey>(AppRoute.Home), recreatedAppState.backStack)
    assertEquals(1L, handler.state.value.lastIssuedId)
    assertNull(handler.state.value.pendingRequest)
  }
}
