/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowSizeClass

internal data class AppAdaptiveLayout(val showRail: Boolean, val showTwoPanes: Boolean)

internal fun appAdaptiveLayout(info: WindowAdaptiveInfo): AppAdaptiveLayout = AppAdaptiveLayout(
  showRail = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(info) == NavigationSuiteType.NavigationRail,
  showTwoPanes = info.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) &&
    info.windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) &&
    !info.windowPosture.isTabletop,
)
