/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.MapSettings
import dev.staticvar.vlr.core.settings.MatchDetailsSection.Breakdown
import dev.staticvar.vlr.core.settings.MatchDetailsSection.HeadToHead
import dev.staticvar.vlr.core.settings.MatchDetailsSection.Media
import dev.staticvar.vlr.core.settings.MatchDetailsSection.Prediction
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchDetailsPreferencesRepositoryTest {
  @Test
  fun freshInstallShowsEverySectionInPageOrder() {
    val preferences = MatchDetailsPreferencesRepository(MapSettings()).preferences.value

    assertEquals(MatchDetailsPreferences(), preferences)
    assertEquals(listOf(Prediction, Breakdown, HeadToHead, Media), preferences.sectionOrder)
  }

  @Test
  fun everyCombinationSurvivesRestartWithoutChangingAppearance() {
    val storage = MapSettings()
    val appearance = AppearanceRepository(storage)
    appearance.setFamily(ThemeFamily.Catppuccin)
    appearance.setCatppuccinFlavour(CatppuccinFlavour.Latte)
    val repository = MatchDetailsPreferencesRepository(storage)

    for (showBreakdown in listOf(true, false)) {
      for (showMedia in listOf(true, false)) {
        for (showHeadToHead in listOf(true, false)) {
          for (showPrediction in listOf(true, false)) {
            val preferences = MatchDetailsPreferences(showBreakdown, showMedia, showHeadToHead, showPrediction)
            repository.setPreferences(preferences)
            assertEquals(preferences, repository.preferences.value)
            assertEquals(preferences, MatchDetailsPreferencesRepository(storage).preferences.value)
            assertEquals(appearance.settings.value, AppearanceRepository(storage).settings.value)
          }
        }
      }
    }
  }

  @Test
  fun unsetSectionKeysRemainVisibleWhenOtherChoicesAlreadyExist() {
    val storage = MapSettings()
    storage.putBoolean("match_details.show_media", false)
    assertEquals(
      MatchDetailsPreferences(showMedia = false),
      MatchDetailsPreferencesRepository(storage).preferences.value,
    )
  }

  @Test
  fun visibilityChosenBeforeOrderingExistedKeepsTheDefaultOrder() {
    val storage = MapSettings()
    storage.putBoolean("match_details.show_breakdown", false)
    storage.putBoolean("match_details.show_prediction", false)

    val preferences = MatchDetailsPreferencesRepository(storage).preferences.value

    assertEquals(listOf(Prediction, Breakdown, HeadToHead, Media), preferences.sectionOrder)
    assertEquals(listOf(false, false, true, true), preferences.orderedSections.map(preferences::isVisible))
  }

  @Test
  fun orderAndVisibilitySurviveRestartInTheExistingKeys() {
    val storage = MapSettings()
    val preferences = MatchDetailsPreferences(sectionOrder = listOf(Media, HeadToHead, Prediction, Breakdown))
      .withVisibility(HeadToHead, visible = false)

    MatchDetailsPreferencesRepository(storage).setPreferences(preferences)

    assertEquals(preferences, MatchDetailsPreferencesRepository(storage).preferences.value)
    assertEquals(false, storage.getBoolean("match_details.show_head_to_head", true))
    assertEquals("Media,HeadToHead,Prediction,Breakdown", storage.getStringOrNull("match_details.section_order"))
  }

  @Test
  fun savedOrderDropsUnknownAndDuplicateNamesAndAppendsMissingSections() {
    val storage = MapSettings()
    storage.putString("match_details.section_order", "Media,Comments,Media,HeadToHead")

    val preferences = MatchDetailsPreferencesRepository(storage).preferences.value

    assertEquals(listOf(Media, HeadToHead, Prediction, Breakdown), preferences.sectionOrder)
  }

  @Test
  fun aPartialOrderIsSavedComplete() {
    val storage = MapSettings()
    val repository = MatchDetailsPreferencesRepository(storage)

    repository.setPreferences(MatchDetailsPreferences(sectionOrder = listOf(Media)))

    assertEquals(listOf(Media, Prediction, Breakdown, HeadToHead), repository.preferences.value.sectionOrder)
    assertEquals("Media,Prediction,Breakdown,HeadToHead", storage.getStringOrNull("match_details.section_order"))
  }

  @Test
  fun movingASectionKeepsEverySectionAndEveryVisibilityFlag() {
    val preferences = MatchDetailsPreferences(showBreakdown = false, showPrediction = false)

    assertEquals(listOf(Prediction, HeadToHead, Breakdown, Media), preferences.moved(Breakdown, 2).sectionOrder)
    assertEquals(listOf(Media, Prediction, Breakdown, HeadToHead), preferences.moved(Media, 0).sectionOrder)
    assertEquals(listOf(Breakdown, HeadToHead, Media, Prediction), preferences.moved(Prediction, 9).sectionOrder)
    assertEquals(listOf(Breakdown, Prediction, HeadToHead, Media), preferences.moved(Breakdown, -3).sectionOrder)
    val moved = preferences.moved(Breakdown, 3)
    assertEquals(
      listOf(false, false, true, true),
      listOf(moved.showPrediction, moved.showBreakdown, moved.showHeadToHead, moved.showMedia),
    )
  }

  @Test
  fun eachSectionTogglesOnlyItsOwnFlag() {
    MatchDetailsSection.entries.forEach { section ->
      val hidden = MatchDetailsPreferences().withVisibility(section, visible = false)

      assertEquals(MatchDetailsSection.entries - section, MatchDetailsSection.entries.filter(hidden::isVisible))
      assertEquals(MatchDetailsPreferences(), hidden.withVisibility(section, visible = true))
    }
  }
}
