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

    assertTrue(handler.openUrl("vlr://match/11757778"))

    assertEquals(
      AppDeepLinkRequest(
        id = 1L,
        destination = AppDeepLinkDestination.Match(matchId = "11757778"),
      ),
      handler.state.value.pendingRequest,
    )
  }

  @Test
  fun homeUrlCreatesHomeRequest() {
    val handler = AppDeepLinkHandler()

    assertTrue(handler.openUrl("vlr://home"))

    assertEquals(
      AppDeepLinkRequest(id = 1L, destination = AppDeepLinkDestination.Home),
      handler.state.value.pendingRequest,
    )
  }

  @Test
  fun unsupportedUrlsDoNotReplaceTheLatestRequest() {
    val handler = AppDeepLinkHandler()
    handler.openUrl("vlr://match/11757778")
    val acceptedRequest = handler.state.value.pendingRequest

    val unsupportedUrls = listOf(
      "vlr://match/not-a-number",
      "vlr://match/",
      "vlr://match/11757778/more",
      "vlr://event/11757778",
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

    handler.openUrl("vlr://match/11757778")
    val firstRequest = handler.state.value.pendingRequest
    firstRequest?.navigate(appState)
    firstRequest?.let { handler.consume(it.id) }
    assertEquals(AppRoute.MatchDetails("11757778"), appState.backStack.last())

    appState.navigateUp()
    assertEquals(AppRoute.Home, appState.backStack.last())
    assertNull(handler.state.value.pendingRequest)

    handler.openUrl("vlr://match/11757778")
    val repeatedRequest = handler.state.value.pendingRequest
    repeatedRequest?.navigate(appState)

    assertEquals(2L, repeatedRequest?.id)
    assertEquals(AppRoute.MatchDetails("11757778"), appState.backStack.last())
  }

  @Test
  fun matchRequestPreservesTheSelectedRoot() {
    val handler = AppDeepLinkHandler()
    val appState = VlrAppState(mutableListOf<NavKey>(AppRoute.Home, AppRoute.Events))

    handler.openUrl("vlr://match/11757778")
    handler.state.value.pendingRequest?.navigate(appState)

    assertEquals(
      listOf<NavKey>(AppRoute.Home, AppRoute.Events, AppRoute.MatchDetails("11757778")),
      appState.backStack,
    )
  }

  @Test
  fun consumedRequestDoesNotReplayInARecreatedConsumer() {
    val handler = AppDeepLinkHandler()
    handler.openUrl("vlr://match/11757778")
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
