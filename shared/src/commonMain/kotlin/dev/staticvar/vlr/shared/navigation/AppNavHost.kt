/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.staticvar.designsystem.component.card.cardMascotViewport
import dev.staticvar.designsystem.component.navigation.PrismBottomNavBar
import dev.staticvar.designsystem.component.navigation.PrismBottomNavBarLarge
import dev.staticvar.designsystem.component.navigation.PrismBottomNavItem
import dev.staticvar.designsystem.prism.Prism
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI
import vlr.shared.generated.resources.Res
import vlr.shared.generated.resources.navigation_events
import vlr.shared.generated.resources.navigation_home
import vlr.shared.generated.resources.navigation_matches
import vlr.shared.generated.resources.navigation_news
import vlr.shared.generated.resources.navigation_ranking

@OptIn(KoinExperimentalAPI::class, ExperimentalSharedTransitionApi::class)
@Composable
public fun AppNavHost(appState: VlrAppState, modifier: Modifier = Modifier) {
  val navigationTelemetry = remember(appState) { NavigationTelemetry() }
  val activeRoute = appState.backStack.lastOrNull() as? AppRoute
  SideEffect { navigationTelemetry.show(activeRoute) }
  val icons = Prism.icons
  val navItems = listOf(
    PrismBottomNavItem(id = HOME_ID, label = stringResource(Res.string.navigation_home), icon = icons.home.unselected, selectedIcon = icons.home.selected),
    PrismBottomNavItem(id = MATCHES_ID, label = stringResource(Res.string.navigation_matches), icon = icons.matches.unselected, selectedIcon = icons.matches.selected),
    PrismBottomNavItem(id = EVENTS_ID, label = stringResource(Res.string.navigation_events), icon = icons.events.unselected, selectedIcon = icons.events.selected),
    PrismBottomNavItem(id = RANKINGS_ID, label = stringResource(Res.string.navigation_ranking), icon = icons.rankings.unselected, selectedIcon = icons.rankings.selected),
    PrismBottomNavItem(id = NEWS_ID, label = stringResource(Res.string.navigation_news), icon = icons.news.unselected, selectedIcon = icons.news.selected),
  )
  val entryProvider = koinEntryProvider<NavKey>()
  val entryDecorators = listOf(
    rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
    rememberViewModelStoreNavEntryDecorator<NavKey>(),
  )

  ProvideVlrAppState(appState = appState) {
    androidx.compose.foundation.layout.BoxWithConstraints(
      modifier = modifier.fillMaxSize().cardMascotViewport(),
    ) {
      val showSceneLayout: Boolean = maxWidth >= sceneBreakpoint
      val showRail: Boolean = maxWidth >= railBreakpoint
      val sceneStrategy = rememberGroupedListDetailSceneStrategy<NavKey>(enabled = showSceneLayout)
      val eventTransitionDurationMillis = Prism.anim.standard.durationMillis
      val entries = rememberDecoratedNavEntries(appState.backStack, entryDecorators, entryProvider)
      val sceneState = rememberSceneState(entries, listOf(sceneStrategy), onBack = appState::navigateUp)
      val scene = sceneState.currentScene
      val navigationEventState = rememberNavigationEventState(
        currentInfo = SceneInfo(scene),
        backInfo = sceneState.previousScenes.map { SceneInfo(it) },
      )
      NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = scene.previousEntries.isNotEmpty(),
        onBackCompleted = {
          repeat((entries.size - scene.previousEntries.size).coerceAtLeast(0)) { appState.navigateUp() }
        },
      )
      val paneTransition = rememberGroupedPaneTransition(
        currentPane = (scene as? GroupedListDetailScene<*>)?.paneState,
        previousPane = (sceneState.previousScenes.lastOrNull() as? GroupedListDetailScene<*>)?.paneState,
        gestureTransition = navigationEventState.transitionState,
      )

      SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(
          LocalAppEventSharedTransitionScope provides if (showSceneLayout) null else this,
          LocalGroupedPaneTransition provides paneTransition,
        ) {
          Row(
            modifier = if (showRail) {
              Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(Prism.dimens.spacingM)
            } else {
              Modifier.fillMaxSize()
            },
            horizontalArrangement = Arrangement.spacedBy(if (showRail) Prism.dimens.spacingM else 0.dp),
          ) {
            if (showRail) {
              PrismBottomNavBarLarge(
                items = navItems,
                selectedItemId = appState.selectedNavigationItemId,
                onItemSelected = { appState.selectRoot(it.id) },
              )
            }
            Column(modifier = Modifier.weight(1f).fillMaxSize()) {
              NavDisplay(
                sceneState = sceneState,
                navigationEventState = navigationEventState,
                transitionSpec = {
                  navigationForwardTransition(
                    durationMillis = navigationDurationMillis(
                      eventTransitionDurationMillis = eventTransitionDurationMillis,
                      eventLogoTransitionEnabled = !showSceneLayout,
                    ),
                  )
                },
                popTransitionSpec = {
                  navigationBackTransition(
                    durationMillis = navigationDurationMillis(
                      eventTransitionDurationMillis = eventTransitionDurationMillis,
                      eventLogoTransitionEnabled = !showSceneLayout,
                    ),
                  )
                },
                predictivePopTransitionSpec = {
                  navigationBackTransition(
                    durationMillis = navigationDurationMillis(
                      eventTransitionDurationMillis = eventTransitionDurationMillis,
                      eventLogoTransitionEnabled = !showSceneLayout,
                    ),
                  )
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
              )
              AnimatedVisibility(
                visible = !showRail && (showSceneLayout || appState.shouldShowBottomNavigation),
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
                  onItemSelected = { appState.selectRoot(it.id) },
                  modifier = Modifier.fillMaxWidth(),
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun navigationForwardTransition(durationMillis: Int = NavigationTransitionDurationMillis) = (
  fadeIn(animationSpec = tween(durationMillis = durationMillis)) +
    slideInHorizontally(
      animationSpec = tween(durationMillis = durationMillis),
      initialOffsetX = { width -> width / NavigationSlideOffsetDivisor },
    )
  ) togetherWith
  (
    fadeOut(animationSpec = tween(durationMillis = durationMillis)) +
      slideOutHorizontally(
        animationSpec = tween(durationMillis = durationMillis),
        targetOffsetX = { width -> -width / NavigationSlideOffsetDivisor },
      )
    )

private fun navigationBackTransition(durationMillis: Int = NavigationTransitionDurationMillis) = (
  fadeIn(animationSpec = tween(durationMillis = durationMillis)) +
    slideInHorizontally(
      animationSpec = tween(durationMillis = durationMillis),
      initialOffsetX = { width -> -width / NavigationSlideOffsetDivisor },
    )
  ) togetherWith
  (
    fadeOut(animationSpec = tween(durationMillis = durationMillis)) +
      slideOutHorizontally(
        animationSpec = tween(durationMillis = durationMillis),
        targetOffsetX = { width -> width / NavigationSlideOffsetDivisor },
      )
    )

private fun AnimatedContentTransitionScope<Scene<NavKey>>.navigationDurationMillis(
  eventTransitionDurationMillis: Int,
  eventLogoTransitionEnabled: Boolean,
): Int {
  if (!eventLogoTransitionEnabled) return NavigationTransitionDurationMillis

  val sharesCard = listOf(EventTransitionRoleKey, MatchTransitionRoleKey).any { key ->
    val initialRole = initialState.entries.lastOrNull()?.metadata?.get(key)
    val targetRole = targetState.entries.lastOrNull()?.metadata?.get(key)
    (initialRole == EventTransitionRole.List && targetRole == EventTransitionRole.Detail) ||
      (initialRole == EventTransitionRole.Detail && targetRole == EventTransitionRole.List)
  }
  return if (sharesCard) eventTransitionDurationMillis else NavigationTransitionDurationMillis
}

internal enum class EventTransitionRole {
  List,
  Detail,
}

internal const val EventTransitionRoleKey: String = "VlrEventTransitionRole"
internal const val MatchTransitionRoleKey: String = "VlrMatchTransitionRole"

private const val NavigationTransitionDurationMillis: Int = 180
private const val NavigationSlideOffsetDivisor: Int = 8
