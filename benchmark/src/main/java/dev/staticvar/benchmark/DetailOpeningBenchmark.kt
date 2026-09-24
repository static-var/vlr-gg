/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.benchmark

import android.os.SystemClock
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
public class DetailOpeningBenchmark {
  @get:Rule
  public val benchmarkRule: MacrobenchmarkRule = MacrobenchmarkRule()

  @Test public fun matchFirstOpening(): Unit = measureOpening(Detail.Match, 0)
  @Test public fun matchSecondOpening(): Unit = measureOpening(Detail.Match, 1)
  @Test public fun matchThirdOpening(): Unit = measureOpening(Detail.Match, 2)
  @Test public fun eventFirstOpening(): Unit = measureOpening(Detail.Event, 0)
  @Test public fun eventSecondOpening(): Unit = measureOpening(Detail.Event, 1)
  @Test public fun eventThirdOpening(): Unit = measureOpening(Detail.Event, 2)

  private fun measureOpening(detail: Detail, warmups: Int) {
    lateinit var selectedCard: UiObject2
    benchmarkRule.measureRepeated(
      packageName = TARGET_PACKAGE,
      metrics = listOf(FrameTimingMetric()),
      compilationMode = CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require),
      iterations = benchmarkIterations(),
      setupBlock = {
        killProcess()
        pressHome()
        startAndAssertHome()
        val navigation = device.requireDetailObject(By.desc(detail.navigation))
        navigation.click()
        device.requireDetailObject(By.text(detail.listSubtitle))
        device.requireDetailObject(By.clickable(true).hasDescendant(By.text("Completed"))).click()
        device.requireDetailObject(By.selected(true).hasDescendant(By.text("Completed")))
        device.waitForIdle()
        val cardText = device.firstDetailCard(detail).textSignature()
        check(cardText.isNotEmpty()) { "Expected text identifying the selected card" }
        repeat(warmups) {
          val card = device.firstDetailCard(detail)
          check(card.textSignature() == cardText) { "Selected card changed during warm-up" }
          device.openDetail(detail, card)
          device.waitForIdle()
          device.pressBack()
          device.requireDetailObject(By.text(detail.listSubtitle))
          device.waitForIdle()
        }
        selectedCard = device.firstDetailCard(detail)
        check(selectedCard.textSignature() == cardText) {
          "List changed during setup; cannot compare openings of the same card"
        }
      },
    ) {
      device.openDetail(detail, selectedCard)
    }
  }
}

private enum class Detail(val navigation: String, val listSubtitle: String, val detailSubtitle: String) {
  Match("Matches", "// RESULTS, SCHEDULES AND LIVE SCORES", "// MAPS, SCORES AND PLAYER STATS"),
  Event("Events", "// EVENTS AROUND THE WORLD", "// TEAMS, MATCHES AND STANDINGS"),
}

private fun UiDevice.firstDetailCard(detail: Detail): UiObject2 {
  val selector = when (detail) {
    Detail.Match -> By.clickable(true).longClickable(true)
    Detail.Event -> By.clickable(true).hasDescendant(By.text("COMPLETED"))
  }
  return checkNotNull(wait({ device ->
    device.findObjects(selector).firstOrNull {
      val bounds = it.visibleBounds
      bounds.height() > 80 && bounds.top > 0 && bounds.bottom < device.displayHeight
    }
  }, 30_000L)) { "No completed $detail card available; check backend connectivity and English locale" }
}

private fun UiObject2.textSignature(): List<String> =
  findObjects(By.clazz("android.widget.TextView")).mapNotNull { it.text }

private fun UiDevice.openDetail(detail: Detail, card: UiObject2) {
  card.click()
  // Keep an identical opening window even if loading indicators never become idle.
  SystemClock.sleep(1_000L)
  check(hasObject(By.text(detail.detailSubtitle))) { "Detail screen did not open within the one-second window" }
  check(!hasObject(By.text(detail.listSubtitle))) {
    "Use a compact phone layout; tablet panes do not run the shared transition"
  }
}

private fun UiDevice.requireDetailObject(selector: BySelector): UiObject2 =
  checkNotNull(wait(Until.findObject(selector), 30_000L)) { "Expected detail benchmark UI: $selector" }
