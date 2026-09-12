/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.baselineprofile

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until

internal fun configureProfileSettings() {
  val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
  device.launchProfileApp()
  device.findObject(By.desc("Show results and stats throughout app"))?.click(100)
  device.settingsObject(By.desc("Hide results and stats throughout app"))
  device.openMascotSettings()
  device.settingsObject(By.text("Companion")).click(100)
  device.settingsObject(By.text("Lynx · Cat")).click(100)
  device.scrollSettingsTo("YES")
  device.settingsObject(By.text("YES")).click(100)
  device.verifyMascotSettings()

  device.executeShellCommand("am force-stop $TARGET_PACKAGE")
  device.launchProfileApp()
  device.openMascotSettings()
  device.verifyMascotSettings()
  device.pressBack()
  device.settingsObject(By.desc("Settings"))
  device.pressHome()
}

private fun UiDevice.launchProfileApp() {
  executeShellCommand("am start -W -n $TARGET_PACKAGE/dev.staticvar.vlr.android.MainActivity")
  settingsObject(By.desc("Settings"))
}

private fun UiDevice.openMascotSettings() {
  settingsObject(By.desc("Settings")).click(100)
  settingsObject(By.text("// MAKE VLR YOURS."))
  scrollSettingsTo("Companion")
}

private fun UiDevice.verifyMascotSettings() {
  settingsObject(By.text("Lynx · Cat"))
  scrollSettingsTo("YES")
  waitForIdle()
  val range = settingsObject(By.clazz("android.widget.SeekBar")).accessibilityNodeInfo.rangeInfo
  check(range != null && range.max == 3f && range.current == range.max) {
    "Mascot Surprise visits must be set to YES, the highest frequency"
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
