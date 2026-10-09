/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.vlr.sharedui.component.event.ProvideEventTransitionScope
import dev.staticvar.vlr.sharedui.component.event.rememberEventSharedText
import dev.staticvar.vlr.sharedui.component.match.MatchSharedContent
import dev.staticvar.vlr.sharedui.component.match.ProvideMatchTransitionScope
import dev.staticvar.vlr.sharedui.component.match.rememberMatchSharedText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(AndroidJUnit4::class)
class SharedTransitionTextTest {
  @get:Rule
  val compose = createAndroidComposeRule<SharedTransitionTextTestActivity>()

  @Test
  fun matchLabelDrawsOnceAndChangesSizeAndColorInBothDirections() {
    verifyRoundTrip(Kind.Match)
  }

  @Test
  fun eventLabelDrawsOnceAndChangesSizeAndColorInBothDirections() {
    verifyRoundTrip(Kind.Event)
  }

  @Test
  fun reversingBeforeCompletionStillDrawsOneLabelAndReturnsToListStyle() {
    val fixture = showFixture(Kind.Match)
    val list = capture(fixture)
    compose.runOnIdle { fixture.details = true }
    compose.mainClock.advanceTimeBy(96)
    assertOneDraw(fixture, "forward before reversal")
    compose.runOnIdle { fixture.details = false }
    compose.mainClock.advanceTimeBy(64)
    assertOneDraw(fixture, "reversed transition")
    compose.mainClock.advanceTimeBy(800)
    val returned = capture(fixture)
    assertEquals("reversal must restore list glyph height", list.height, returned.height)
    assertTrue("reversal must restore red list text: $returned", returned.red > 0.9f && returned.blue < 0.1f)
  }

  @Test
  fun unmatchedTextAndDisabledSharedScopeRemainVisible() {
    val fixture = showFixture(Kind.Event, matched = false)
    assertTrue("unmatched list text must draw", capture(fixture).height > 0)
    compose.runOnIdle { fixture.details = true }
    compose.mainClock.advanceTimeBy(800)
    assertTrue("unmatched detail text must draw", capture(fixture).height > 0)
    compose.runOnIdle { fixture.enabled = false }
    assertTrue("text must draw when shared transitions are disabled", capture(fixture).height > 0)
  }

  @Test
  fun unmatchedTextDoesNotExtendTheNavigationFade() {
    val fixture = showFixture(Kind.Event, matched = false, navigationFadeMillis = 180)
    compose.runOnIdle { fixture.details = true }
    compose.mainClock.advanceTimeByFrame()
    compose.mainClock.advanceTimeBy(180)
    val detail = assertOneDraw(fixture, "unmatched navigation after its 180ms fade")
    assertTrue("unmatched text must already have its final color: $detail", detail.blue > 0.9f)
    assertEquals(setOf(true), fixture.renderedEndpoints)
  }

  @Test
  fun seekingBackAndCancellingKeepsOneLabelAndRestoresDetailStyle() {
    val fixture = showFixture(Kind.Event, seekable = true)
    compose.runOnIdle { fixture.scope.launch { fixture.seekableState.snapTo(true) } }
    compose.mainClock.advanceTimeByFrame()
    val detail = capture(fixture)

    compose.runOnIdle { fixture.scope.launch { fixture.seekableState.seekTo(0f, false) } }
    compose.mainClock.advanceTimeByFrame()
    compose.runOnIdle { fixture.scope.launch { fixture.seekableState.seekTo(0.5f, false) } }
    compose.mainClock.advanceTimeByFrame()
    val halfway = assertOneDraw(fixture, "predictive back at 50 percent")
    assertTrue("seeking must reduce the glyph size: $halfway", halfway.height < detail.height)
    assertTrue("seeking must interpolate text color: $halfway", halfway.red > 0.1f && halfway.blue > 0.1f)

    compose.runOnIdle { fixture.scope.launch { fixture.seekableState.seekTo(0.2f, false) } }
    compose.mainClock.advanceTimeByFrame()
    assertOneDraw(fixture, "predictive back after reversing the gesture")
    compose.runOnIdle { fixture.scope.launch { fixture.seekableState.animateTo(true) } }
    compose.mainClock.advanceTimeBy(800)
    val cancelled = assertOneDraw(fixture, "cancelled predictive back")
    assertEquals("cancelling back must restore detail glyph height", detail.height, cancelled.height)
    assertTrue("cancelling back must restore blue detail text: $cancelled", cancelled.blue > 0.9f)
  }

