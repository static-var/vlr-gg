/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismCatppuccinFlavour
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import dev.staticvar.designsystem.prism.PrismVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class PrismThemeLifetimeTest {
  @Test
  fun changingAppearanceUpdatesColorsWithoutReplacingScreenStateOrScope() = runComposeUiTest {
    var appearance by mutableStateOf(Appearance())
    lateinit var screenState: MutableState<Int>
    lateinit var screenScope: CoroutineScope
    var background = Color.Unspecified
    var effectStarts = 0
    var effectDisposals = 0

    setContent {
      PrismTheme(appearance.variant, appearance.family, appearance.flavour) {
        val state = remember { mutableStateOf(0) }
        val scope = rememberCoroutineScope()
        val color = Prism.color.background
        DisposableEffect(Unit) {
          effectStarts++
          onDispose { effectDisposals++ }
        }
        SideEffect {
          screenState = state
          screenScope = scope
          background = color
        }
      }
    }
    waitForIdle()
    val originalState = screenState
    val originalScope = screenScope
    runOnIdle { screenState.value = 42 }

    val appearances = listOf(Appearance(variant = PrismVariant.Dark)) +
      PrismCatppuccinFlavour.entries.map { flavour ->
        Appearance(family = PrismThemeFamily.Catppuccin, flavour = flavour)
      } + Appearance()
    appearances.forEach { next ->
      val previousBackground = background
      runOnIdle { appearance = next }
      waitForIdle()
      runOnIdle {
        assertSame(originalState, screenState, "$next replaced remembered screen state")
        assertEquals(42, screenState.value)
        assertSame(originalScope, screenScope, "$next replaced the screen coroutine scope")
        assertTrue(originalScope.isActive)
        assertEquals(1, effectStarts)
        assertEquals(0, effectDisposals)
        assertNotEquals(previousBackground, background, "$next did not update theme colors")
      }
    }
  }

  private data class Appearance(
    val variant: PrismVariant = PrismVariant.Light,
    val family: PrismThemeFamily = PrismThemeFamily.Brutalist,
    val flavour: PrismCatppuccinFlavour = PrismCatppuccinFlavour.Latte,
  )
}
