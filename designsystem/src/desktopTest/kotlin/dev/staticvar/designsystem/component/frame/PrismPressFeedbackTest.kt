/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.frame

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import dev.staticvar.designsystem.prism.animation.LocalPrismAnimations
import dev.staticvar.designsystem.prism.animation.PrismAnimationPreset
import dev.staticvar.designsystem.prism.animation.PrismAnimationTokens
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class PrismPressFeedbackTest {
  @get:Rule
  val compose = createComposeRule()

  @Test(timeout = 30_000)
  fun sameFramePressAndReleaseStillDepressesThenReturns() {
    val interactions = MutableInteractionSource()
    val progress = installFeedback(interactions)
    val press = PressInteraction.Press(Offset.Zero)
    compose.runOnIdle {
      interactions.tryEmit(press)
      interactions.tryEmit(PressInteraction.Release(press))
    }
    advance(48)
    compose.runOnIdle { assertTrue(progress().value > 0f, "A quick tap must produce visible displacement") }
    advance(300)
    compose.runOnIdle { assertEquals(0f, progress().value) }
  }

  @Test(timeout = 30_000)
  fun overlappingPressesStayDepressedUntilTheLastRelease() {
    val interactions = MutableInteractionSource()
    val progress = installFeedback(interactions)
    val first = PressInteraction.Press(Offset.Zero)
    val second = PressInteraction.Press(Offset.Zero)
    compose.runOnIdle {
      interactions.tryEmit(first)
      interactions.tryEmit(second)
    }
    advance(160)
    compose.runOnIdle { interactions.tryEmit(PressInteraction.Release(first)) }
    advance(160)
    compose.runOnIdle {
      assertEquals(1f, progress().value)
      interactions.tryEmit(PressInteraction.Release(second))
    }
    advance(160)
    compose.runOnIdle { assertEquals(0f, progress().value) }
  }

  @Test(timeout = 30_000)
  fun cancelledPressReturnsWithoutStayingDepressed() {
    val interactions = MutableInteractionSource()
    val progress = installFeedback(interactions)
    val press = PressInteraction.Press(Offset.Zero)
    compose.runOnIdle { interactions.tryEmit(press) }
    advance(48)
    compose.runOnIdle {
      assertTrue(progress().value > 0f)
      interactions.tryEmit(PressInteraction.Cancel(press))
    }
    advance(300)
    compose.runOnIdle { assertEquals(0f, progress().value) }
  }

  @Test(timeout = 30_000)
  fun zeroDurationPressAndReleaseSettleWithoutWaitingForMotion() {
    val interactions = MutableInteractionSource()
    val progress = installFeedback(interactions, durationMillis = 0)
    val press = PressInteraction.Press(Offset.Zero)
    compose.runOnIdle { interactions.tryEmit(press) }
    advance(32)
    compose.runOnIdle {
      assertEquals(1f, progress().value)
      interactions.tryEmit(PressInteraction.Release(press))
    }
    advance(32)
    compose.runOnIdle { assertEquals(0f, progress().value) }
  }

  @Test(timeout = 30_000)
  fun clickableConsoleCardMovesItsContentWhileKeepingBoundsFixed() {
    compose.mainClock.autoAdvance = false
    val interactions = MutableInteractionSource()
    compose.setContent {
      PrismTheme(family = PrismThemeFamily.Console) {
        PrismCard(
          modifier = Modifier.testTag("card"),
          onClick = {},
          interactionSource = interactions,
        ) {
          Box(Modifier.size(24.dp).background(Color.Red))
        }
      }
    }
    compose.waitForIdle()
    val card = compose.onNodeWithTag("card")
    val initialBounds = card.fetchSemanticsNode().boundsInRoot
    val initialPosition = redContentPosition()
    val press = PressInteraction.Press(Offset.Zero)
    compose.runOnIdle { interactions.tryEmit(press) }
    advance(160)
    val pressedPosition = redContentPosition()
    assertTrue(pressedPosition.x > initialPosition.x)
    assertTrue(pressedPosition.y > initialPosition.y)
    assertEquals(initialBounds, card.fetchSemanticsNode().boundsInRoot)
    compose.runOnIdle { interactions.tryEmit(PressInteraction.Release(press)) }
    advance(160)
    assertEquals(initialPosition, redContentPosition())
  }

  private fun installFeedback(
    interactions: MutableInteractionSource,
    durationMillis: Int = 60,
  ): () -> State<Float> {
    compose.mainClock.autoAdvance = false
    lateinit var progress: State<Float>
    compose.setContent {
      PrismTheme(family = PrismThemeFamily.Console) {
        CompositionLocalProvider(
          LocalPrismAnimations provides PrismAnimationTokens(
            press = PrismAnimationPreset(durationMillis, LinearEasing),
          ),
        ) {
          progress = rememberPrismPressProgress(interactions)
        }
      }
    }
    compose.waitForIdle()
    return { progress }
  }

  private fun advance(milliseconds: Long) {
    compose.mainClock.advanceTimeBy(milliseconds)
    compose.waitForIdle()
  }

  private fun redContentPosition(): Offset {
    val pixels = compose.onNodeWithTag("card").captureToImage().toPixelMap()
    for (y in 0 until pixels.height) {
      for (x in 0 until pixels.width) {
        if (pixels[x, y] == Color.Red) return Offset(x.toFloat(), y.toFloat())
      }
    }
    error("Card content was not drawn")
  }
}
