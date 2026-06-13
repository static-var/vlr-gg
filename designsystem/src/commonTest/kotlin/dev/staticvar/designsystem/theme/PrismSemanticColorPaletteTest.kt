/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme

import dev.staticvar.designsystem.theme.dark.DarkPalette
import dev.staticvar.designsystem.theme.light.LightPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class PrismSemanticColorPaletteTest {
  @Test
  fun lightSemanticContainersDoNotCollapseToSurfaceColor() {
    val palette = LightPalette.create()

    assertNotEquals(palette.surface, palette.successContainer)
    assertNotEquals(palette.surface, palette.warningContainer)
    assertNotEquals(palette.surface, palette.dangerContainer)
    assertNotEquals(palette.surface, palette.infoContainer)
  }

  @Test
  fun darkSemanticContainersDoNotCollapseToSurfaceColor() {
    val palette = DarkPalette.create()

    assertNotEquals(palette.surface, palette.successContainer)
    assertNotEquals(palette.surface, palette.warningContainer)
    assertNotEquals(palette.surface, palette.dangerContainer)
    assertNotEquals(palette.surface, palette.infoContainer)
  }

  @Test
  fun lightSemanticContainersRemainDistinctFromEachOther() {
    val palette = LightPalette.create()
    val containers =
      setOf(
        palette.successContainer,
        palette.warningContainer,
        palette.dangerContainer,
        palette.infoContainer,
      )

    assertEquals(4, containers.size)
  }

  @Test
  fun darkSemanticContainersRemainDistinctFromEachOther() {
    val palette = DarkPalette.create()
    val containers =
      setOf(
        palette.successContainer,
        palette.warningContainer,
        palette.dangerContainer,
        palette.infoContainer,
      )

    assertEquals(4, containers.size)
  }
}
