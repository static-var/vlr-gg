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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class RememberCardMascotTest {
  private val firstOwner = MascotOwnerToken()
  private val secondOwner = MascotOwnerToken()

  @Test
  fun rejectedVisitNeverGrantsOwnership() {
    val state = CardMascotState(allowed = false)
    state.register(firstOwner)
    state.register(secondOwner)
    state.selectOwner(Random(0))
    assertEquals(emptyList(), state.visibleCandidates)
    assertNull(state.ownerId)
  }

  @Test
  fun ownershipIsExclusiveAndReleasedAppearanceCannotBeRepeated() {
    val state = CardMascotState(allowed = true)
    state.register(firstOwner)
    state.register(firstOwner)
    assertEquals(listOf(firstOwner), state.visibleCandidates)
    state.selectOwner(Random(0))
    state.register(secondOwner)
    state.selectOwner(Random(1))
    assertSame(firstOwner, state.ownerId)

    state.release(secondOwner)
    assertSame(firstOwner, state.ownerId)
    state.release(firstOwner)
    state.register(secondOwner)
    state.selectOwner(Random(2))
    assertNull(state.ownerId)
  }

  @Test
  fun laterVisibleCandidateCanWinAfterLayoutSettles() = runTest {
    withMascot(rolls = listOf(0)) {
      state.register(firstOwner)
      state.register(secondOwner)
      recompose()
      assertNull(state.ownerId)
      advanceTimeBy(249)
      runCurrent()
      assertNull(state.ownerId)
      advanceTimeBy(1)
      runCurrent()
      assertSame(secondOwner, state.ownerId)
      assertEquals(1, random.rollCount)
    }
  }

  @Test
  fun candidateRemovedWhileSettlingCannotWin() = runTest {
    withMascot(rolls = listOf(0)) {
      state.register(firstOwner)
      state.register(secondOwner)
      recompose()
      advanceTimeBy(100)
      state.release(secondOwner)
      recompose()
      settle()
      assertSame(firstOwner, state.ownerId)
    }
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
      state.register(firstOwner)
      settle()
      recomposeUnrelatedContent()
      assertSame(firstOwner, state.ownerId)

      probabilityPercent = 20
      recompose()
      assertSame(initialState, state)
      assertEquals(1, random.rollCount)

      screenKey = "events"
      recompose()
      assertEquals(2, random.rollCount)
      assertNull(initialState.ownerId)
      initialState.register(secondOwner)
      initialState.selectOwner(Random(0))
      assertNull(initialState.ownerId)
      state.register(firstOwner)
      settle()
      assertSame(firstOwner, state.ownerId)
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
          val candidate = MascotOwnerToken()
          state.register(candidate)
          settle()
          assertEquals(if (roll < percentage) candidate else null, state.ownerId)
        }
      assertEquals(8, random.rollCount)
    }
  }

  @Test
  fun leavingCompositionReleasesOwnershipAndReentryStartsANewVisit() = runTest {
    withMascot(rolls = listOf(0, 0)) {
      state.register(firstOwner)
      settle()
      assertSame(firstOwner, state.ownerId)
      val previousVisit = state
      isPresent = false
      recompose()
      previousVisit.register(secondOwner)
      previousVisit.selectOwner(Random(0))
      assertNull(previousVisit.ownerId)

      isPresent = true
      recompose()
      assertEquals(2, random.rollCount)
      state.register(secondOwner)
      settle()
      assertSame(secondOwner, state.ownerId)
    }
  }

  @Test
  fun disposingBeforeSelectionPreventsPendingAndStaleCallbacksFromAcquiring() = runTest {
    withMascot(rolls = listOf(0)) {
      val pendingVisit = state
      state.register(firstOwner)
      recompose()
      isPresent = false
      recompose()
      pendingVisit.register(secondOwner)
      pendingVisit.selectOwner(Random(0))
      settle()
      assertEquals(emptyList(), pendingVisit.visibleCandidates)
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

    override fun nextInt(until: Int): Int = if (until == 100) rolls[rollCount++] else until - 1

    override fun nextBits(bitCount: Int): Int = error("Expected bounded random selection")
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

    fun settle() {
      recompose()
      scope.advanceTimeBy(250)
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
