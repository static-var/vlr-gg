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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RememberMascotTest {
  @Test
  fun leavingDuringDelayCancelsThePendingAppearance() = runTest {
    withMascot {
      advanceTimeBy(750)
      val retained = state
      isPresent = false
      recompose()
      advanceTimeBy(3_000)
      runCurrent()

      assertNull(retained.cue)
      assertFalse(retained.hasShown)
    }
  }

  @Test
  fun leavingWhileVisibleClearsTheRetainedState() = runTest {
    withMascot {
      advanceTimeBy(1_500)
      runCurrent()
      val retained = state
      assertEquals(cue, retained.cue)

      isPresent = false
      recompose()

      assertNull(retained.cue)
      assertTrue(retained.hasShown)
    }
  }

  @Test
  fun inactivityAndInteractionRestartTheFullSettleDelay() = runTest {
    withMascot {
      advanceTimeBy(750)
      isActive = false
      recompose()
      advanceTimeBy(3_000)
      runCurrent()
      assertNull(state.cue)
      assertFalse(state.hasShown)

      isActive = true
      recompose()
      advanceTimeBy(750)
      isInteracting = true
      recompose()
      advanceTimeBy(3_000)
      runCurrent()
      assertNull(state.cue)
      assertFalse(state.hasShown)

      isInteracting = false
      recompose()
      advanceTimeBy(1_499)
      runCurrent()
      assertNull(state.cue)
      advanceTimeBy(1)
      runCurrent()
      assertEquals(cue, state.cue)
    }
  }

  @Test
  fun returningToTheSameVisitDoesNotReplayADismissedMascot() = runTest {
    withMascot {
      advanceTimeBy(1_500)
      runCurrent()
      assertEquals(cue, state.cue)

      isActive = false
      recompose()
      assertNull(state.cue)
      isActive = true
      recompose()
      advanceTimeBy(3_000)
      runCurrent()

      assertNull(state.cue)
      assertTrue(state.hasShown)
    }
  }

  private fun TestScope.withMascot(block: MascotComposition.() -> Unit) {
    val harness = MascotComposition(this)
    try {
      harness.recompose()
      harness.block()
    } finally {
      harness.dispose()
    }
  }

  private class MascotComposition(private val scope: TestScope) {
    val cue = MascotCue("match:winner:team", "Go team!", 10)
    var isPresent by mutableStateOf(true)
    var isActive by mutableStateOf(true)
    var isInteracting by mutableStateOf(false)
    lateinit var state: MascotState
      private set

    private val frameClock = object : MonotonicFrameClock {
      override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
        onFrame(scope.testScheduler.currentTime * 1_000_000)
    }
    private val recomposer = Recomposer(scope.backgroundScope.coroutineContext + frameClock)
    private val composition = Composition(UnitApplier(), recomposer)

    init {
      scope.backgroundScope.launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }
      composition.setContent {
        if (isPresent) {
          state = rememberMascot(
            screenKey = "match:1",
            candidates = listOf(cue),
            isScreenActive = isActive,
            isContentReady = true,
            isInteracting = isInteracting,
          )
        }
      }
    }

    fun recompose() {
      Snapshot.sendApplyNotifications()
      scope.runCurrent()
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
