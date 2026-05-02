/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals

/**
 * Test-only helper to assert a set of wire tokens map to expected enum constants, including UNKNOWN fallback.
 */
object EnumFallbackAsserts {
  private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
  }

  /**
   * Provide a map of raw wire strings to expected enum constants. Each entry is asserted via provided serializer.
   */
  fun <T> assertMappings(serializer: KSerializer<T>, expectations: Map<String, T>) {
    expectations.forEach { (raw, expected) ->
      val decoded = json.decodeFromString(serializer, "\"$raw\"")
      assertEquals(expected, decoded, "Expected '$raw' to decode to $expected but got $decoded")
    }
  }

  fun <T> assertNullableMappings(serializer: KSerializer<T?>, expectations: Map<String, T?>) {
    expectations.forEach { (raw, expected) ->
      val decoded = json.decodeFromString(serializer, "\"$raw\"")
      assertEquals(expected, decoded, "Expected '$raw' to decode to $expected but got $decoded (nullable serializer)")
    }
  }
}
