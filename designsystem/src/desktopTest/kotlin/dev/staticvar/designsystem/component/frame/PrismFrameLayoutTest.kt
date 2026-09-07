/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.frame

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.frame.LocalPrismFrames
import dev.staticvar.designsystem.prism.frame.PrismFrameTokens
import dev.staticvar.designsystem.prism.frame.PrismFrames
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

internal class PrismFrameLayoutTest {
  @get:Rule
  val compose = createComposeRule()

  private val frame = PrismFrameTokens(
    border = BorderStroke(2.dp, Color.Black),
    shadowColor = Color.Black,
    shadowOffset = DpOffset(6.dp, 6.dp),
  )

  @Test(timeout = 30_000)
  fun shadowReservesSpaceAndPressKeepsSemanticBoundsFixed() {
    val progress = mutableFloatStateOf(0f)
    compose.setContent {
      Box(Modifier.testTag("frame").prismFrame(frame, RectangleShape, progress.floatValue)) {
        Box(Modifier.size(100.dp, 40.dp).testTag("face"))
      }
    }
    compose.onNodeWithTag("frame").assertWidthIsEqualTo(106.dp).assertHeightIsEqualTo(46.dp)
    val initialBounds = compose.onNodeWithTag("face").fetchSemanticsNode().boundsInRoot
    compose.runOnIdle { progress.floatValue = 1f }
    assertEquals(initialBounds, compose.onNodeWithTag("face").fetchSemanticsNode().boundsInRoot)
  }

  @Test(timeout = 30_000)
  fun framedButtonRetainsClickAndBoundsThroughPress() {
    val interactions = MutableInteractionSource()
    var clicks = 0
    compose.setContent {
      PrismTheme {
        CompositionLocalProvider(LocalPrismFrames provides PrismFrames(control = frame)) {
          PrismButton(
            onClick = { clicks++ },
            modifier = Modifier.testTag("button"),
            interactionSource = interactions,
          ) { Text("Play") }
        }
      }
    }
    val button = compose.onNodeWithTag("button").assertHasClickAction()
    val initialBounds = button.fetchSemanticsNode().boundsInRoot
    val press = PressInteraction.Press(Offset.Zero)
    compose.runOnIdle { interactions.tryEmit(press) }
    compose.waitForIdle()
    assertEquals(initialBounds, button.fetchSemanticsNode().boundsInRoot)
    compose.runOnIdle { interactions.tryEmit(PressInteraction.Release(press)) }
    button.performClick()
    compose.runOnIdle { assertEquals(1, clicks) }
  }
}
