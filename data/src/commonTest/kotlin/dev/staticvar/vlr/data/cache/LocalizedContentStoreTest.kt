/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.cache

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalizedContentStoreTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun `region label follows current locale and keeps canonical keys separate`() = runTest {
    var language = "hi-IN"
    val store = LocalizedRegionLabelStore(MapSettings(), json) { language }
    store.put("hi", mapOf("North America" to "उत्तरी अमेरिका"))
    store.put("de", mapOf("North America" to "Nordamerika"))

    assertEquals("उत्तरी अमेरिका", store.label("North America"))
    assertEquals("", store.label("Europe"))

    language = "de-DE"
    assertEquals("Nordamerika", store.label("North America"))
  }

  @Test
  fun `region label is not stored without confirmed content language`() = runTest {
    val store = LocalizedRegionLabelStore(MapSettings(), json) { "fr" }

    store.put(null, mapOf("Europe" to "Europe"))

    assertEquals("", store.label("Europe"))
  }

  @Test
  fun `concurrent distinct writes are retained`() = runTest {
    val regionStore = LocalizedRegionLabelStore(MapSettings(), json) { "en" }

    listOf("North America", "Europe", "Pacific").map { region ->
      async { regionStore.put("en", mapOf(region to "$region label")) }
    }.awaitAll()
    assertEquals("Europe label", regionStore.label("Europe"))
  }

  @Test
  fun `corrupt persisted content falls back to empty`() = runTest {
    val settings = MapSettings()
    settings.putString("localized_content.region_labels.languages", "en")
    settings.putString("localized_content.region_labels.en", "not json")

    assertEquals("", LocalizedRegionLabelStore(settings, json) { "en" }.label("Europe"))
  }
}
