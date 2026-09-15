/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.benchmark

import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

internal const val TARGET_PACKAGE: String = "dev.staticvar.vlr"

internal fun benchmarkIterations(): Int {
  val configured = InstrumentationRegistry.getArguments().getString(BENCHMARK_ITERATIONS_ARGUMENT)
    ?: return DEFAULT_BENCHMARK_ITERATIONS
  return requireNotNull(configured.toIntOrNull()?.takeIf { it > 0 }) {
    "$BENCHMARK_ITERATIONS_ARGUMENT must be a positive integer, but was '$configured'"
  }
}

internal fun MacrobenchmarkScope.startAndAssertHome() {
  startActivityAndWait()
  device.assertHomeScreen()
}

internal fun UiDevice.navigateSettingsAndAbout() {
  requireObject(By.desc("Settings")).click(100)
  requireObject(By.text("// MAKE IT YOURS."))
  waitForIdle(UI_IDLE_TIMEOUT)

  val aboutTitle = By.text("About Val Esports")
  scrollTo(aboutTitle)
  requireObject(By.clickable(true).hasDescendant(aboutTitle)).activate()
  requireObject(By.text("About"))
  waitForIdle(UI_IDLE_TIMEOUT)

  pressBack()
  requireObject(By.text("// MAKE IT YOURS."))
  waitForIdle(UI_IDLE_TIMEOUT)

  pressBack()
  assertHomeScreen()
  waitForIdle(UI_IDLE_TIMEOUT)
}

private fun UiDevice.assertHomeScreen() {
  requireObject(By.desc("Home"))
  requireObject(By.desc("Settings"))
}

private fun UiDevice.scrollTo(selector: BySelector): UiObject2 {
  repeat(MAX_SCROLL_ATTEMPTS) {
    findVisibleObject(selector)?.let { return it }
    val list = requireScrollableContent()
    val bounds = list.visibleBounds
    swipe(
      bounds.centerX(),
      bounds.bottom - bounds.height() / 5,
      bounds.centerX(),
      bounds.top + bounds.height() / 5,
      SCROLL_STEPS,
    )
    waitForIdle(UI_IDLE_TIMEOUT)
  }
  return checkNotNull(findVisibleObject(selector)) {
    "Expected VLR UI element was not visible after scrolling: $selector"
  }
}

private fun UiDevice.requireScrollableContent(): UiObject2 = checkNotNull(
  findObjects(By.scrollable(true))
    .filter { it.visibleBounds.height() > displayHeight / 3 }
    .lastOrNull(),
) {
  "Expected a vertical VLR settings list"
}

private fun UiDevice.findVisibleObject(selector: BySelector): UiObject2? =
  findObject(selector)?.takeIf {
    val bounds: Rect = it.visibleBounds
    !bounds.isEmpty && bounds.bottom > 0 && bounds.top < displayHeight
  }

private fun UiDevice.requireObject(selector: BySelector): UiObject2 =
  checkNotNull(wait(Until.findObject(selector), UI_ELEMENT_TIMEOUT)) {
    val visibleText = findObjects(By.clazz("android.widget.TextView")).map { it.text }.take(12)
    "Expected VLR UI element was not displayed: $selector. Visible text: $visibleText"
  }

private fun UiObject2.activate() {
  SystemClock.sleep(100)
  check(accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
    "The selected control did not accept its accessibility click action"
  }
}

private const val BENCHMARK_ITERATIONS_ARGUMENT: String = "benchmarkIterations"
private const val DEFAULT_BENCHMARK_ITERATIONS: Int = 5
private const val MAX_SCROLL_ATTEMPTS: Int = 12
private const val SCROLL_STEPS: Int = 30
private const val UI_ELEMENT_TIMEOUT: Long = 10_000L
private const val UI_IDLE_TIMEOUT: Long = 2_000L
