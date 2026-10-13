/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

/** Sections shown below the event hero, declared in their default order. */
public enum class EventDetailsSection {
  Participants,
  Prizes,
  Standings,
  Matches,
}

/**
 * Global layout of the event details screen.
 *
 * [sectionOrder] and [hiddenSections] cover every section, including ones the current event has no
 * data for, so a section that appears later still lands where the user put it.
 */
public data class EventDetailsPreferences(
  val sectionOrder: List<EventDetailsSection> = EventDetailsSection.entries,
  val hiddenSections: Set<EventDetailsSection> = emptySet(),
) {
  /** Every section exactly once: saved positions first, then sections the saved order did not know. */
  public val orderedSections: List<EventDetailsSection>
    get() = (sectionOrder + EventDetailsSection.entries).distinct()

  public fun isVisible(section: EventDetailsSection): Boolean = section !in hiddenSections

  public fun withVisibility(section: EventDetailsSection, visible: Boolean): EventDetailsPreferences =
    copy(hiddenSections = if (visible) hiddenSections - section else hiddenSections + section)

  /** Moves [section] to [index] of [orderedSections], clamped to the list bounds. */
  public fun moved(section: EventDetailsSection, index: Int): EventDetailsPreferences {
    val order = orderedSections - section
    return copy(sectionOrder = order.toMutableList().apply { add(index.coerceIn(0, order.size), section) })
  }
}
