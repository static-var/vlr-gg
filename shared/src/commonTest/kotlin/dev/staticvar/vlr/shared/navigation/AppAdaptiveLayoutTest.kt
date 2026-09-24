/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals

class AppAdaptiveLayoutTest {
  @Test
  fun navigationChangesAtMediumWidthWhileContentStaysSinglePane() {
    assertEquals(AppAdaptiveLayout(false, false), layout(599, 900))
    assertEquals(AppAdaptiveLayout(true, false), layout(600, 900))
    assertEquals(AppAdaptiveLayout(true, false), layout(839, 900))
  }

  @Test
  fun relatedContentUsesTwoPanesFromExpandedWidth() {
    for (width in listOf(840, 919, 920, 1119, 1120, 1200, 1600)) {
      assertEquals(AppAdaptiveLayout(true, true), layout(width, 900), "width=$width")
    }
  }

  @Test
  fun compactHeightKeepsBottomNavigationAndSinglePane() {
    for (width in listOf(600, 840, 1200)) {
      assertEquals(AppAdaptiveLayout(false, false), layout(width, 479), "width=$width")
    }
    assertEquals(AppAdaptiveLayout(true, true), layout(840, 480))
  }

  @Test
  fun tabletopUsesBottomNavigationWithoutSideBySideContent() {
    assertEquals(AppAdaptiveLayout(false, false), layout(1200, 900, tabletop = true))
    assertEquals(AppAdaptiveLayout(true, true), layout(1200, 900, tabletop = false))
  }

  @Test
  fun ipadRotationChangesPanesWithoutChangingNavigation() {
    assertEquals(AppAdaptiveLayout(true, false), layout(834, 1210))
    assertEquals(AppAdaptiveLayout(true, true), layout(1210, 834))
  }

  private fun layout(width: Int, height: Int, tabletop: Boolean = false): AppAdaptiveLayout = appAdaptiveLayout(
    WindowAdaptiveInfo(
      WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(widthDp = width, heightDp = height),
      Posture(isTabletop = tabletop),
    ),
  )
}
