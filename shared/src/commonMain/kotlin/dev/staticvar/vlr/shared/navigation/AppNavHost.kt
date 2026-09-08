/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.staticvar.designsystem.component.navigation.PrismBottomNavBar
import dev.staticvar.designsystem.component.navigation.PrismBottomNavBarLarge
import dev.staticvar.designsystem.prism.Prism
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
public fun AppNavHost(appState: VlrAppState, modifier: Modifier = Modifier) {
  val navigationTelemetry = remember(appState) { NavigationTelemetry() }
  val activeRoute = appState.backStack.lastOrNull() as? AppRoute
  SideEffect { navigationTelemetry.show(activeRoute) }
  val navItems = remember(appState.navigationItems) { appState.navigationItems }
  val entryProvider = koinEntryProvider<NavKey>()
  val entryDecorators = listOf(
    rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
    rememberViewModelStoreNavEntryDecorator<NavKey>(),
  )

  ProvideVlrAppState(appState = appState) {
    androidx.compose.foundation.layout.BoxWithConstraints(
      modifier = modifier.fillMaxSize(),
    ) {
      val showSceneLayout: Boolean = maxWidth >= sceneBreakpoint
      val showRail: Boolean = maxWidth >= railBreakpoint
      val sceneStrategy = rememberGroupedListDetailSceneStrategy<NavKey>(enabled = showSceneLayout)

      if (showRail) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(Prism.dimens.spacingM),
          horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
        ) {
          PrismBottomNavBarLarge(
            items = navItems,
            selectedItemId = appState.selectedNavigationItemId,
            onItemSelected = appState::selectRoot,
          )
          NavDisplay(
            backStack = appState.backStack,
            onBack = appState::navigateUp,
            sceneStrategy = sceneStrategy,
            entryProvider = entryProvider,
            entryDecorators = entryDecorators,
            transitionSpec = { navigationForwardTransition() },
            popTransitionSpec = { navigationBackTransition() },
            predictivePopTransitionSpec = { navigationBackTransition() },
            modifier = Modifier.weight(1f).fillMaxSize(),
          )
        }
      } else {
        Column(
          modifier = Modifier.fillMaxSize(),
        ) {
          NavDisplay(
            backStack = appState.backStack,
            onBack = appState::navigateUp,
            sceneStrategy = sceneStrategy,
            entryProvider = entryProvider,
            entryDecorators = entryDecorators,
            transitionSpec = { navigationForwardTransition() },
            popTransitionSpec = { navigationBackTransition() },
            predictivePopTransitionSpec = { navigationBackTransition() },
            modifier = Modifier.weight(1f).fillMaxWidth(),
          )
          AnimatedVisibility(
            visible = appState.shouldShowBottomNavigation,
            enter = slideInVertically(
              animationSpec = tween(durationMillis = NavigationTransitionDurationMillis),
              initialOffsetY = { height -> height },
            ) + fadeIn(animationSpec = tween(durationMillis = NavigationTransitionDurationMillis)),
            exit = slideOutVertically(
              animationSpec = tween(durationMillis = NavigationTransitionDurationMillis),
              targetOffsetY = { height -> height },
            ) + fadeOut(animationSpec = tween(durationMillis = NavigationTransitionDurationMillis)),
          ) {
            PrismBottomNavBar(
              items = navItems,
              selectedItemId = appState.selectedNavigationItemId,
              onItemSelected = appState::selectRoot,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      }
    }
  }
}

private fun navigationForwardTransition() = (
  fadeIn(animationSpec = tween(durationMillis = NavigationTransitionDurationMillis)) +
    slideInHorizontally(
      animationSpec = tween(durationMillis = NavigationTransitionDurationMillis),
      initialOffsetX = { width -> width / NavigationSlideOffsetDivisor },
    )
  ) togetherWith
  (
    fadeOut(animationSpec = tween(durationMillis = NavigationTransitionDurationMillis)) +
      slideOutHorizontally(
        animationSpec = tween(durationMillis = NavigationTransitionDurationMillis),
        targetOffsetX = { width -> -width / NavigationSlideOffsetDivisor },
      )
    )

private fun navigationBackTransition() = (
  fadeIn(animationSpec = tween(durationMillis = NavigationTransitionDurationMillis)) +
    slideInHorizontally(
      animationSpec = tween(durationMillis = NavigationTransitionDurationMillis),
      initialOffsetX = { width -> -width / NavigationSlideOffsetDivisor },
    )
  ) togetherWith
  (
    fadeOut(animationSpec = tween(durationMillis = NavigationTransitionDurationMillis)) +
      slideOutHorizontally(
        animationSpec = tween(durationMillis = NavigationTransitionDurationMillis),
        targetOffsetX = { width -> width / NavigationSlideOffsetDivisor },
      )
    )

private const val NavigationTransitionDurationMillis: Int = 180
private const val NavigationSlideOffsetDivisor: Int = 8
