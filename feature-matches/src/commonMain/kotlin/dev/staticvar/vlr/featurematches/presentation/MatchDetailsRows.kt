/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.core.settings.MatchDetailsPreferences
import dev.staticvar.vlr.core.settings.MatchDetailsSection
import dev.staticvar.vlr.domain.model.MatchDetails

/**
 * Sections this match can show. The options sheet disables the rest. A prediction that can still be
 * requested counts, so its loading and retry states keep their place.
 */
internal val MatchDetails.availableSections: Set<MatchDetailsSection>
  get() = buildSet {
    if (prediction != null || canRequestPrediction) add(MatchDetailsSection.Prediction)
    if (matchData.isNotEmpty()) add(MatchDetailsSection.Breakdown)
    if (head2head.isNotEmpty()) add(MatchDetailsSection.HeadToHead)
    if (videos.streams.isNotEmpty() || videos.vods.isNotEmpty()) add(MatchDetailsSection.Media)
  }

/** One item in the match details list below the hero. Keys stay stable across reorders and refreshes. */
internal enum class MatchDetailsRow(val key: String) {
  Prediction("prediction"),
  Breakdown("breakdown"),
  BreakdownEmpty("breakdown-empty"),
  HeadToHead("head-to-head"),
  MediaTitle("media-title"),
  Media("media"),
  BottomSpacer("bottom-spacer"),
}

/**
 * Builds the list below the hero from the user's section order and visibility. [showBreakdownEmpty]
 * puts the breakdown empty state in the breakdown's place while the match has nothing to show.
 */
internal fun buildMatchDetailsRows(
  match: MatchDetails,
  preferences: MatchDetailsPreferences,
  showBreakdownEmpty: Boolean,
): List<MatchDetailsRow> = buildList {
  val available = match.availableSections
  preferences.orderedSections
    .filter(preferences::isVisible)
    .forEach { section ->
      when (section) {
        MatchDetailsSection.Prediction -> if (section in available) add(MatchDetailsRow.Prediction)
        MatchDetailsSection.Breakdown -> when {
          section in available -> add(MatchDetailsRow.Breakdown)
          showBreakdownEmpty -> add(MatchDetailsRow.BreakdownEmpty)
        }

        MatchDetailsSection.HeadToHead -> if (section in available) add(MatchDetailsRow.HeadToHead)
        MatchDetailsSection.Media -> if (section in available) {
          add(MatchDetailsRow.MediaTitle)
          add(MatchDetailsRow.Media)
        }
      }
    }
  add(MatchDetailsRow.BottomSpacer)
}
