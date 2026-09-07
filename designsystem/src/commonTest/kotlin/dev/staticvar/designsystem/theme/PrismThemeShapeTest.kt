/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme

import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.staticvar.designsystem.prism.PrismCatppuccinFlavour
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.designsystem.theme.catppuccin.CatppuccinThemeDefinition
import dev.staticvar.designsystem.theme.console.ConsoleThemeDefinition
import dev.staticvar.designsystem.theme.dark.DarkThemeDefinition
import dev.staticvar.designsystem.theme.light.LightThemeDefinition
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class PrismThemeShapeTest {
  @Test
  fun everyCatppuccinFlavourRoundsAllContainerCorners() {
    PrismCatppuccinFlavour.entries.forEach { flavour ->
      outlines(CatppuccinThemeDefinition(flavour).shapes).forEach { outline ->
        val rounded = assertIs<Outline.Rounded>(outline, "$flavour containers must be rounded").roundRect
        val corners = listOf(
          rounded.topLeftCornerRadius,
          rounded.topRightCornerRadius,
          rounded.bottomRightCornerRadius,
          rounded.bottomLeftCornerRadius,
        )
        assertTrue(corners.all { it.x > 0f && it.y > 0f }, "$flavour must round every container corner")
      }
    }
  }

  @Test
  fun bothBrutalistAppearancesKeepSquareContainers() {
    listOf(LightThemeDefinition.shapes, DarkThemeDefinition.shapes).forEach { shapes ->
      outlines(shapes).forEach { outline ->
        assertIs<Outline.Rectangle>(outline, "Brutalist containers must keep square corners")
      }
    }
  }

  @Test
  fun consoleKeepsSquareCornersInBothAppearances() {
    PrismVariant.entries.forEach { variant ->
      outlines(ConsoleThemeDefinition(variant).shapes).forEach { outline ->
        assertIs<Outline.Rectangle>(outline)
      }
    }
  }

  private fun outlines(shapes: Shapes): List<Outline> =
    listOf(shapes.extraSmall, shapes.small, shapes.medium, shapes.large, shapes.extraLarge).map { shape ->
      shape.createOutline(
        size = Size(100f, 100f),
        layoutDirection = LayoutDirection.Ltr,
        density = Density(1f),
      )
    }
}
