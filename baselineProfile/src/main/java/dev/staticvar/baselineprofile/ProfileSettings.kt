/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.baselineprofile

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until

internal fun configureProfileSettings() {
  val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
  device.launchProfileApp()
  device.tapSettingsIfPresent(By.desc("Show results and stats throughout app"))
  device.settingsObject(By.desc("Hide results and stats throughout app"))
  device.openMascotSettings()
  device.tapSettings(By.text("Companion"))
  device.tapSettings(By.text("Lynx · Cat"))
  device.scrollSettingsTo("YES")
  device.tapSettings(By.text("YES"))
  device.verifyMascotSettings()

  device.executeShellCommand("am force-stop $TARGET_PACKAGE")
  device.launchProfileApp()
  device.openMascotSettings()
  device.verifyMascotSettings()
  device.pressBack()
  device.settingsObject(By.text("Appearance"))
  device.pressBack()
  device.settingsObject(By.desc("Settings"))
  device.pressHome()
}

private fun UiDevice.launchProfileApp() {
  executeShellCommand("am force-stop $TARGET_PACKAGE")
  executeShellCommand("am start -W -n $TARGET_PACKAGE/dev.staticvar.vlr.android.MainActivity")
  settingsObject(By.desc("Settings"))
}

private fun UiDevice.openMascotSettings() {
  tapSettings(By.desc("Settings"))
  settingsObject(By.text("// MAKE IT YOURS."))
  tapSettings(By.clickable(true).hasDescendant(By.text("Appearance")))
  scrollSettingsTo("Companion")
}

private fun UiDevice.verifyMascotSettings() {
  settingsObject(By.text("Lynx · Cat"))
  scrollSettingsTo("YES")
  waitForIdle()
  val slider = settingsObject(By.clazz("android.widget.SeekBar")).accessibilityNodeInfo
  val range = slider.rangeInfo
  check(range != null && range.max > range.min && range.current == range.max) {
    "Mascot Surprise visits must be set to YES, the highest frequency; " +
      "range=${range?.min}..${range?.max}, current=${range?.current}, state=${slider.stateDescription}"
  }
}

private fun UiDevice.scrollSettingsTo(text: String) {
  waitForIdle()
  UiScrollable(UiSelector().scrollable(true)).apply {
    setAsVerticalList()
    setMaxSearchSwipes(6)
  }.scrollIntoView(UiSelector().text(text))
  settingsObject(By.text(text))
}

private fun UiDevice.settingsObject(selector: BySelector): UiObject2 {
  return checkNotNull(wait(Until.findObject(selector), 10_000L)) {
    "Expected profile setting was not displayed: $selector"
  }
}

private fun UiDevice.tapSettingsIfPresent(selector: BySelector): Boolean {
  if (!hasObject(selector)) return false
  tapSettings(selector)
  return true
}

private fun UiDevice.tapSettings(selector: BySelector) {
  repeat(3) {
    try {
      settingsObject(selector).click(100)
      waitForIdle()
      return
    } catch (_: StaleObjectException) {
      // The dropdown or navigation transition replaced the node; tap its current replacement.
    }
  }
  error("Profile setting did not remain stable long enough to tap: $selector")
}
