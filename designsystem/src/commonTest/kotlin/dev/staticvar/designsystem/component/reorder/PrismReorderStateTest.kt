/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.reorder

import androidx.compose.animation.core.snap
import androidx.compose.runtime.BroadcastFrameClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrismReorderStateTest {
  private val savedOrder = listOf(A, B, C, D)

  @Test
  fun firstRowReachesTheLastSlotWhileClampedToTheList() = runTest {
    val state = reorderState()

    state.start(A)
    state.dragBy(10_000f)

    assertEquals(listOf(B, C, D, A), state.order)
    assertEquals(330f..430f, state.draggedSpan())
    assertEquals(listOf(B, C, D, A), state.end())
  }

  @Test
  fun lastRowReachesTheFirstSlotWhileClampedToTheList() = runTest {
    val state = reorderState()

    state.start(D)
    state.dragBy(-10_000f)

    assertEquals(listOf(D, A, B, C), state.order)
    assertEquals(0f..100f, state.draggedSpan())
  }

  @Test
  fun unevenRowsStillReachBothEnds() {
    val heights = mapOf(A to 160, B to 40, C to 40, D to 40)
    val listHeight = 160f + 3 * 40f + 3 * 8f

    assertEquals(listOf(B, C, D, A), savedOrder.reorderedForDrag(A, top = listHeight - 160f, heights, spacing = 8f))
    assertEquals(listOf(D, A, B, C), savedOrder.reorderedForDrag(D, top = 0f, heights, spacing = 8f))
  }

  @Test
  fun rowsSwapOnlyPastTheNeighboursMiddleAndDoNotSwapStraightBack() = runTest {
    val state = reorderState()
    state.start(A)

    assertFalse(state.dragBy(40f))
    assertEquals(savedOrder, state.order)

    assertTrue(state.dragBy(30f))
    assertEquals(listOf(B, A, C, D), state.order)
    assertEquals(110f, state.offsetOf(B), "B starts where it was drawn, then slides up")
    assertEquals(70f - 110f, state.offsetOf(A), "the held row stays under the finger")

    assertFalse(state.dragBy(-5f))
    assertEquals(listOf(B, A, C, D), state.order)
  }

  @Test
  fun cancellingRestoresTheSavedOrderAndSavesNothing() = runTest {
    val state = reorderState()
    state.start(A)
    state.dragBy(10_000f)

    state.cancel()

    assertEquals(savedOrder, state.order)
    assertNull(state.dragged)
    assertEquals(330f, state.offsetOf(A), "the row slides back from where it was dropped")
    assertNull(state.end())
  }

  @Test
  fun droppingSavesOnlyAChangedOrderAndOnlyOnce() = runTest {
    val state = reorderState()

    state.start(B)
    state.dragBy(20f)
    assertNull(state.end(), "a drop back in its own slot saves nothing")

    state.start(B)
    state.dragBy(70f)
    assertEquals(listOf(A, C, B, D), state.end())
    assertNull(state.end())
  }

  @Test
  fun aSavedOrderArrivingMidDragWaitsForTheDragAndThenApplies() = runTest {
    val external = listOf(D, C, B, A)
    val state = reorderState()
    state.start(A)

    state.sync(external)
    assertEquals(savedOrder, state.order, "rows keep following the finger")

    assertNull(state.end(), "a drop back in place saves nothing")
    assertEquals(external, state.order, "the order saved meanwhile shows without another sync")
  }

  @Test
  fun cancellingAfterASavedOrderArrivesSettlesOnThatOrder() = runTest {
    val external = listOf(D, C, B, A)
    val state = reorderState()
    state.start(A)
    state.dragBy(10_000f)

    state.sync(external)
    state.cancel()

    assertEquals(external, state.order)
    assertNull(state.end())
  }

  @Test
  fun aRowAboveResizingMidDragLeavesTheHeldRowWhereItIs() = runTest {
    val state = reorderState()
    state.start(D)

    state.onMeasured(A, 160)

    assertEquals(330f..430f, state.draggedSpan())
    assertEquals(savedOrder, state.order)
    assertNull(state.end())
  }

  @Test
  fun onlyOneRowCanBeHeld() = runTest {
    val state = reorderState()

    assertTrue(state.start(A))
    assertFalse(state.start(D))
    assertEquals(A, state.dragged)
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
  private fun TestScope.reorderState(): PrismReorderState<String> {
    val scope = CoroutineScope(backgroundScope.coroutineContext + BroadcastFrameClock())
    return PrismReorderState(savedOrder, scope, spacing = 10f, settleSpec = snap()).apply {
      savedOrder.forEach { onMeasured(it, 100) }
    }
  }

  private companion object {
    const val A = "a"
    const val B = "b"
    const val C = "c"
    const val D = "d"
  }
}
