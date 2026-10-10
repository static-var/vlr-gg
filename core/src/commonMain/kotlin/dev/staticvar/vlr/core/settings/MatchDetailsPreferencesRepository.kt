/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public class MatchDetailsPreferencesRepository(private val storage: Settings) {
  public val preferences: StateFlow<MatchDetailsPreferences>
    field = MutableStateFlow(
      MatchDetailsPreferences(
        showBreakdown = storage.getBoolean(BreakdownKey, true),
        showMedia = storage.getBoolean(MediaKey, true),
        showHeadToHead = storage.getBoolean(HeadToHeadKey, true),
        showPrediction = storage.getBoolean(PredictionKey, true),
        sectionOrder = storage.getStringOrNull(SectionOrderKey)?.toSections() ?: MatchDetailsSection.entries,
      ).let { saved -> saved.copy(sectionOrder = saved.orderedSections) },
    )

  public fun setPreferences(preferences: MatchDetailsPreferences) {
    val normalized = preferences.copy(sectionOrder = preferences.orderedSections)
    storage.putBoolean(BreakdownKey, normalized.showBreakdown)
    storage.putBoolean(MediaKey, normalized.showMedia)
    storage.putBoolean(HeadToHeadKey, normalized.showHeadToHead)
    storage.putBoolean(PredictionKey, normalized.showPrediction)
    storage.putString(SectionOrderKey, normalized.sectionOrder.joinToString(Separator) { it.name })
    this.preferences.value = normalized
  }

  private companion object {
    const val BreakdownKey: String = "match_details.show_breakdown"
    const val MediaKey: String = "match_details.show_media"
    const val PredictionKey: String = "match_details.show_prediction"
    const val HeadToHeadKey: String = "match_details.show_head_to_head"
    const val SectionOrderKey: String = "match_details.section_order"
    const val Separator: String = ","

    // Names from a newer or older build that no longer exist are dropped.
    fun String.toSections(): List<MatchDetailsSection> = split(Separator)
      .mapNotNull { name -> MatchDetailsSection.entries.firstOrNull { it.name == name.trim() } }
  }
}
