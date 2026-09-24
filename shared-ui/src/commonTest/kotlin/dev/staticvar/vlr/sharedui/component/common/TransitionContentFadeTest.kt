/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.runtime.mutableStateOf
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransitionContentFadeTest {
  @Test
  fun `only the selected card fades including when match and event ids overlap`() {
    val hidden = TransitionContentFade(
      alpha = mutableStateOf(0f),
      isSettled = false,
      isVisible = false,
      acceptsInputState = mutableStateOf(false),
    )
    for (active in listOf(TransitionItem.Match("42"), TransitionItem.Event("42"))) {
      assertSame(hidden, selectTransitionContentFade(hidden, active, active))
      assertSame(hidden, selectTransitionContentFade(hidden, active, null))
      for (card in listOf(TransitionItem.Match("42"), TransitionItem.Match("43"), TransitionItem.Event("42"), TransitionItem.Event("43"))) {
        if (card == active) continue
        val visible = selectTransitionContentFade(hidden, active, card)
        assertEquals(1f, visible.alpha.value)
        assertTrue(visible.isVisible)
        assertTrue(visible.acceptsInput)
      }
    }
    val withoutTransition = selectTransitionContentFade(null, null, TransitionItem.Match("42"))
    assertEquals(1f, withoutTransition.alpha.value)
    assertTrue(withoutTransition.acceptsInput)
  }

  @Test
  fun `content is visible only when scoped navigation and shared transitions settle`() {
    assertFalse(settled(current = false, target = true, navRunning = false, sharedRunning = false))
    assertFalse(settled(current = false, target = true, navRunning = true, sharedRunning = true))
    assertFalse(settled(current = true, target = true, navRunning = false, sharedRunning = true))
    assertTrue(settled(current = true, target = true, navRunning = false, sharedRunning = false))
    assertFalse(settled(current = true, target = false, navRunning = true, sharedRunning = true))
  }

  @Test
  fun `content is visible without a transition scope`() {
    assertTrue(
      transitionContentSettled(
        hasScope = false,
        navCurrentVisible = false,
        navTargetVisible = false,
        navRunning = true,
        sharedRunning = true,
      ),
    )
  }

  private fun settled(current: Boolean, target: Boolean, navRunning: Boolean, sharedRunning: Boolean): Boolean =
    transitionContentSettled(
      hasScope = true,
      navCurrentVisible = current,
      navTargetVisible = target,
      navRunning = navRunning,
      sharedRunning = sharedRunning,
    )
}