  @Test
  fun longTeamNameScalesFromWeightedListToSingleLineDetailAndBack() {
    val fixture = showFixture(Kind.Match, weightedName = true)
    val list = capture(fixture)
    compose.runOnIdle { fixture.details = true }
    compose.mainClock.advanceTimeBy(32)
    val firstEntering = assertOneDraw(fixture, "long team name first entering frame")
    listOf(96L, 128L).forEach { elapsed ->
      compose.mainClock.advanceTimeBy(elapsed)
      val entering = assertOneDraw(fixture, "long team name entering details")
      if (elapsed == 96L) {
        assertTrue("long name must interpolate color on entry: $entering", entering.red > 0.1f && entering.blue > 0.1f)
      }
    }
    compose.mainClock.advanceTimeBy(800)
    val detail = capture(fixture)
    assertTrue("long detail name must shrink to fit its column: $detail", detail.height < list.height)
    assertTrue("detail name must fit its 100dp column: $detail", detail.width <= fixture.detailWidthPixels)
    compose.runOnIdle {
      assertEquals("detail team name must use one line", 1, fixture.detailLineCount)
      assertTrue("detail team name must fit without clipping", !fixture.detailOverflows)
    }
    assertTrue(
      "first entering frame must keep an intermediate glyph size: $firstEntering",
      firstEntering.height in (detail.height + 1)..list.height,
    )
    assertTrue("first entering frame must not snap to detail size", firstEntering.height >= list.height * 0.75f)
    compose.runOnIdle { fixture.details = false }
    listOf(32L, 96L, 128L).forEach { elapsed ->
      compose.mainClock.advanceTimeBy(elapsed)
      val returning = assertOneDraw(fixture, "long team name returning to list")
      if (elapsed == 96L) {
        assertTrue("long name must interpolate color on pop: $returning", returning.red > 0.1f && returning.blue > 0.1f)
      }
      assertTrue(
        "pop glyph must stay between endpoint sizes: $returning",
        returning.height in detail.height..list.height,
      )
    }
    compose.mainClock.advanceTimeBy(800)
    assertEquals("pop must restore list name size", list.height, capture(fixture).height)
  }

  @Test
  fun eventTitleKeepsItsGlyphSizeWhenChangingFromOneLineToTwoLines() {
    val fixture = showFixture(Kind.Event, wrappingTitle = true)
    val list = capture(fixture)
    assertEquals("list title must fit on one line", 1, list.lines)
    compose.runOnIdle { fixture.details = true }
    compose.mainClock.advanceTimeBy(32)
    val first = assertOneDraw(fixture, "title first entering frame")
    assertTrue(
      "changing line count must not shrink the first line: $list -> $first",
      first.firstLineHeight >= list.firstLineHeight * 0.9f,
    )
    listOf(96L, 128L).forEach { elapsed ->
      compose.mainClock.advanceTimeBy(elapsed)
      assertOneDraw(fixture, "title entering details")
    }
    compose.mainClock.advanceTimeBy(800)
    val detail = capture(fixture)
    assertEquals("detail title must show both lines", 2, detail.lines)
    assertTrue("detail title must finish blue", detail.blue > 0.9f && detail.red < 0.1f)
    compose.runOnIdle { fixture.details = false }
    listOf(32L, 96L, 128L).forEach { elapsed ->
      compose.mainClock.advanceTimeBy(elapsed)
      assertOneDraw(fixture, "title returning to list")
    }
    compose.mainClock.advanceTimeBy(800)
    val returned = capture(fixture)
    assertEquals("pop must restore one title line", 1, returned.lines)
    assertEquals("pop must restore list glyph size", list.firstLineHeight, returned.firstLineHeight)
    assertTrue("pop must finish red", returned.red > 0.9f && returned.blue < 0.1f)
  }

  private fun verifyRoundTrip(kind: Kind) {
    val fixture = showFixture(kind)
    val list = capture(fixture)
    compose.runOnIdle { fixture.details = true }
    compose.mainClock.advanceTimeBy(144)
    val entering = assertOneDraw(fixture, "$kind entering details")
    compose.mainClock.advanceTimeBy(800)
    val detail = capture(fixture)
    assertTrue("detail text must be larger: $list -> $detail", detail.height > list.height * 1.7f)
    assertTrue("detail text must end blue: $detail", detail.blue > 0.9f && detail.red < 0.1f)
    assertInterpolated(entering, list, detail, "$kind entering details")

    compose.runOnIdle { fixture.details = false }
    compose.mainClock.advanceTimeBy(144)
    val exiting = assertOneDraw(fixture, "$kind returning to list")
    assertInterpolated(exiting, list, detail, "$kind returning to list")
    compose.mainClock.advanceTimeBy(800)
    val returned = capture(fixture)
    assertEquals("pop must restore list glyph height", list.height, returned.height)
    assertTrue("pop must restore red list text: $returned", returned.red > 0.9f && returned.blue < 0.1f)
  }

