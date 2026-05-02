/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.ui.helper

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import dev.staticvar.vlr.ui.LocalNavigationSuiteType

@Composable
fun ShowIfLargeFormFactorDevice(content: @Composable () -> Unit) {
  val navigationType = LocalNavigationSuiteType.current

  when (navigationType) {
    NavigationSuiteType.None,
    NavigationSuiteType.NavigationBar,
    -> {}

    NavigationSuiteType.NavigationRail,
    NavigationSuiteType.NavigationDrawer,
    -> content()
  }
}
