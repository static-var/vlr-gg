/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.PrismTheme
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

internal class PrismTableLayoutTest {
  @get:Rule
  val compose = createComposeRule()

  @Test(timeout = 30_000)
  fun scrollingKeepsLaterStickyCellsAlignedWithoutRevisitingRows() {
    val rows = CountingRows(
      List(12) { index ->
        PrismTableRow("row-$index", mapOf("team" to "Team $index", "score" to "$index"))
      },
    )
    compose.setContent {
      PrismTheme {
        PrismTable(
          columns = listOf(PrismTableColumn("team", "Team"), PrismTableColumn("score", "Score")),
          rows = rows,
          modifier = Modifier.size(width = 300.dp, height = 200.dp),
          options = PrismTableOptions(cellPadding = PaddingValues(0.dp)),
          cellContentResolver = PrismTableCellContentResolver { context, _ ->
            {
              val prefix = if (context.isStickyColumn) "sticky" else "main-${context.columnIndex}"
              Box(Modifier.fillMaxSize().testTag("$prefix-${context.rowIndex}"))
            }
          },
        )
      }
    }
    compose.waitForIdle()
    val initialReads = rows.readCount

    compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
      .performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, 10_000f) }

    compose.onNodeWithTag("sticky-11").assertIsDisplayed().assertHeightIsEqualTo(48.dp)
    val stickyBounds = compose.onNodeWithTag("sticky-11").fetchSemanticsNode().boundsInRoot
    val mainBounds = compose.onNodeWithTag("main-1-11").fetchSemanticsNode().boundsInRoot
    assertEquals(mainBounds.top, stickyBounds.top, "The final sticky row must track the scrolling body")
    assertEquals(initialReads, rows.readCount, "Scrolling must place existing cells without traversing rows again")
  }

  private class CountingRows(private val rows: List<PrismTableRow>) : AbstractList<PrismTableRow>() {
    var readCount: Int = 0
      private set

    override val size: Int get() = rows.size

    override fun get(index: Int): PrismTableRow {
      readCount++
      return rows[index]
    }
  }
}