  private fun assertInterpolated(actual: Glyph, list: Glyph, detail: Glyph, stage: String) {
    assertTrue("$stage must animate glyph size: $actual", actual.height > list.height && actual.height < detail.height)
    assertTrue("$stage must animate glyph color: $actual", actual.red > 0.1f && actual.blue > 0.1f)
  }

  private fun assertOneDraw(fixture: Fixture, stage: String): Glyph {
    val image = captureImage(fixture)
    assertEquals(
      "$stage must render one Text, visible endpoints=${fixture.renderedEndpoints}",
      1,
      fixture.renderedEndpoints.size,
    )
    return image.glyph()
  }

  private fun capture(fixture: Fixture): Glyph = captureImage(fixture).glyph()

  private fun captureImage(fixture: Fixture): ImageBitmap {
    compose.runOnIdle {
      fixture.frameToken.intValue++
    }
    return compose.onNodeWithTag(CanvasTag).captureToImage().also {
      fixture.renderedEndpoints = it.renderedEndpoints()
    }
  }

  private fun showFixture(
    kind: Kind,
    matched: Boolean = true,
    seekable: Boolean = false,
    navigationFadeMillis: Int = 0,
    weightedName: Boolean = false,
    wrappingTitle: Boolean = false,
  ): Fixture {
    compose.mainClock.autoAdvance = false
    val fixture = Fixture(kind, matched, weightedName, wrappingTitle)
    compose.setContent {
      PrismTheme {
        ProvideSharedTextTransitions {
          val scope = rememberCoroutineScope()
          val detailWidthPixels = with(LocalDensity.current) { 100.dp.roundToPx() }
          SideEffect {
            fixture.scope = scope
            fixture.detailWidthPixels = detailWidthPixels
          }
          SharedTransitionLayout(
            modifier = Modifier.size(320.dp).background(Color.White).testTag(CanvasTag),
          ) {
            val transition = if (seekable) {
              rememberTransition(fixture.seekableState, "seekable navigation")
            } else {
              updateTransition(fixture.details, "navigation")
            }
            transition.AnimatedContent(
              modifier = Modifier.fillMaxSize(),
              transitionSpec = {
                if (navigationFadeMillis == 0) {
                  (EnterTransition.None togetherWith ExitTransition.None).using(null)
                } else {
                  (fadeIn(tween(navigationFadeMillis)) togetherWith fadeOut(tween(navigationFadeMillis))).using(null)
                }
              },
            ) { details ->
              val content: @Composable () -> Unit = {
                Box(
                  modifier = Modifier.fillMaxSize(),
                  contentAlignment = if (details) Alignment.BottomEnd else Alignment.TopStart,
                ) {
                  if (wrappingTitle) {
                    Box(Modifier.width(if (details) 180.dp else 280.dp)) { Label(fixture, details) }
                  } else if (weightedName) {
                    if (details) {
                      Box(Modifier.width(100.dp)) { Label(fixture, details) }
                    } else {
                      Row(Modifier.fillMaxWidth()) {
                        Box(Modifier.weight(2f)) { Label(fixture, details) }
                        Spacer(Modifier.weight(1f))
                      }
                    }
                  } else {
                    Label(fixture, details)
                  }
                }
              }
              when (kind) {
                Kind.Match -> ProvideMatchTransitionScope(this@SharedTransitionLayout, this, fixture.enabled, content)
                Kind.Event -> ProvideEventTransitionScope(this@SharedTransitionLayout, this, fixture.enabled, content)
              }
            }
          }
        }
      }
    }
    return fixture
  }

