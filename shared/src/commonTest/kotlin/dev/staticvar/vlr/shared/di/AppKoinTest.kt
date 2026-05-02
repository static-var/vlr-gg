/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.di

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AppKoinTest {
  @Test
  fun `buildDefaultNetworkHeaders always includes application header`() {
    val headers = buildDefaultNetworkHeaders(authToken = null)

    assertEquals("dev.staticvar.vlr", headers["app-name"])
    assertFalse(headers.containsKey("Authorization"))
  }

  @Test
  fun `buildDefaultNetworkHeaders adds normalized authorization header when token provided`() {
    val headers = buildDefaultNetworkHeaders(authToken = "  \"secret-token\"  ")

    assertEquals("dev.staticvar.vlr", headers["app-name"])
    assertEquals("secret-token", headers["Authorization"])
  }
}
