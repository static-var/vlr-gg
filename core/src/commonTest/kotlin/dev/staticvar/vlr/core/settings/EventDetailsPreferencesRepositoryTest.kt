/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.MapSettings
import dev.staticvar.vlr.core.settings.EventDetailsSection.Matches
import dev.staticvar.vlr.core.settings.EventDetailsSection.Participants
import dev.staticvar.vlr.core.settings.EventDetailsSection.Prizes
import dev.staticvar.vlr.core.settings.EventDetailsSection.Standings
import kotlin.test.Test
import kotlin.test.assertEquals

class EventDetailsPreferencesRepositoryTest {
  @Test
  fun freshInstallShowsEverySectionInDefaultOrder() {
    val preferences = EventDetailsPreferencesRepository(MapSettings()).preferences.value

    assertEquals(listOf(Participants, Prizes, Standings, Matches), preferences.sectionOrder)
    assertEquals(emptySet(), preferences.hiddenSections)
  }

  @Test
  fun orderAndVisibilitySurviveRestartWithoutTouchingOtherSettings() {
    val storage = MapSettings()
    val matchDetails = MatchDetailsPreferencesRepository(storage)
    matchDetails.setPreferences(MatchDetailsPreferences(showMedia = false))
    val repository = EventDetailsPreferencesRepository(storage)
    val preferences = EventDetailsPreferences(
      sectionOrder = listOf(Matches, Standings, Participants, Prizes),
      hiddenSections = setOf(Prizes, Standings),
    )

    repository.setPreferences(preferences)

    assertEquals(preferences, repository.preferences.value)
    assertEquals(preferences, EventDetailsPreferencesRepository(storage).preferences.value)
    assertEquals(
      MatchDetailsPreferences(showMedia = false),
      MatchDetailsPreferencesRepository(storage).preferences.value,
    )
  }

  @Test
  fun showingEverySectionAgainPersistsAnEmptyHiddenSet() {
    val storage = MapSettings()
    val repository = EventDetailsPreferencesRepository(storage)
    repository.setPreferences(EventDetailsPreferences(hiddenSections = setOf(Matches)))

    repository.setPreferences(repository.preferences.value.withVisibility(Matches, visible = true))

    assertEquals(EventDetailsPreferences(), EventDetailsPreferencesRepository(storage).preferences.value)
  }

  @Test
  fun savedOrderDropsUnknownAndDuplicateNamesAndAppendsMissingSections() {
    val storage = MapSettings()
    storage.putString("event_details.section_order", "Matches,Bracket,Matches,Prizes")
    storage.putString("event_details.hidden_sections", "Bracket,Standings")

    val preferences = EventDetailsPreferencesRepository(storage).preferences.value

    assertEquals(listOf(Matches, Prizes, Participants, Standings), preferences.sectionOrder)
    assertEquals(setOf(Standings), preferences.hiddenSections)
  }

  @Test
  fun movingASectionKeepsEverySectionAndItsVisibility() {
    val preferences = EventDetailsPreferences(hiddenSections = setOf(Prizes))

    assertEquals(listOf(Participants, Standings, Prizes, Matches), preferences.moved(Prizes, 2).sectionOrder)
    assertEquals(listOf(Prizes, Participants, Standings, Matches), preferences.moved(Prizes, 0).sectionOrder)
    assertEquals(listOf(Participants, Standings, Matches, Prizes), preferences.moved(Prizes, 9).sectionOrder)
    assertEquals(setOf(Prizes), preferences.moved(Prizes, 3).hiddenSections)
  }

  @Test
  fun hidingAnUnavailableSectionIsRememberedForLaterEvents() {
    val storage = MapSettings()
    val repository = EventDetailsPreferencesRepository(storage)

    repository.setPreferences(EventDetailsPreferences().withVisibility(Standings, visible = false))

    val restored = EventDetailsPreferencesRepository(storage).preferences.value
    assertEquals(false, restored.isVisible(Standings))
    assertEquals(true, restored.isVisible(Matches))
  }
}
