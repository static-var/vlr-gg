/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

/** Sections shown below the match hero, declared in their default order. */
public enum class MatchDetailsSection {
  Insights,
  Breakdown,
  HeadToHead,
  Media,
}

/**
 * Global layout of the match details screen.
 *
 * [sectionOrder] covers every section, including ones the current match has no data for, so a
 * section that appears later still lands where the user put it. Visibility stays in one flag per
 * section.
 */
public data class MatchDetailsPreferences(
  val showBreakdown: Boolean = true,
  val showMedia: Boolean = true,
  val showHeadToHead: Boolean = true,
  val showInsights: Boolean = true,
  val sectionOrder: List<MatchDetailsSection> = MatchDetailsSection.entries,
) {
  /** Every section exactly once: saved positions first, then sections the saved order did not know. */
  public val orderedSections: List<MatchDetailsSection>
    get() = (sectionOrder + MatchDetailsSection.entries).distinct()

  public fun isVisible(section: MatchDetailsSection): Boolean = when (section) {
    MatchDetailsSection.Insights -> showInsights
    MatchDetailsSection.Breakdown -> showBreakdown
    MatchDetailsSection.HeadToHead -> showHeadToHead
    MatchDetailsSection.Media -> showMedia
  }

  public fun withVisibility(section: MatchDetailsSection, visible: Boolean): MatchDetailsPreferences = when (section) {
    MatchDetailsSection.Insights -> copy(showInsights = visible)
    MatchDetailsSection.Breakdown -> copy(showBreakdown = visible)
    MatchDetailsSection.HeadToHead -> copy(showHeadToHead = visible)
    MatchDetailsSection.Media -> copy(showMedia = visible)
  }

  /** Moves [section] to [index] of [orderedSections], clamped to the list bounds. */
  public fun moved(section: MatchDetailsSection, index: Int): MatchDetailsPreferences {
    val order = orderedSections - section
    return copy(sectionOrder = order.toMutableList().apply { add(index.coerceIn(0, order.size), section) })
  }
}
