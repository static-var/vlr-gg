/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public class MatchDetailsPreferencesRepository(private val storage: Settings) {
  private val mutablePreferences = MutableStateFlow(
    MatchDetailsPreferences(
      showBreakdown = storage.getBoolean(BreakdownKey, true),
      showMedia = storage.getBoolean(MediaKey, true),
      showHeadToHead = storage.getBoolean(HeadToHeadKey, true),
    ),
  )
  public val preferences: StateFlow<MatchDetailsPreferences> = mutablePreferences.asStateFlow()

  public fun setPreferences(preferences: MatchDetailsPreferences) {
    storage.putBoolean(BreakdownKey, preferences.showBreakdown)
    storage.putBoolean(MediaKey, preferences.showMedia)
    storage.putBoolean(HeadToHeadKey, preferences.showHeadToHead)
    mutablePreferences.value = preferences
  }

  private companion object {
    const val BreakdownKey: String = "match_details.show_breakdown"
    const val MediaKey: String = "match_details.show_media"
    const val HeadToHeadKey: String = "match_details.show_head_to_head"
  }
}
