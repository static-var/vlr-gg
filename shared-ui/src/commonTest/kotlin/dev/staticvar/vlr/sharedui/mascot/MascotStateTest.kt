/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MascotStateTest {
  private val lead = MascotCue("match:lead:team", "Go team!", 10)
  private val performance = MascotCue("match:top:player", "Player cooked!", 40)

  @Test
  fun eligibleVisitShowsOnceEvenWhenCandidatesChange() {
    val state = MascotState()
    state.showOnce(listOf(lead))
    assertEquals(lead, state.cue)
    state.showOnce(listOf(performance))
    assertTrue(state.hasShown)
    assertEquals(lead, state.cue)
  }

  @Test
  fun emptyDataDoesNotSpendTheVisit() {
    val state = MascotState()
    state.showOnce(emptyList())
    assertFalse(state.hasShown)
    state.showOnce(listOf(lead))
    assertEquals(lead, state.cue)
  }

  @Test
  fun selectsTheHighestPriorityWithoutDependingOnListOrder() {
    val state = MascotState()
    state.showOnce(listOf(lead, performance))
    assertEquals(performance, state.cue)
    state.onFinished()
    state.showOnce(listOf(lead))
    assertNull(state.cue)
  }

  @Test
  fun interactionDismissesTheMascotWithoutReplayingAfterwards() {
    val state = MascotState()
    state.showOnce(listOf(lead))
    state.dismissIfUnavailable(listOf(lead), canShow = false)
    assertNull(state.cue)
    state.showOnce(listOf(lead))
    assertNull(state.cue)
  }

  @Test
  fun losingTheLeadWithdrawsItsBanner() {
    val state = MascotState()
    state.showOnce(listOf(lead))
    state.dismissIfUnavailable(listOf(performance), canShow = true)
    assertNull(state.cue)
    assertTrue(state.hasShown)
  }

  @Test
  fun blockedBeforeShowingCanStillShowLater() {
    val state = MascotState()
    state.dismissIfUnavailable(listOf(lead), canShow = false)
    assertFalse(state.hasShown)
    state.showOnce(listOf(lead))
    assertEquals(lead, state.cue)
  }
}
