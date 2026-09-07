/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.sheet

import androidx.compose.animation.core.LinearEasing
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import dev.staticvar.designsystem.prism.animation.LocalPrismAnimations
import dev.staticvar.designsystem.prism.animation.PrismAnimationPreset
import dev.staticvar.designsystem.prism.animation.PrismAnimationTokens
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class PrismModalSheetLifecycleTest {
  @get:Rule
  val compose = createComposeRule()

  @Test
  fun zeroDurationStillCallsCollapsedOnceAfterContentDisposal() {
    compose.mainClock.autoAdvance = false
    var visible by mutableStateOf(false)
    var contentComposed = false
    var collapsedCount = 0
    val immediate = PrismAnimationPreset(0, LinearEasing)
    compose.setContent {
      PrismTheme(family = PrismThemeFamily.Console) {
        CompositionLocalProvider(
          LocalPrismAnimations provides PrismAnimationTokens(
            sheetEnter = immediate,
            sheetExit = immediate,
            scrimEnter = immediate,
            scrimExit = immediate,
          ),
        ) {
          PrismModalSheet(
            visible = visible,
            onDismissRequest = { visible = false },
            onCollapsed = {
              assertFalse(contentComposed)
              collapsedCount++
            },
          ) {
            DisposableEffect(Unit) {
              contentComposed = true
              onDispose { contentComposed = false }
            }
            Text("Sheet content")
          }
        }
      }
    }
    advance(100)
    compose.runOnIdle {
      assertEquals(1, collapsedCount)
      visible = true
    }
    advance(100)
    compose.runOnIdle {
      assertTrue(contentComposed)
      assertEquals(1, collapsedCount)
      visible = false
    }
    advance(100)
    compose.runOnIdle {
      assertFalse(contentComposed)
      assertEquals(2, collapsedCount)
    }
    advance(100)
    compose.runOnIdle { assertEquals(2, collapsedCount) }
  }

  @Test
  fun reopeningDuringExitCancelsCollapseUntilNextCompletedDismissal() {
    compose.mainClock.autoAdvance = false
    var visible by mutableStateOf(false)
    var contentComposed = false
    var collapsedCount = 0
    compose.setContent {
      PrismTheme(family = PrismThemeFamily.Console) {
        PrismModalSheet(
          visible = visible,
          onDismissRequest = { visible = false },
          onCollapsed = {
            assertFalse(contentComposed)
            collapsedCount++
          },
        ) {
          DisposableEffect(Unit) {
            contentComposed = true
            onDispose { contentComposed = false }
          }
          Text("Sheet content")
        }
      }
    }
    advance(100)
    compose.runOnIdle {
      assertEquals(1, collapsedCount)
      visible = true
    }
    advance(500)
    compose.runOnIdle {
      assertTrue(contentComposed)
      visible = false
    }
    advance(32)
    compose.runOnIdle {
      assertTrue(contentComposed)
      assertEquals(1, collapsedCount)
      visible = true
    }
    advance(500)
    compose.runOnIdle {
      assertTrue(contentComposed)
      assertEquals(1, collapsedCount)
      visible = false
    }
    advance(500)
    compose.runOnIdle {
      assertFalse(contentComposed)
      assertEquals(2, collapsedCount)
    }
    advance(500)
    compose.runOnIdle { assertEquals(2, collapsedCount) }
  }

  private fun advance(milliseconds: Long) {
    compose.mainClock.advanceTimeBy(milliseconds)
    compose.waitForIdle()
  }
}
