/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransitionContentFadeTest {
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
