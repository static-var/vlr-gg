/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.core.settings.MatchDetailsPreferences
import dev.staticvar.vlr.core.settings.MatchDetailsSection.Breakdown
import dev.staticvar.vlr.core.settings.MatchDetailsSection.HeadToHead
import dev.staticvar.vlr.core.settings.MatchDetailsSection.Insights
import dev.staticvar.vlr.core.settings.MatchDetailsSection.Media
import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchInsights
import dev.staticvar.vlr.domain.model.MatchPrediction
import dev.staticvar.vlr.domain.model.MatchSummary
import dev.staticvar.vlr.domain.model.MatchVideos
import dev.staticvar.vlr.domain.model.PredictionSource
import dev.staticvar.vlr.domain.model.PreviousEncounter
import dev.staticvar.vlr.domain.model.VideoReference
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchDetailsLayoutTest {
  private val everything = match(
    insights = MatchInsights(prediction = MatchPrediction("a", "b", 0.6, 0.4, PredictionSource.MODEL)),
    maps = listOf(MapData("Lotus", emptyList(), emptyList(), emptyList())),
    encounters = listOf(PreviousEncounter("older", emptyList())),
    vods = listOf(VideoReference("Map 1", "youtube.com/watch")),
  )

  @Test
  fun defaultOrderMatchesThePageBeforeOrderingExisted() {
    assertEquals(
      listOf("insights", "breakdown", "head-to-head", "media-title", "media", "bottom-spacer"),
      buildMatchDetailsRows(everything, MatchDetailsPreferences(), showBreakdownEmpty = false).map(MatchDetailsRow::key),
    )
  }

  @Test
  fun sectionsFollowTheSavedOrderAndMediaStaysTogether() {
    val preferences = MatchDetailsPreferences(sectionOrder = listOf(Media, HeadToHead, Breakdown, Insights))

    assertEquals(
      listOf(
        MatchDetailsRow.MediaTitle,
        MatchDetailsRow.Media,
        MatchDetailsRow.HeadToHead,
        MatchDetailsRow.Breakdown,
        MatchDetailsRow.Insights,
        MatchDetailsRow.BottomSpacer,
      ),
      buildMatchDetailsRows(everything, preferences, showBreakdownEmpty = false),
    )
  }

  @Test
  fun hiddenSectionsLeaveTheBodyButKeepTheirPlace() {
    val preferences = MatchDetailsPreferences(showInsights = false, showMedia = false)
      .moved(Insights, 3)

    assertEquals(
      listOf(MatchDetailsRow.Breakdown, MatchDetailsRow.HeadToHead, MatchDetailsRow.BottomSpacer),
      buildMatchDetailsRows(everything, preferences, showBreakdownEmpty = false),
    )
    assertEquals(
      listOf(Breakdown, HeadToHead, Media, Insights),
      preferences.withVisibility(Insights, visible = true).orderedSections,
    )
  }

  @Test
  fun sectionsWithoutDataStayOutOfTheBodyAndCannotBeToggled() {
    val noMedia = everything.copy(videos = MatchVideos(emptyList(), emptyList()), head2head = emptyList())

    assertEquals(setOf(Insights, Breakdown), noMedia.availableSections)
    assertEquals(
      listOf(MatchDetailsRow.Insights, MatchDetailsRow.Breakdown, MatchDetailsRow.BottomSpacer),
      buildMatchDetailsRows(noMedia, MatchDetailsPreferences(), showBreakdownEmpty = false),
    )
  }

  @Test
  fun predictionOnlyAndSummaryOnlyInsightsAreAvailable() {
    for (available in listOf(
      match(insights = MatchInsights(prediction = MatchPrediction("a", "b", 0.6, 0.4, PredictionSource.MODEL))),
      match(insights = MatchInsights(preMatchSummary = MatchSummary("Pre-game analysis.", "2026-10-16T12:00:00Z"))),
      match(insights = MatchInsights(postMatchSummary = MatchSummary("Post-game analysis.", "2026-10-16T15:00:00Z")), status = "completed"),
    )) {
      assertEquals(setOf(Insights), available.availableSections)
      assertEquals(
        listOf(MatchDetailsRow.Insights, MatchDetailsRow.BottomSpacer),
        buildMatchDetailsRows(available, MatchDetailsPreferences(), showBreakdownEmpty = false),
      )
      assertEquals(
        listOf(MatchDetailsRow.BottomSpacer),
        buildMatchDetailsRows(available, MatchDetailsPreferences(showInsights = false), showBreakdownEmpty = false),
      )
    }
  }

  @Test
  fun missingInsightsAndSummariesForAnotherPhaseStayHidden() {
    val pre = MatchSummary("Pre-game analysis.", "2026-10-16T12:00:00Z")
    val post = MatchSummary("Post-game analysis.", "2026-10-16T15:00:00Z")
    for (match in listOf(
      match(),
      match(insights = MatchInsights()),
      match(insights = MatchInsights(postMatchSummary = post)),
      match(insights = MatchInsights(preMatchSummary = pre), status = "completed"),
    )) {
      assertEquals(emptySet(), match.availableSections)
      assertEquals(
        listOf(MatchDetailsRow.BottomSpacer),
        buildMatchDetailsRows(match, MatchDetailsPreferences(), showBreakdownEmpty = false),
      )
    }
  }

  @Test
  fun theSummaryFollowsTheMatchPhase() {
    val pre = MatchSummary("Pre-game analysis.", "2026-10-16T12:00:00Z")
    val post = MatchSummary("Post-game analysis.", "2026-10-16T15:00:00Z")
    val insights = MatchInsights(preMatchSummary = pre, postMatchSummary = post)
    for (status in listOf("upcoming", "TBD", "LIVE", " ongoing ")) {
      assertEquals(MatchInsightSummary(MatchSummaryPhase.PreGame, pre), match(insights, status).selectedInsightSummary())
    }
    for (status in listOf("completed", "FINAL")) {
      assertEquals(MatchInsightSummary(MatchSummaryPhase.PostGame, post), match(insights, status).selectedInsightSummary())
    }
    for (status in listOf(null, "unknown")) {
      assertEquals(null, match(insights, status).selectedInsightSummary())
    }
  }

  @Test
  fun aBlankSummaryDoesNotCreateAnEmptySection() {
    val blank = MatchSummary(" \n ", "2026-10-16T12:00:00Z")
    assertEquals(emptySet(), match(insights = MatchInsights(preMatchSummary = blank)).availableSections)
  }

  @Test
  fun theBreakdownEmptyStateTakesTheBreakdownsPlace() {
    val pending = match(insights = MatchInsights(preMatchSummary = MatchSummary("Pre-game analysis.", "2026-10-16T12:00:00Z")))
    val preferences = MatchDetailsPreferences(sectionOrder = listOf(Breakdown, Insights))

    assertEquals(
      listOf(MatchDetailsRow.BreakdownEmpty, MatchDetailsRow.Insights, MatchDetailsRow.BottomSpacer),
      buildMatchDetailsRows(pending, preferences, showBreakdownEmpty = true),
    )
    assertEquals(
      listOf(MatchDetailsRow.Insights, MatchDetailsRow.BottomSpacer),
      buildMatchDetailsRows(pending, preferences.withVisibility(Breakdown, visible = false), showBreakdownEmpty = false),
    )
  }

  @Test
  fun rowKeysAreUniqueAndAvoidTheHeroKey() {
    val keys = MatchDetailsRow.entries.map(MatchDetailsRow::key)

    assertEquals(keys.distinct(), keys)
    assertEquals(false, "shared-detail-hero" in keys)
  }

  private fun match(
    insights: MatchInsights? = null,
    status: String? = "upcoming",
    maps: List<MapData> = emptyList(),
    encounters: List<PreviousEncounter> = emptyList(),
    vods: List<VideoReference> = emptyList(),
  ): MatchDetails = MatchDetails(
    id = "match-1",
    event = EventInfo(
      id = "event-1",
      name = "Masters",
      series = "Bo3",
      stage = "Playoffs",
      img = "",
      date = "Today",
      patch = null,
      status = status,
    ),
    head2head = encounters,
    note = "",
    score = "0:0",
    teams = emptyList(),
    bans = emptyList(),
    videos = MatchVideos(streams = emptyList(), vods = vods),
    matchData = maps,
    mapCount = maps.size,
    insights = insights,
  )
}
