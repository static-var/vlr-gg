/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.selection

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

internal class PrismSegmentedButtonsLayoutTest {
  @get:Rule
  val compose = createComposeRule()

  @Test
  fun consoleSelectionKeepsGroupBoundsAndDisabledSemantics() {
    compose.setContent {
      PrismTheme(family = PrismThemeFamily.Console) {
        var selected by remember { mutableStateOf("all") }
        PrismSegmentedButtons(
          options = listOf(
            PrismSegmentedButtonOption("all", "All"),
            PrismSegmentedButtonOption("live", "Live"),
            PrismSegmentedButtonOption("ended", "Ended", enabled = false),
          ),
          selectedOptionId = selected,
          onOptionSelected = { selected = it.id },
          modifier = Modifier.width(300.dp).testTag("group"),
        )
      }
    }
    val bounds = compose.onNodeWithTag("group").fetchSemanticsNode().boundsInRoot
    compose.onNodeWithText("All").assertIsSelected()
    compose.onNodeWithText("Ended").assertIsNotEnabled()
    compose.onNodeWithText("Live").performClick().assertIsSelected()
    assertEquals(bounds, compose.onNodeWithTag("group").fetchSemanticsNode().boundsInRoot)
  }
}
