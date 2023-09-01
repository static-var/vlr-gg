package com.example.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

class BaselineProfileGenerator {
  @get:Rule val baselineProfileRule = BaselineProfileRule()

  @Test
  fun startup() =
    baselineProfileRule.collect(packageName = "dev.staticvar.vlr", maxIterations = 5) { userflow() }
}
