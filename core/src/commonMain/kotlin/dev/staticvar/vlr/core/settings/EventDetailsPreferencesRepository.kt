/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public class EventDetailsPreferencesRepository(private val storage: Settings) {
  public val preferences: StateFlow<EventDetailsPreferences>
    field = MutableStateFlow(
      EventDetailsPreferences(
        sectionOrder = storage.getStringOrNull(SectionOrderKey)?.toSections() ?: EventDetailsSection.entries,
        hiddenSections = storage.getStringOrNull(HiddenSectionsKey)?.toSections()?.toSet().orEmpty(),
      ).let { saved -> saved.copy(sectionOrder = saved.orderedSections) },
    )

  public fun setPreferences(preferences: EventDetailsPreferences) {
    val normalized = preferences.copy(sectionOrder = preferences.orderedSections)
    storage.putString(SectionOrderKey, normalized.sectionOrder.joinToString(Separator) { it.name })
    storage.putString(HiddenSectionsKey, normalized.hiddenSections.joinToString(Separator) { it.name })
    this.preferences.value = normalized
  }

  private companion object {
    const val SectionOrderKey: String = "event_details.section_order"
    const val HiddenSectionsKey: String = "event_details.hidden_sections"
    const val Separator: String = ","

    // Names from a newer or older build that no longer exist are dropped.
    fun String.toSections(): List<EventDetailsSection> = split(Separator)
      .mapNotNull { name -> EventDetailsSection.entries.firstOrNull { it.name == name.trim() } }
  }
}
