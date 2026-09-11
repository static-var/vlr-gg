/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.icon

import dev.staticvar.designsystem.prism.icon.news.StairStepNews
import dev.staticvar.designsystem.prism.icon.news.StairStepNewsFilled
import dev.staticvar.designsystem.prism.icon.matches.StairStepMatches
import dev.staticvar.designsystem.prism.icon.matches.StairStepMatchesFilled
import dev.staticvar.designsystem.prism.icon.events.StairStepEvents
import dev.staticvar.designsystem.prism.icon.events.StairStepEventsFilled
import dev.staticvar.designsystem.prism.icon.rankings.StairStepRankings
import dev.staticvar.designsystem.prism.icon.rankings.StairStepRankingsFilled
import dev.staticvar.designsystem.prism.icon.settings.StairStepSettings

internal val ConsoleIcons: PrismIcons by lazy {
  PrismIcons(
    refresh = prismIconArtwork(
      name = "ConsoleRefresh",
      fill = "M8 2 H16 V4 H18 V6 H20 V2 H22 V10 H14 V8 H18 V6 H16 V4 H8 V6 H6 V10 H4 V6 H6 V4 H8 Z M20 14 V18 H18 V20 H16 V22 H8 V20 H6 V18 H4 V22 H2 V14 H10 V16 H6 V18 H8 V20 H16 V18 H18 V14 Z",
    ),
    share = prismIconArtwork(
      name = "ConsoleShare",
      fill = "M11 2 H13 V4 H15 V6 H17 V8 H15 V6 H13 V16 H11 V6 H9 V8 H7 V6 H9 V4 H11 Z M3 12 H5 V20 H19 V12 H21 V22 H3 Z",
    ),
    preview = prismIconArtwork(
      name = "ConsolePreview",
      fill = "M8 4 H16 V6 H20 V8 H22 V10 H24 V14 H22 V16 H20 V18 H16 V20 H8 V18 H4 V16 H2 V14 H0 V10 H2 V8 H4 V6 H8 Z M8 6 V8 H4 V10 H2 V14 H4 V16 H8 V18 H16 V16 H20 V14 H22 V10 H20 V8 H16 V6 Z M10 8 H14 V10 H16 V14 H14 V16 H10 V14 H8 V10 H10 Z",
    ),
    back = prismIconArtwork(
      name = "ConsoleBack",
      fill = "M8 4 H12 V8 H8 V10 H22 V14 H8 V16 H12 V20 H8 V18 H6 V16 H4 V14 H2 V10 H4 V8 H6 V6 H8 Z",
      autoMirror = true,
    ),
    home = PrismNavigationIcons(
      unselected = prismIconArtwork(
        name = "ConsoleHome",
        fill = "M10 2 H14 V4 H16 V6 H18 V8 H20 V10 H22 V22 H14 V16 H10 V22 H2 V10 H4 V8 H6 V6 H8 V4 H10 Z M10 6 V8 H8 V10 H6 V12 H4 V20 H8 V14 H16 V20 H20 V12 H18 V10 H16 V8 H14 V6 Z",
      ),
      selected = prismIconArtwork(
        name = "ConsoleHomeSelected",
        fill = "M10 2 H14 V4 H16 V6 H18 V8 H20 V10 H22 V22 H14 V16 H10 V22 H2 V10 H4 V8 H6 V6 H8 V4 H10 Z",
      ),
    ),
    news = PrismNavigationIcons(StairStepNews, StairStepNewsFilled),
    matches = PrismNavigationIcons(StairStepMatches, StairStepMatchesFilled),
    events = PrismNavigationIcons(StairStepEvents, StairStepEventsFilled),
    rankings = PrismNavigationIcons(StairStepRankings, StairStepRankingsFilled),
    settings = PrismNavigationIcons(StairStepSettings, StairStepSettings),
  )
}
