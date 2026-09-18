/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import dev.staticvar.designsystem.component.tag.PrismTagStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.format_status_completed
import vlr.shared_ui.generated.resources.format_status_ongoing
import vlr.shared_ui.generated.resources.format_status_paused
import vlr.shared_ui.generated.resources.format_status_unknown
import vlr.shared_ui.generated.resources.format_status_upcoming

class MatchDetailStatusTest {
  @Test
  fun finalAndTbdUseCompletedAndUpcomingLabelsAndStyles() {
    assertEquals(Res.string.format_status_completed, " FINAL ".matchDetailStatusLabelResource)
    assertEquals(PrismTagStyle.Success, " FINAL ".matchDetailStatusTagStyle)
    assertEquals(Res.string.format_status_upcoming, " TbD ".matchDetailStatusLabelResource)
    assertEquals(PrismTagStyle.Info, " TbD ".matchDetailStatusTagStyle)
  }

  @Test
  fun ongoingAndPausedKeepDistinctLabelsAndStyles() {
    assertEquals(Res.string.format_status_ongoing, "ongoing".matchDetailStatusLabelResource)
    assertEquals(PrismTagStyle.Danger, "ongoing".matchDetailStatusTagStyle)
    assertEquals(Res.string.format_status_paused, "paused".matchDetailStatusLabelResource)
    assertEquals(PrismTagStyle.Neutral, "paused".matchDetailStatusTagStyle)
    assertEquals(Res.string.format_status_unknown, null.matchDetailStatusLabelResource)
    assertEquals(PrismTagStyle.Neutral, null.matchDetailStatusTagStyle)
  }
}
