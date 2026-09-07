/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

internal class PrismConsoleNavigationTest {
  @get:Rule
  val compose = createComposeRule()

  @Test
  fun framedTabsKeepSelectionBoundsStableAndDisabledFiltersIgnoreClicks() {
    val selectedTab = mutableStateOf("overview")
    val selectedFilter = mutableStateOf("all")
    compose.setContent {
      PrismTheme(family = PrismThemeFamily.Console) {
        Column(Modifier.width(360.dp)) {
          PrismTabs(
            tabs = listOf(PrismTab("overview", "Overview"), PrismTab("stats", "Stats")),
            selectedTabId = selectedTab.value,
            onTabSelected = { selectedTab.value = it.id },
          )
          PrismSegmentedFilterTabs(
            tabs = listOf(
              PrismSegmentedFilterTab("all", "All"),
              PrismSegmentedFilterTab("live", "Live"),
              PrismSegmentedFilterTab("archived", "Archived", enabled = false),
            ),
            selectedTabId = selectedFilter.value,
            onTabSelected = { selectedFilter.value = it.id },
          )
        }
      }
    }
    val overviewBounds = compose.onNodeWithText("Overview").getUnclippedBoundsInRoot()
    val statsBounds = compose.onNodeWithText("Stats").getUnclippedBoundsInRoot()
    val allBounds = compose.onNodeWithText("All").getUnclippedBoundsInRoot()
    val liveBounds = compose.onNodeWithText("Live").getUnclippedBoundsInRoot()

    compose.onNodeWithText("Stats").performClick().assertIsSelected()
    compose.onNodeWithText("Live").performClick().assertIsSelected()
    compose.onNodeWithText("Archived").performClick().assertIsNotSelected()
    compose.waitForIdle()

    assertEquals(overviewBounds, compose.onNodeWithText("Overview").getUnclippedBoundsInRoot())
    assertEquals(statsBounds, compose.onNodeWithText("Stats").getUnclippedBoundsInRoot())
    assertEquals(allBounds, compose.onNodeWithText("All").getUnclippedBoundsInRoot())
    assertEquals(liveBounds, compose.onNodeWithText("Live").getUnclippedBoundsInRoot())
    assertEquals("live", selectedFilter.value)
  }
}
