/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.staticvar.designsystem.prism.PrismTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Guards the banner-clipping fix: at the narrowest width and largest font scale, every message shows whole. */
@RunWith(AndroidJUnit4::class)
class MascotCelebrationLayoutTest {
  @get:Rule
  val compose = createAndroidComposeRule<MascotCelebrationLayoutTestActivity>()

  @Test
  fun longMessagesFitTheBanner() {
    showScenes {
      LongMessages.forEachIndexed { index, message -> Scene(index, message, secondaryMessage = null) }
    }
    LongMessages.forEachIndexed { index, message -> assertTextFits(index, message) }
  }

  @Test
  fun bothLinesFitWithASecondaryMessage() {
    showScenes { Scene(0, PrimaryLine, SecondaryLine) }
    assertTextFits(0, PrimaryLine)
    assertTextFits(0, SecondaryLine)
  }

  @Test
  fun sceneStaysWithinABoundedParent() {
    val parentHeight = 150.dp
    showScenes {
      Box(Modifier.height(parentHeight)) { Scene(0, LongMessages.first(), SecondaryLine) }
    }
    val scene = compose.onNodeWithTag(sceneTag(0)).getBoundsInRoot()
    val sceneHeight = scene.bottom - scene.top
    val sceneWidth = scene.right - scene.left
    assertTrue("scene height $sceneHeight exceeds its parent's $parentHeight", sceneHeight <= parentHeight + OnePixel)
    assertTrue("scene width $sceneWidth exceeds $NarrowWidth", sceneWidth <= NarrowWidth + OnePixel)
  }

  private fun showScenes(content: @Composable () -> Unit) {
    compose.setContent {
      val density = LocalDensity.current
      CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
        PrismTheme {
          // Scrolls so each scene gets its natural height instead of the space left on screen.
          Column(Modifier.width(NarrowWidth).verticalScroll(rememberScrollState())) { content() }
        }
      }
    }
    compose.waitForIdle()
  }

  @Composable
  private fun Scene(index: Int, message: String, secondaryMessage: String?) {
    CelebrationScene(
      character = if (index % 2 == 0) MascotCharacter.Lynx else MascotCharacter.Rosie,
      message = message,
      secondaryMessage = secondaryMessage,
      animated = false,
      entrance = { 1f },
      bannerProgress = { 1f },
      modifier = Modifier.testTag(sceneTag(index)),
    )
  }

  private fun assertTextFits(index: Int, text: String) {
    val node = compose.onNodeWithText(text, useUnmergedTree = true)
    val layout = node.textLayout()
    assertFalse("\"$text\" overflows its width", layout.didOverflowWidth)
    assertFalse("\"$text\" overflows its height", layout.didOverflowHeight)
    assertFalse("\"$text\" is ellipsized", (0 until layout.lineCount).any(layout::isLineEllipsized))
    val scene = compose.onNodeWithTag(sceneTag(index)).boundsInRoot()
    val textBounds = node.boundsInRoot()
    assertTrue("\"$text\" at $textBounds leaves its scene at $scene", scene.encloses(textBounds))
  }

  private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
    val results = mutableListOf<TextLayoutResult>()
    fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
    return results.single()
  }

  /** Unclipped, so scenes scrolled below the screen still report their full bounds. */
  private fun SemanticsNodeInteraction.boundsInRoot(): Rect =
    fetchSemanticsNode().let { Rect(it.positionInRoot, it.size.toSize()) }

  private fun Rect.encloses(other: Rect): Boolean =
    other.left >= left - Tolerance && other.top >= top - Tolerance &&
      other.right <= right + Tolerance && other.bottom <= bottom + Tolerance

  private companion object {
    const val Tolerance = 0.5f

    /** Bounds come back in dp converted from whole pixels. */
    val OnePixel = 1.dp
    val NarrowWidth = 320.dp
    const val PrimaryLine = "Paper Rex hat gewonnen und"
    const val SecondaryLine = "Something hat abgeliefert!!!"
    val LongMessages = listOf(
      "Natus Vincere hat das Event gewonnen!",
      "Победитель турнира — Natus Vincere!",
      "Natus Vincere ने इवेंट जीत लिया!",
      "Gen.G Global Esports 팀이 이벤트에서 우승했습니다!",
    )

    fun sceneTag(index: Int) = "celebration-scene-$index"
  }
}

class MascotCelebrationLayoutTestActivity : ComponentActivity()
