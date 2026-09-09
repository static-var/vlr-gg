/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RememberCardMascotTest {
  @Test
  fun rejectedVisitNeverGrantsOwnership() {
    val state = CardMascotState(allowed = false)
    assertFalse(state.tryAcquire("first"))
    assertFalse(state.tryAcquire("second"))
    assertNull(state.ownerId)
  }

  @Test
  fun ownershipIsExclusiveAndRepeatedAcquisitionByTheOwnerIsStable() {
    val state = CardMascotState(allowed = true)
    assertTrue(state.tryAcquire("first"))
    assertTrue(state.tryAcquire("first"))
    assertFalse(state.tryAcquire("second"))

    state.release("second")
    assertEquals("first", state.ownerId)

    state.release("first")
    assertNull(state.ownerId)
    assertFalse(state.tryAcquire("first"))
    assertFalse(state.tryAcquire("second"))
  }

  @Test
  fun recompositionDuringLoadingAndAfterContentArrivesDoesNotReroll() = runTest {
    withMascot(rolls = listOf(4, 5)) {
      val initialState = state
      assertEquals(1, random.rollCount)
      recomposeUnrelatedContent()
      isLoading = false
      recompose()
      assertSame(initialState, state)
      assertEquals(1, random.rollCount)
      assertTrue(state.tryAcquire("first"))
      recomposeUnrelatedContent()
      assertEquals("first", state.ownerId)

      probabilityPercent = 20
      recompose()
      assertSame(initialState, state)
      assertEquals(1, random.rollCount)

      screenKey = "events"
      recompose()
      assertEquals(2, random.rollCount)
      assertNull(initialState.ownerId)
      assertFalse(initialState.tryAcquire("second"))
      assertTrue(state.tryAcquire("first"))
    }
  }

  @Test
  fun visitOddsUseTheSelectedPercentage() = runTest {
    withMascot(rolls = listOf(0, 0, 9, 10, 19, 20, 39, 40)) {
      listOf(0 to 0, 10 to 9, 10 to 10, 20 to 19, 20 to 20, 40 to 39, 40 to 40)
        .forEachIndexed { index, (percentage, roll) ->
          probabilityPercent = percentage
          screenKey = "visit-$index"
          recompose()
          assertEquals(roll < percentage, state.tryAcquire("card"))
        }
    }
  }

  @Test
  fun leavingCompositionReleasesOwnershipAndReentryStartsANewVisit() = runTest {
    withMascot(rolls = listOf(0, 0)) {
      assertTrue(state.tryAcquire("first"))
      val previousVisit = state
      isPresent = false
      recompose()
      assertNull(previousVisit.ownerId)
      assertFalse(previousVisit.tryAcquire("second"))

      isPresent = true
      recompose()
      assertEquals(2, random.rollCount)
      assertTrue(state.tryAcquire("second"))
      assertEquals("second", state.ownerId)
    }
  }

  @Test
  fun disposingBeforeAnyCardAppearsPreventsStaleCallbacksFromAcquiring() = runTest {
    withMascot(rolls = listOf(0)) {
      val pendingVisit = state
      isPresent = false
      recompose()
      assertFalse(pendingVisit.tryAcquire("first"))
      assertNull(pendingVisit.ownerId)
    }
  }

  private fun TestScope.withMascot(rolls: List<Int>, block: MascotComposition.() -> Unit) {
    val harness = MascotComposition(this, rolls)
    try {
      harness.recompose()
      harness.block()
    } finally {
      harness.dispose()
    }
  }

  private class CountingRandom(private val rolls: List<Int>) : Random() {
    var rollCount: Int = 0
      private set

    override fun nextInt(until: Int): Int {
      assertEquals(100, until)
      return rolls[rollCount++]
    }

    override fun nextBits(bitCount: Int): Int = error("Expected one percentage roll")
  }

  private class MascotComposition(private val scope: TestScope, rolls: List<Int>) {
    var screenKey by mutableStateOf("matches")
    var probabilityPercent by mutableStateOf(10)
    var isLoading by mutableStateOf(true)
    var isPresent by mutableStateOf(true)
    private var unrelatedContent by mutableStateOf(0)
    lateinit var state: CardMascotState
      private set

    val random = CountingRandom(rolls)
    private val frameClock = object : MonotonicFrameClock {
      override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
        onFrame(scope.testScheduler.currentTime * 1_000_000)
    }
    private val recomposer = Recomposer(scope.backgroundScope.coroutineContext + frameClock)
    private val composition = Composition(UnitApplier(), recomposer)

    init {
      scope.backgroundScope.launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }
      composition.setContent {
        @Suppress("UNUSED_EXPRESSION")
        unrelatedContent
        @Suppress("UNUSED_EXPRESSION")
        isLoading
        if (isPresent) {
          state = rememberCardMascot(screenKey = screenKey, probabilityPercent = probabilityPercent, random = random)
        }
      }
    }

    fun recompose() {
      Snapshot.sendApplyNotifications()
      scope.runCurrent()
    }

    fun recomposeUnrelatedContent() {
      unrelatedContent++
      recompose()
    }

    fun dispose() {
      composition.dispose()
      recomposer.cancel()
    }
  }

  private class UnitApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun remove(index: Int, count: Int) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun onClear() = Unit
  }
}
