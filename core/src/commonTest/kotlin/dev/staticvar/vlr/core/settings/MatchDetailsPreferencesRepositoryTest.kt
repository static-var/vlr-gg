/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.MapSettings
import dev.staticvar.vlr.core.settings.MatchDetailsSection.Breakdown
import dev.staticvar.vlr.core.settings.MatchDetailsSection.HeadToHead
import dev.staticvar.vlr.core.settings.MatchDetailsSection.Media
import dev.staticvar.vlr.core.settings.MatchDetailsSection.Insights
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchDetailsPreferencesRepositoryTest {
  @Test
  fun freshInstallShowsEverySectionInPageOrder() {
    val preferences = MatchDetailsPreferencesRepository(MapSettings()).preferences.value

    assertEquals(MatchDetailsPreferences(), preferences)
    assertEquals(listOf(Insights, Breakdown, HeadToHead, Media), preferences.sectionOrder)
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
          for (showInsights in listOf(true, false)) {
            val preferences = MatchDetailsPreferences(showBreakdown, showMedia, showHeadToHead, showInsights)
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
    storage.putBoolean("match_details.show_insights", false)

    val preferences = MatchDetailsPreferencesRepository(storage).preferences.value

    assertEquals(listOf(Insights, Breakdown, HeadToHead, Media), preferences.sectionOrder)
    assertEquals(listOf(false, false, true, true), preferences.orderedSections.map(preferences::isVisible))
  }

  @Test
  fun orderAndVisibilitySurviveRestartInTheExistingKeys() {
    val storage = MapSettings()
    val preferences = MatchDetailsPreferences(sectionOrder = listOf(Media, HeadToHead, Insights, Breakdown))
      .withVisibility(HeadToHead, visible = false)

    MatchDetailsPreferencesRepository(storage).setPreferences(preferences)

    assertEquals(preferences, MatchDetailsPreferencesRepository(storage).preferences.value)
    assertEquals(false, storage.getBoolean("match_details.show_head_to_head", true))
    assertEquals("Media,HeadToHead,Insights,Breakdown", storage.getStringOrNull("match_details.section_order"))
  }

  @Test
  fun savedOrderDropsUnknownAndDuplicateNamesAndAppendsMissingSections() {
    val storage = MapSettings()
    storage.putString("match_details.section_order", "Media,Comments,Media,HeadToHead")

    val preferences = MatchDetailsPreferencesRepository(storage).preferences.value

    assertEquals(listOf(Media, HeadToHead, Insights, Breakdown), preferences.sectionOrder)
  }

  @Test
  fun aPartialOrderIsSavedComplete() {
    val storage = MapSettings()
    val repository = MatchDetailsPreferencesRepository(storage)

    repository.setPreferences(MatchDetailsPreferences(sectionOrder = listOf(Media)))

    assertEquals(listOf(Media, Insights, Breakdown, HeadToHead), repository.preferences.value.sectionOrder)
    assertEquals("Media,Insights,Breakdown,HeadToHead", storage.getStringOrNull("match_details.section_order"))
  }

  @Test
  fun movingASectionKeepsEverySectionAndEveryVisibilityFlag() {
    val preferences = MatchDetailsPreferences(showBreakdown = false, showInsights = false)

    assertEquals(listOf(Insights, HeadToHead, Breakdown, Media), preferences.moved(Breakdown, 2).sectionOrder)
    assertEquals(listOf(Media, Insights, Breakdown, HeadToHead), preferences.moved(Media, 0).sectionOrder)
    assertEquals(listOf(Breakdown, HeadToHead, Media, Insights), preferences.moved(Insights, 9).sectionOrder)
    assertEquals(listOf(Breakdown, Insights, HeadToHead, Media), preferences.moved(Breakdown, -3).sectionOrder)
    val moved = preferences.moved(Breakdown, 3)
    assertEquals(
      listOf(false, false, true, true),
      listOf(moved.showInsights, moved.showBreakdown, moved.showHeadToHead, moved.showMedia),
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
