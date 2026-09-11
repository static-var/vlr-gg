/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.icon

internal val BrutalistIcons: PrismIcons by lazy {
  PrismIcons(
    refresh = prismIconArtwork(
      name = "BrutalistRefresh",
      outline = "M4 9 L8 4 H15 L20 9 M15 9 H20 V4 M20 15 L16 20 H9 L4 15 M9 15 H4 V20",
    ),
    share = prismIconArtwork(
      name = "BrutalistShare",
      outline = "M12 15 V3 M7 8 L12 3 L17 8 M4 13 V21 H20 V13",
    ),
    preview = prismIconArtwork(
      name = "BrutalistPreview",
      outline = "M2 12 L8 5 H16 L22 12 L16 19 H8 Z M9 9 H15 V15 H9 Z",
    ),
    back = prismIconArtwork(
      name = "BrutalistBack",
      outline = "M11 4 L3 12 L11 20 M3 12 H21",
      autoMirror = true,
    ),
    home = brutalistNavigation(
      name = "Home",
      outline = "M3 10 L12 2 L21 10 V21 H15 V14 H9 V21 H3 Z",
      selectedOutline = "",
      selectedFill = "M3 10 L12 2 L21 10 V21 H15 V14 H9 V21 H3 Z",
    ),
    news = brutalistNavigation(
      name = "News",
      outline = "M4 3 H20 V21 H4 Z M8 7 H12 V11 H8 Z M16 7 H17 M16 11 H17 M8 15 H16 M8 18 H14",
      selectedOutline = "M4 3 H20 V21 H4 Z M8 15 H16 M8 18 H14",
      selectedFill = "M7 6 H17 V12 H7 Z",
    ),
    matches = brutalistNavigation(
      name = "Matches",
      outline = "M6 5 H18 L21 9 V19 H16 L14 16 H10 L8 19 H3 V9 Z M6 11 H10 M8 9 V13 M15 10 H16 M18 13 H19",
      selectedOutline = "M6 5 H18 L21 9 V19 H16 L14 16 H10 L8 19 H3 V9 Z",
      selectedFill = "M7 8 H9 V10 H11 V12 H9 V14 H7 V12 H5 V10 H7 Z M14 9 H17 V12 H14 Z M17 12 H20 V15 H17 Z M8 4 H16 V7 H8 Z",
    ),
    events = brutalistNavigation(
      name = "Events",
      outline = "M7 3 H17 V11 L14 15 H10 L7 11 Z M7 5 H3 V10 L7 12 M17 5 H21 V10 L17 12 M12 15 V21 M7 21 H17",
      selectedOutline = "M7 5 H3 V10 L7 12 M17 5 H21 V10 L17 12 M12 15 V21 M7 21 H17",
      selectedFill = "M7 3 H17 V11 L14 15 H10 L7 11 Z",
    ),
    rankings = brutalistNavigation(
      name = "Rankings",
      outline = "M3 21 V12 H8 V21 M9 21 V4 H15 V21 M16 21 V9 H21 V21 M2 21 H22",
      selectedOutline = "M2 21 H22",
      selectedFill = "M3 12 H8 V21 H3 Z M9 4 H15 V21 H9 Z M16 9 H21 V21 H16 Z",
    ),
    settings = brutalistNavigation(
      name = "Settings",
      outline = "M9 2 H15 L16 5 L19 4 L22 9 L19.5 11 V13 L22 15 L19 20 L16 19 L15 22 H9 L8 19 L5 20 L2 15 L4.5 13 V11 L2 9 L5 4 L8 5 Z M15 12 A3 3 0 1 0 9 12 A3 3 0 1 0 15 12 Z",
      selectedOutline = "M9 2 H15 L16 5 L19 4 L22 9 L19.5 11 V13 L22 15 L19 20 L16 19 L15 22 H9 L8 19 L5 20 L2 15 L4.5 13 V11 L2 9 L5 4 L8 5 Z",
      selectedFill = "M9 2 H15 L16 5 L19 4 L22 9 L19.5 11 V13 L22 15 L19 20 L16 19 L15 22 H9 L8 19 L5 20 L2 15 L4.5 13 V11 L2 9 L5 4 L8 5 Z M15 12 A3 3 0 1 0 9 12 A3 3 0 1 0 15 12 Z",
    ),
  )
}

private fun brutalistNavigation(
  name: String,
  outline: String,
  selectedOutline: String,
  selectedFill: String,
): PrismNavigationIcons = PrismNavigationIcons(
  unselected = prismIconArtwork(name = "Brutalist${name}", outline = outline),
  selected = prismIconArtwork(name = "Brutalist${name}Selected", outline = selectedOutline, fill = selectedFill),
)
