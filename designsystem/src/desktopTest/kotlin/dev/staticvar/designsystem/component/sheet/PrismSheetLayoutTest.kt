/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.sheet

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

internal class PrismSheetLayoutTest {
  @get:Rule
  val compose = createComposeRule()

  @Test
  fun consoleSheetScrollsContentWithinFixedFrame() {
    compose.setContent {
      PrismTheme(family = PrismThemeFamily.Console) {
        PrismSheet(
          modifier = Modifier.width(300.dp).heightIn(max = 200.dp).testTag("sheet"),
          scrollState = rememberScrollState(),
        ) {
          repeat(30) { Text("Option $it") }
        }
      }
    }
    val bounds = compose.onNodeWithTag("sheet").fetchSemanticsNode().boundsInRoot
    compose.onNodeWithText("Option 29").performScrollTo().assertIsDisplayed()
    assertEquals(bounds, compose.onNodeWithTag("sheet").fetchSemanticsNode().boundsInRoot)
  }
}
