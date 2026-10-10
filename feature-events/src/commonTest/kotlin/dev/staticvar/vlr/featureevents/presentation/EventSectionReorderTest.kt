/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.compose.animation.core.snap
import androidx.compose.runtime.BroadcastFrameClock
import dev.staticvar.vlr.core.settings.EventDetailsSection
import dev.staticvar.vlr.core.settings.EventDetailsSection.Matches
import dev.staticvar.vlr.core.settings.EventDetailsSection.Participants
import dev.staticvar.vlr.core.settings.EventDetailsSection.Prizes
import dev.staticvar.vlr.core.settings.EventDetailsSection.Standings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventSectionReorderTest {
  private val savedOrder = listOf(Participants, Prizes, Standings, Matches)

  @Test
  fun firstRowReachesTheLastSlotWhileClampedToTheList() = runTest {
    val state = reorderState()

    state.start(Participants)
    state.dragBy(10_000f)

    assertEquals(listOf(Prizes, Standings, Matches, Participants), state.order)
    assertEquals(330f..430f, state.draggedSpan())
    assertEquals(listOf(Prizes, Standings, Matches, Participants), state.end())
  }

  @Test
  fun lastRowReachesTheFirstSlotWhileClampedToTheList() = runTest {
    val state = reorderState()

    state.start(Matches)
    state.dragBy(-10_000f)

    assertEquals(listOf(Matches, Participants, Prizes, Standings), state.order)
    assertEquals(0f..100f, state.draggedSpan())
  }

  @Test
  fun unevenRowsStillReachBothEnds() {
    val heights = mapOf(Participants to 160, Prizes to 40, Standings to 40, Matches to 40)
    val listHeight = 160f + 3 * 40f + 3 * 8f

    assertEquals(
      listOf(Prizes, Standings, Matches, Participants),
      savedOrder.reorderedForDrag(Participants, top = listHeight - 160f, heights, spacing = 8f),
    )
    assertEquals(
      listOf(Matches, Participants, Prizes, Standings),
      savedOrder.reorderedForDrag(Matches, top = 0f, heights, spacing = 8f),
    )
  }

  @Test
  fun rowsSwapOnlyPastTheNeighboursMiddleAndDoNotSwapStraightBack() = runTest {
    val state = reorderState()
    state.start(Participants)

    assertFalse(state.dragBy(40f))
    assertEquals(savedOrder, state.order)

    assertTrue(state.dragBy(30f))
    assertEquals(listOf(Prizes, Participants, Standings, Matches), state.order)
    assertEquals(110f, state.offsetOf(Prizes), "Prizes starts where it was drawn, then slides up")
    assertEquals(70f - 110f, state.offsetOf(Participants), "the held row stays under the finger")

    assertFalse(state.dragBy(-5f))
    assertEquals(listOf(Prizes, Participants, Standings, Matches), state.order)
  }

  @Test
  fun cancellingRestoresTheSavedOrderAndSavesNothing() = runTest {
    val state = reorderState()
    state.start(Participants)
    state.dragBy(10_000f)

    state.cancel()

    assertEquals(savedOrder, state.order)
    assertNull(state.dragged)
    assertEquals(330f, state.offsetOf(Participants), "the row slides back from where it was dropped")
    assertNull(state.end())
  }

  @Test
  fun droppingSavesOnlyAChangedOrderAndOnlyOnce() = runTest {
    val state = reorderState()

    state.start(Prizes)
    state.dragBy(20f)
    assertNull(state.end(), "a drop back in its own slot saves nothing")

    state.start(Prizes)
    state.dragBy(70f)
    assertEquals(listOf(Participants, Standings, Prizes, Matches), state.end())
    assertNull(state.end())
  }

  @Test
  fun aSavedOrderArrivingMidDragWaitsForTheDragToFinish() = runTest {
    val state = reorderState()
    state.start(Participants)

    state.sync(listOf(Matches, Standings, Prizes, Participants))
    assertEquals(savedOrder, state.order)
    assertNull(state.end())

    state.sync(listOf(Matches, Standings, Prizes, Participants))
    assertEquals(listOf(Matches, Standings, Prizes, Participants), state.order)
  }

  @Test
  fun aRowAboveResizingMidDragLeavesTheHeldRowWhereItIs() = runTest {
    val state = reorderState()
    state.start(Matches)

    state.onMeasured(Participants, 160)

    assertEquals(330f..430f, state.draggedSpan())
    assertEquals(savedOrder, state.order)
    assertNull(state.end())
  }

  @Test
  fun onlyOneRowCanBeHeld() = runTest {
    val state = reorderState()

    assertTrue(state.start(Participants))
    assertFalse(state.start(Matches))
    assertEquals(Participants, state.dragged)
  }

  @Test
  fun autoScrollStartsInsideTheEdgeBandAndIsCapped() {
    val visible = 0f..300f

    assertEquals(0f, autoScrollStep(row = 100f..200f, visible = visible, edge = 40f, maxStep = 10f))
    assertEquals(5f, autoScrollStep(row = 180f..280f, visible = visible, edge = 40f, maxStep = 10f))
    assertEquals(10f, autoScrollStep(row = 250f..350f, visible = visible, edge = 40f, maxStep = 10f))
    assertEquals(-10f, autoScrollStep(row = -60f..40f, visible = visible, edge = 40f, maxStep = 10f))
  }

  @Test
  fun aRowReachingBothEdgeBandsScrollsTowardTheEdgeItPassesFurther() {
    val visible = 0f..150f

    assertEquals(10f, autoScrollStep(row = 20f..180f, visible = visible, edge = 48f, maxStep = 10f))
    assertEquals(-10f, autoScrollStep(row = -30f..130f, visible = visible, edge = 48f, maxStep = 10f))
    assertEquals(0f, autoScrollStep(row = -5f..155f, visible = visible, edge = 48f, maxStep = 10f))
  }

  /** Four 100px rows 10px apart; settle animations wait on a frame clock that never ticks. */
  private fun TestScope.reorderState(): EventSectionReorderState {
    val scope = CoroutineScope(backgroundScope.coroutineContext + BroadcastFrameClock())
    return EventSectionReorderState(savedOrder, scope, spacing = 10f, settleSpec = snap()).apply {
      EventDetailsSection.entries.forEach { onMeasured(it, 100) }
    }
  }
}
