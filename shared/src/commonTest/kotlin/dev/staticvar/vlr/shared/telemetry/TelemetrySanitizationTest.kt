/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelemetrySanitizationTest {
  @Test
  fun removesUrlQueriesAndFragmentsButPreservesTheEndpoint() {
    assertEquals(
      "Failed GET https://api.example.com/matches and https://example.com/team",
      sanitizeTelemetryText("Failed GET https://api.example.com/matches?token=secret#private and https://example.com/team#detail"),
    )
  }

  @Test
  fun removesCredentialsFromDiagnosticMessages() {
    val output = sanitizeTelemetryText("Authorization: Bearer secret-value password=hunter2 api_key=private")
    assertTrue("secret-value" !in output)
    assertTrue("hunter2" !in output)
    assertTrue("private" !in output)
    assertTrue(isSensitiveTelemetryKey("request.headers.Authorization"))
  }
}
