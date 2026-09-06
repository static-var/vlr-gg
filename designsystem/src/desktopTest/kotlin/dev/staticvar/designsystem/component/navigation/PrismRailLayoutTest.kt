/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.HomeSolid
import dev.staticvar.designsystem.prism.PrismTheme
import org.junit.Rule
import org.junit.Test

internal class PrismRailLayoutTest {
  @get:Rule
  val compose = createComposeRule()

  @Test
  fun railLeavesRemainingWidthForWeightedScreenContent() {
    compose.setContent {
      PrismTheme {
        Row(Modifier.requiredSize(width = 840.dp, height = 480.dp)) {
          PrismBottomNavBarLarge(
            items = listOf(
              PrismBottomNavItem(
                id = "matches",
                label = "Matches",
                icon = LineAwesomeIcons.HomeSolid,
                selectedIcon = LineAwesomeIcons.HomeSolid,
              ),
            ),
            selectedItemId = "matches",
            onItemSelected = {},
            modifier = Modifier.testTag("rail"),
          )
          Box(Modifier.weight(1f).fillMaxHeight().testTag("content"))
        }
      }
    }

    compose.onNodeWithTag("rail").assertWidthIsEqualTo(220.dp)
    compose.onNodeWithTag("content").assertWidthIsEqualTo(620.dp)
  }
}
