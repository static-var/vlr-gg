package dev.staticvar.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

class StartupProfileGenerator {
  @get:Rule
  val baselineProfileRule = BaselineProfileRule()

  @Test
  fun startup() =
    baselineProfileRule.collect(
      packageName = "dev.staticvar.vlr",
      maxIterations = 5,
      includeInStartupProfile = true
    ) { startActivityAndWait() }
}
