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

  /** Swaps [section] with the nearest earlier section in [available]. Unavailable sections keep their slots. */
  public fun movedUp(section: EventDetailsSection, available: Set<EventDetailsSection>): EventDetailsPreferences =
    moved(section, step = -1, available = available)

  /** Swaps [section] with the nearest later section in [available]. Unavailable sections keep their slots. */
  public fun movedDown(section: EventDetailsSection, available: Set<EventDetailsSection>): EventDetailsPreferences =
    moved(section, step = 1, available = available)

  private fun moved(
    section: EventDetailsSection,
    step: Int,
    available: Set<EventDetailsSection>,
  ): EventDetailsPreferences {
    val order = orderedSections.toMutableList()
    val from = order.indexOf(section)
    if (section !in available) return copy(sectionOrder = order)
    var to = from + step
    while (to in order.indices && order[to] !in available) to += step
    if (to in order.indices) {
      order[from] = order[to]
      order[to] = section
    }
    return copy(sectionOrder = order)
  }
}
