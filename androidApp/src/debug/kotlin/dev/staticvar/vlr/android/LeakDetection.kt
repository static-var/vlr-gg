/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android

import leakcanary.LeakCanary
import shark.AndroidObjectInspectors
import shark.ObjectInspector

internal fun configureLeakDetection() {
  LeakCanary.config = LeakCanary.config.copy(
    objectInspectors = LeakCanary.config.objectInspectors.map { inspector ->
      if (inspector == AndroidObjectInspectors.COMPOSITION_IMPL) compositionInspector else inspector
    },
  )
}

// LeakCanary 2.14 assumes a removed field: https://github.com/square/leakcanary/pull/2797
private val compositionInspector = ObjectInspector { reporter ->
  reporter.whenInstanceOf("androidx.compose.runtime.CompositionImpl") { instance ->
    when (val state = instance["androidx.compose.runtime.CompositionImpl", "state"]?.value?.asInt) {
      0 -> notLeakingReasons += "Composition running"
      1 -> labels += "Composition deactivated"
      2 -> labels += "Composition inconsistent"
      3 -> leakingReasons += "Composition disposed"
      else -> labels += "Composition state: $state"
    }
  }
}
