/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.icon

internal val CatppuccinIcons: PrismIcons by lazy {
  PrismIcons(
    refresh = prismIconArtwork(
      name = "CatppuccinRefresh",
      outline = "M4.5 9 A8 8 0 0 1 19.5 8 M15 8 H20 V3.5 M19.5 15 A8 8 0 0 1 4.5 16 M9 16 H4 V20.5",
      rounded = true,
    ),
    share = prismIconArtwork(
      name = "CatppuccinShare",
      outline = "M12 15 V3 M7.5 7.5 L12 3 L16.5 7.5 M5 12 V18 Q5 21 8 21 H16 Q19 21 19 18 V12",
      rounded = true,
    ),
    preview = prismIconArtwork(
      name = "CatppuccinPreview",
      outline = "M2 12 Q6 5 12 5 Q18 5 22 12 Q18 19 12 19 Q6 19 2 12 Z M15 12 A3 3 0 1 1 9 12 A3 3 0 1 1 15 12 Z",
      rounded = true,
    ),
    back = prismIconArtwork(
      name = "CatppuccinBack",
      outline = "M10.5 5 L3.5 12 L10.5 19 M4 12 H20.5",
      rounded = true,
      autoMirror = true,
    ),
    home = catppuccinNavigation(
      name = "Home",
      outline = "M3 10 L10.5 3 Q12 1.5 13.5 3 L21 10 V19 Q21 21 19 21 H15 V14 H9 V21 H5 Q3 21 3 19 Z",
      selectedOutline = "",
      selectedFill = "M3 10 L10.5 3 Q12 1.5 13.5 3 L21 10 V19 Q21 21 19 21 H15 V14 H9 V21 H5 Q3 21 3 19 Z",
    ),
    news = catppuccinNavigation(
      name = "News",
      outline = "M7 3 H17 Q20 3 20 6 V18 Q20 21 17 21 H7 Q4 21 4 18 V6 Q4 3 7 3 Z M8 7 H11 V11 H8 Z M15 7 H16 M15 11 H16 M8 15 H16 M8 18 H14",
      selectedOutline = "M7 3 H17 Q20 3 20 6 V18 Q20 21 17 21 H7 Q4 21 4 18 V6 Q4 3 7 3 Z M8 15 H16 M8 18 H14",
      selectedFill = "M8 6 H16 Q17 6 17 7 V11 Q17 12 16 12 H8 Q7 12 7 11 V7 Q7 6 8 6 Z",
    ),
    matches = catppuccinNavigation(
      name = "Matches",
      outline = "M8 6 H16 Q19 6 20 10 L21 16 Q21.5 20 18 19 L14.5 16 H9.5 L6 19 Q2.5 20 3 16 L4 10 Q5 6 8 6 Z M6.5 11 H10.5 M8.5 9 V13 M15.5 10 H15.6 M18 12.5 H18.1",
      selectedOutline = "M8 6 H16 Q19 6 20 10 L21 16 Q21.5 20 18 19 L14.5 16 H9.5 L6 19 Q2.5 20 3 16 L4 10 Q5 6 8 6 Z M6.5 11 H10.5 M8.5 9 V13",
      selectedFill = "M17 10 A1.5 1.5 0 1 1 14 10 A1.5 1.5 0 1 1 17 10 Z M19.5 12.5 A1.5 1.5 0 1 1 16.5 12.5 A1.5 1.5 0 1 1 19.5 12.5 Z M8 5 H16 Q18 5 19 8 H5 Q6 5 8 5 Z",
    ),
    events = catppuccinNavigation(
      name = "Events",
      outline = "M7 3 H17 V9 Q17 15 12 15 Q7 15 7 9 Z M7 5 H3 V8 Q3 12 8 12 M17 5 H21 V8 Q21 12 16 12 M12 15 V21 M8 21 H16",
      selectedOutline = "M7 5 H3 V8 Q3 12 8 12 M17 5 H21 V8 Q21 12 16 12 M12 15 V21 M8 21 H16",
      selectedFill = "M8 3 H16 Q17 3 17 4 V9 Q17 15 12 15 Q7 15 7 9 V4 Q7 3 8 3 Z",
    ),
    rankings = catppuccinNavigation(
      name = "Rankings",
      outline = "M3 21 V14 Q3 12 5 12 H6 Q8 12 8 14 V21 M9.5 21 V6 Q9.5 4 11.5 4 H12.5 Q14.5 4 14.5 6 V21 M16 21 V11 Q16 9 18 9 H19 Q21 9 21 11 V21 M2 21 H22",
      selectedOutline = "M2 21 H22",
      selectedFill = "M3 21 V14 Q3 12 5 12 H6 Q8 12 8 14 V21 Z M9.5 21 V6 Q9.5 4 11.5 4 H12.5 Q14.5 4 14.5 6 V21 Z M16 21 V11 Q16 9 18 9 H19 Q21 9 21 11 V21 Z",
    ),
    settings = catppuccinNavigation(
      name = "Settings",
      outline = "M9 2 H15 L16 5 L19 4 L22 9 L19.5 11 V13 L22 15 L19 20 L16 19 L15 22 H9 L8 19 L5 20 L2 15 L4.5 13 V11 L2 9 L5 4 L8 5 Z M15 12 A3 3 0 1 0 9 12 A3 3 0 1 0 15 12 Z",
      selectedOutline = "M9 2 H15 L16 5 L19 4 L22 9 L19.5 11 V13 L22 15 L19 20 L16 19 L15 22 H9 L8 19 L5 20 L2 15 L4.5 13 V11 L2 9 L5 4 L8 5 Z",
      selectedFill = "M9 2 H15 L16 5 L19 4 L22 9 L19.5 11 V13 L22 15 L19 20 L16 19 L15 22 H9 L8 19 L5 20 L2 15 L4.5 13 V11 L2 9 L5 4 L8 5 Z M15 12 A3 3 0 1 0 9 12 A3 3 0 1 0 15 12 Z",
    ),
  )
}

private fun catppuccinNavigation(
  name: String,
  outline: String,
  selectedOutline: String,
  selectedFill: String,
): PrismNavigationIcons = PrismNavigationIcons(
  unselected = prismIconArtwork(name = "Catppuccin${name}", outline = outline, rounded = true),
  selected = prismIconArtwork(name = "Catppuccin${name}Selected", outline = selectedOutline, fill = selectedFill, rounded = true),
)