  @Composable
  private fun Label(fixture: Fixture, details: Boolean) {
    val key = if (fixture.matched || !details) "test-item" else "unmatched-item"
    val color = if (details) Color.Blue else Color.Red
    val fontSize = if (fixture.wrappingTitle) {
      if (details) 24.sp else 16.sp
    } else if (fixture.weightedName) {
      if (details) 16.sp else 24.sp
    } else {
      if (details) 48.sp else 24.sp
    }
    val transition = when (fixture.kind) {
      Kind.Match -> rememberMatchSharedText(key, MatchSharedContent.TeamName, "test-team", color)
      Kind.Event -> rememberEventSharedText(key, color, fontSize.takeIf { fixture.wrappingTitle })
    }
    Text(
      text = if (fixture.wrappingTitle) {
        "OOOOOOOO OOOOOOOO"
      } else if (fixture.weightedName) {
        "Nongshim RedForce"
      } else {
        "O"
      },
      color = transition.color,
      fontSize = fontSize,
      maxLines = if (fixture.wrappingTitle) 2 else 1,
      autoSize = if (fixture.weightedName && details) TextAutoSize.StepBased(8.sp, 16.sp) else null,
      onTextLayout = { layout ->
        if (fixture.weightedName && details) {
          fixture.detailLineCount = layout.lineCount
          fixture.detailOverflows = layout.hasVisualOverflow
        }
      },
      modifier = transition.modifier.drawWithContent {
        fixture.frameToken.intValue
        drawContent()
        val markerSize = 3.dp.toPx()
        drawRect(
          color = if (details) Color.Cyan else Color.Yellow,
          topLeft = if (details) Offset(size.width - markerSize, 0f) else Offset.Zero,
          size = Size(markerSize, markerSize),
        )
      },
    )
  }

  private fun ImageBitmap.renderedEndpoints(): Set<Boolean> {
    val pixels = toPixelMap()
    val endpoints = mutableSetOf<Boolean>()
    for (y in 0 until height) {
      for (x in 0 until width) {
        val pixel = pixels[x, y]
        if (pixel.green > 0.95f) {
          if (pixel.red > 0.95f && pixel.blue < 0.85f) endpoints += false
          if (pixel.blue > 0.95f && pixel.red < 0.85f) endpoints += true
        }
      }
    }
    return endpoints
  }

  private fun ImageBitmap.glyph(): Glyph {
    val pixels = toPixelMap()
    var top = height
    var bottom = -1
    var left = width
    var right = -1
    var red = 0f
    var blue = 0f
    var count = 0
    var coreGreen = 1f
    val inkRows = BooleanArray(height)
    for (y in 0 until height) {
      for (x in 0 until width) {
        val pixel = pixels[x, y]
        if (pixel.green < 0.6f) {
          top = minOf(top, y)
          bottom = maxOf(bottom, y)
          left = minOf(left, x)
          right = maxOf(right, x)
          coreGreen = minOf(coreGreen, pixel.green)
          inkRows[y] = true
        }
      }
    }
    for (y in 0 until height) {
      for (x in 0 until width) {
        val pixel = pixels[x, y]
        if (pixel.green < coreGreen + 0.04f && pixel.green < 0.6f) {
          red += pixel.red
          blue += pixel.blue
          count++
        }
      }
    }
    assertTrue("captured text must have visible glyph pixels", bottom >= top && count > 0)
    val lineHeights = mutableListOf<Int>()
    var currentLineHeight = 0
    inkRows.forEach { ink ->
      if (ink) {
        currentLineHeight++
      } else if (currentLineHeight != 0) {
        lineHeights += currentLineHeight
        currentLineHeight = 0
      }
    }
    if (currentLineHeight != 0) lineHeights += currentLineHeight
    return Glyph(bottom - top + 1, right - left + 1, red / count, blue / count, lineHeights.size, lineHeights.first())
  }

  private data class Glyph(
    val height: Int,
    val width: Int,
    val red: Float,
    val blue: Float,
    val lines: Int,
    val firstLineHeight: Int,
  )

  private class Fixture(val kind: Kind, val matched: Boolean, val weightedName: Boolean, val wrappingTitle: Boolean) {
    var details by mutableStateOf(false)
    var enabled by mutableStateOf(true)
    val frameToken = mutableIntStateOf(0)
    var renderedEndpoints: Set<Boolean> = emptySet()
    val seekableState = SeekableTransitionState(false)
    lateinit var scope: CoroutineScope
    var detailWidthPixels: Int = 0
    var detailLineCount: Int = 0
    var detailOverflows: Boolean = false
  }

  private enum class Kind { Match, Event }

  private companion object {
    const val CanvasTag = "shared-text-canvas"
  }
}

class SharedTransitionTextTestActivity : ComponentActivity()
