/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.cache

import com.russhwolf.settings.MapSettings
import dev.staticvar.vlr.domain.model.MatchVeto
import dev.staticvar.vlr.domain.model.VetoAction
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalizedContentStoreTest {

  @Test
  fun `region label follows current locale and keeps canonical keys separate`() = runTest {
    var language = "hi-IN"
    val store = LocalizedRegionLabelStore(MapSettings()) { language }
    store.put("hi", mapOf("North America" to "उत्तरी अमेरिका"))
    store.put("de", mapOf("North America" to "Nordamerika"))

    assertEquals("उत्तरी अमेरिका", store.label("North America"))
    assertEquals("", store.label("Europe"))

    language = "de-DE"
    assertEquals("Nordamerika", store.label("North America"))
  }

  @Test
  fun `region label is not stored without confirmed content language`() = runTest {
    val store = LocalizedRegionLabelStore(MapSettings()) { "fr" }

    store.put(null, mapOf("Europe" to "Europe"))

    assertEquals("", store.label("Europe"))
  }

  @Test
  fun `veto is returned only for the raw notes that produced it`() = runTest {
    val store = MatchVetoStore(MapSettings())
    val veto = listOf(MatchVeto(team = "FNC", action = VetoAction.BAN, map = "Bind"))
    store.put("123", rawBans = listOf("FNC ban Bind"), veto = veto)

    assertEquals(veto, store.get("123", rawBans = listOf("FNC ban Bind")))
    assertEquals(emptyList(), store.get("123", rawBans = listOf("FNC pick Bind")))
  }

  @Test
  fun `unknown veto keeps the complete raw note`() = runTest {
    val store = MatchVetoStore(MapSettings())
    val veto = listOf(MatchVeto(team = null, action = VetoAction.UNKNOWN, map = "Map ban: Bind, Haven"))
    store.put("123", rawBans = listOf("Map ban: Bind, Haven"), veto = veto)

    assertEquals(veto, store.get("123", rawBans = listOf("Map ban: Bind, Haven")))
  }

  @Test
  fun `concurrent distinct writes are retained`() = runTest {
    val regionStore = LocalizedRegionLabelStore(MapSettings()) { "en" }
    val vetoStore = MatchVetoStore(MapSettings())

    listOf("North America", "Europe", "Pacific").map { region ->
      async { regionStore.put("en", mapOf(region to "$region label")) }
    }.awaitAll()
    (1..20).map { id ->
      async {
        vetoStore.put(
          matchId = id.toString(),
          rawBans = listOf("$id ban Bind"),
          veto = listOf(MatchVeto(id.toString(), VetoAction.BAN, "Bind")),
        )
      }
    }.awaitAll()

    assertEquals("Europe label", regionStore.label("Europe"))
    assertEquals("20", vetoStore.get("20", listOf("20 ban Bind")).single().team)
  }

  @Test
  fun `veto cache evicts the oldest entry`() = runTest {
    val store = MatchVetoStore(MapSettings())
    (1..101).forEach { id ->
      store.put(
        matchId = id.toString(),
        rawBans = listOf("note $id"),
        veto = listOf(MatchVeto(null, VetoAction.UNKNOWN, "note $id")),
      )
    }

    assertEquals(emptyList(), store.get("1", listOf("note 1")))
    assertEquals("note 101", store.get("101", listOf("note 101")).single().map)
  }

  @Test
  fun `corrupt persisted content falls back to empty`() = runTest {
    val settings = MapSettings()
    settings.putString("localized_content.region_labels.languages", "en")
    settings.putString("localized_content.region_labels.en", "not json")
    settings.putString("localized_content.match_veto", "not json")

    assertEquals("", LocalizedRegionLabelStore(settings) { "en" }.label("Europe"))
    assertEquals(emptyList(), MatchVetoStore(settings).get("123", listOf("note")))
  }
}
