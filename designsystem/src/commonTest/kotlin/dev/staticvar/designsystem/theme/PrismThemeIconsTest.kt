/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme

import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.PrismCatppuccinFlavour
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.designsystem.theme.catppuccin.CatppuccinThemeDefinition
import dev.staticvar.designsystem.theme.console.ConsoleThemeDefinition
import dev.staticvar.designsystem.theme.dark.DarkThemeDefinition
import dev.staticvar.designsystem.theme.light.LightThemeDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class PrismThemeIconsTest {
  @Test
  fun appearancesShareArtworkWithinAFamilyButFamiliesStayDistinct() {
    val brutalist = LightThemeDefinition.icons
    val console = ConsoleThemeDefinition(PrismVariant.Light).icons
    val catppuccin = CatppuccinThemeDefinition(PrismCatppuccinFlavour.Latte).icons
    assertSame(brutalist, DarkThemeDefinition.icons)
    assertSame(console, ConsoleThemeDefinition(PrismVariant.Dark).icons)
    PrismCatppuccinFlavour.entries.forEach { flavour ->
      assertSame(catppuccin, CatppuccinThemeDefinition(flavour).icons)
    }
    assertNotSame(brutalist.refresh, console.refresh)
    assertNotSame(brutalist.refresh, catppuccin.refresh)
    assertNotSame(console.refresh, catppuccin.refresh)
  }

  @Test
  fun everyFamilyProvidesConsistentIconSizesAndDirectionalBackArrows() {
    val families = listOf(
      LightThemeDefinition.icons,
      ConsoleThemeDefinition(PrismVariant.Light).icons,
      CatppuccinThemeDefinition(PrismCatppuccinFlavour.Latte).icons,
    )
    families.forEach { icons ->
      assertTrue(icons.back.autoMirror)
      val navigation = listOf(icons.home, icons.news, icons.matches, icons.events, icons.rankings, icons.settings)
      val vectors = listOf(icons.refresh, icons.share, icons.preview, icons.back) +
        navigation.flatMap { listOf(it.unselected, it.selected) }
      vectors.forEach { vector ->
        assertEquals(24.dp, vector.defaultWidth, vector.name)
        assertEquals(24.dp, vector.defaultHeight, vector.name)
        assertTrue(vector.root.size > 0, vector.name)
      }
    }
  }
}
