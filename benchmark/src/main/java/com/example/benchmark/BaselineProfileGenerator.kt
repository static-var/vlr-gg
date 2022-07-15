package com.example.benchmark

import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test

@ExperimentalBaselineProfilesApi
class BaselineProfileGenerator {
  @get:Rule
  val baselineProfileRule = BaselineProfileRule()

  @Test
  fun startup() =
    baselineProfileRule.collectBaselineProfile(
      packageName = "dev.staticvar.vlr"
    ) {
      pressHome()
      startActivityAndWait()
      device.waitForIdle()

      device.findObject(By.text("Matches")).click()
      device.waitForIdle()

      device.findObject(By.text("Events")).click()
      device.waitForIdle()
    }
}