/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.core.settings.MatchDetailsPreferences
import dev.staticvar.vlr.core.settings.MatchDetailsSection.Breakdown
import dev.staticvar.vlr.core.settings.MatchDetailsSection.HeadToHead
import dev.staticvar.vlr.core.settings.MatchDetailsSection.Media
import dev.staticvar.vlr.core.settings.MatchDetailsSection.Prediction
import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPrediction
import dev.staticvar.vlr.domain.model.MatchVideos
import dev.staticvar.vlr.domain.model.PredictionSource
import dev.staticvar.vlr.domain.model.PreviousEncounter
import dev.staticvar.vlr.domain.model.VideoReference
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchDetailsLayoutTest {
  private val everything = match(
    prediction = MatchPrediction("a", "b", 0.6, 0.4, PredictionSource.MODEL),
    maps = listOf(MapData("Lotus", emptyList(), emptyList(), emptyList())),
    encounters = listOf(PreviousEncounter("older", emptyList())),
    vods = listOf(VideoReference("Map 1", "youtube.com/watch")),
  )

  @Test
  fun defaultOrderMatchesThePageBeforeOrderingExisted() {
    assertEquals(
      listOf("prediction", "breakdown", "head-to-head", "media-title", "media", "bottom-spacer"),
      buildMatchDetailsRows(everything, MatchDetailsPreferences(), showBreakdownEmpty = false).map(MatchDetailsRow::key),
    )
  }

  @Test
  fun sectionsFollowTheSavedOrderAndMediaStaysTogether() {
    val preferences = MatchDetailsPreferences(sectionOrder = listOf(Media, HeadToHead, Breakdown, Prediction))

    assertEquals(
      listOf(
        MatchDetailsRow.MediaTitle,
        MatchDetailsRow.Media,
        MatchDetailsRow.HeadToHead,
        MatchDetailsRow.Breakdown,
        MatchDetailsRow.Prediction,
        MatchDetailsRow.BottomSpacer,
      ),
      buildMatchDetailsRows(everything, preferences, showBreakdownEmpty = false),
    )
  }

  @Test
  fun hiddenSectionsLeaveTheBodyButKeepTheirPlace() {
    val preferences = MatchDetailsPreferences(showPrediction = false, showMedia = false)
      .moved(Prediction, 3)

    assertEquals(
      listOf(MatchDetailsRow.Breakdown, MatchDetailsRow.HeadToHead, MatchDetailsRow.BottomSpacer),
      buildMatchDetailsRows(everything, preferences, showBreakdownEmpty = false),
    )
    assertEquals(
      listOf(Breakdown, HeadToHead, Media, Prediction),
      preferences.withVisibility(Prediction, visible = true).orderedSections,
    )
  }

  @Test
  fun sectionsWithoutDataStayOutOfTheBodyAndCannotBeToggled() {
    val noMedia = everything.copy(videos = MatchVideos(emptyList(), emptyList()), head2head = emptyList())

    assertEquals(setOf(Prediction, Breakdown), noMedia.availableSections)
    assertEquals(
      listOf(MatchDetailsRow.Prediction, MatchDetailsRow.Breakdown, MatchDetailsRow.BottomSpacer),
      buildMatchDetailsRows(noMedia, MatchDetailsPreferences(), showBreakdownEmpty = false),
    )
  }

  @Test
  fun aRequestablePredictionIsAvailableBeforeItArrives() {
    val pending = match(canRequestPrediction = true)

    assertEquals(setOf(Prediction), pending.availableSections)
    assertEquals(
      listOf(MatchDetailsRow.Prediction, MatchDetailsRow.BottomSpacer),
      buildMatchDetailsRows(pending, MatchDetailsPreferences(), showBreakdownEmpty = false),
    )
    assertEquals(emptySet(), match().availableSections)
  }

  @Test
  fun theBreakdownEmptyStateTakesTheBreakdownsPlace() {
    val pending = match(canRequestPrediction = true)
    val preferences = MatchDetailsPreferences(sectionOrder = listOf(Breakdown, Prediction))

    assertEquals(
      listOf(MatchDetailsRow.BreakdownEmpty, MatchDetailsRow.Prediction, MatchDetailsRow.BottomSpacer),
      buildMatchDetailsRows(pending, preferences, showBreakdownEmpty = true),
    )
    assertEquals(
      listOf(MatchDetailsRow.Prediction, MatchDetailsRow.BottomSpacer),
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
    prediction: MatchPrediction? = null,
    canRequestPrediction: Boolean = false,
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
      status = "upcoming",
    ),
    head2head = encounters,
    note = "",
    score = "0:0",
    teams = emptyList(),
    bans = emptyList(),
    videos = MatchVideos(streams = emptyList(), vods = vods),
    matchData = maps,
    mapCount = maps.size,
    prediction = prediction,
    canRequestPrediction = canRequestPrediction,
  )
}
