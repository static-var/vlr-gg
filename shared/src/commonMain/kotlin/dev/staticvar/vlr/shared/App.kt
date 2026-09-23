/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismCatppuccinFlavour
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.network.NetworkStatus
import dev.staticvar.vlr.core.notifications.PushTokenProvider
import dev.staticvar.vlr.core.settings.CatppuccinFlavour
import dev.staticvar.vlr.core.settings.CacheCleanupPreferencesRepository
import dev.staticvar.vlr.core.settings.MascotPreference
import dev.staticvar.vlr.core.settings.LiveMatchNotificationPreferencesRepository
import dev.staticvar.vlr.core.settings.LiveMatchNotificationSettingsController
import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import dev.staticvar.vlr.core.settings.ThemeFamily
import dev.staticvar.vlr.domain.repository.CacheCleanupRepository
import dev.staticvar.vlr.domain.usecase.InitialFavoriteProfilesRefresh
import dev.staticvar.vlr.shared.appearance.AppearanceViewModel
import dev.staticvar.vlr.shared.appearance.ApplyPlatformAppearance
import dev.staticvar.vlr.shared.navigation.AppDeepLinkHandler
import dev.staticvar.vlr.shared.navigation.AppNavHost
import dev.staticvar.vlr.shared.navigation.rememberVlrAppState
import dev.staticvar.vlr.shared.notifications.FavoriteLiveUpdateCoordinator
import dev.staticvar.vlr.shared.notifications.LocalLiveMatchNotificationSettingsController
import dev.staticvar.vlr.shared.notifications.PushTokenRegistrationCoordinator
import dev.staticvar.vlr.shared.notifications.PushTokenRegistrationUploader
import dev.staticvar.vlr.shared.search.PublishSearchFavorites
import dev.staticvar.vlr.shared.widget.PublishUpcomingMatchesWidget
import dev.staticvar.vlr.sharedui.component.common.LocalIsOnline
import dev.staticvar.vlr.sharedui.image.ProvideSharedImageLoader
import dev.staticvar.vlr.sharedui.mascot.LocalMascotCharacter
import dev.staticvar.vlr.sharedui.mascot.MascotCharacter
import dev.staticvar.vlr.sharedui.mascot.ProvideCardMascots
import dev.staticvar.vlr.sharedui.notifications.LocalNotificationPermissionProvider
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerMode
import org.koin.compose.koinInject
import dev.staticvar.vlr.shared.di.vlrViewModel
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/**
 * Main entry point for the shared Compose UI.
 * This will be used by both Android and iOS platforms.
 */
@Composable
public fun App(
  deepLinkHandler: AppDeepLinkHandler? = null,
  onWidgetSnapshotChanged: suspend (String) -> Unit = {},
  onSearchFavoritesChanged: suspend (String) -> Unit = {},
  pushTokenProvider: PushTokenProvider? = null,
) {
  ProvideSharedImageLoader()
  PublishSearchFavorites(onSearchFavoritesChanged)

  val networkMonitor = koinInject<NetworkMonitor>()
  val spoilerPreferences = koinInject<SpoilerPreferencesRepository>()
  val spoilersHidden by spoilerPreferences.enabled.collectAsStateWithLifecycle()
  val networkStatus by networkMonitor.status.collectAsStateWithLifecycle()
  val initialFavoriteProfilesRefresh = koinInject<InitialFavoriteProfilesRefresh>()
  LaunchedEffect(initialFavoriteProfilesRefresh) {
    networkMonitor.status.first { it == NetworkStatus.Online }
    initialFavoriteProfilesRefresh.awaitInitialRefresh()
  }
  val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
  val notificationPermissionProvider = LocalNotificationPermissionProvider.current
  val notificationPreferences = koinInject<LiveMatchNotificationPreferencesRepository>()
  val favoriteLiveUpdateCoordinator = koinInject<FavoriteLiveUpdateCoordinator>()
  val pushTokenRegistrationUploader = koinInject<PushTokenRegistrationUploader>()
  val mainScope = rememberCoroutineScope()
  val pushTokenRegistrationCoordinator = if (pushTokenProvider != null && notificationPermissionProvider != null) {
    remember(
      pushTokenProvider,
      notificationPermissionProvider,
      notificationPreferences,
      favoriteLiveUpdateCoordinator,
      pushTokenRegistrationUploader,
      mainScope,
    ) {
      PushTokenRegistrationCoordinator(
        pushTokenProvider = pushTokenProvider,
        permissionProvider = notificationPermissionProvider,
        notificationPreferences = notificationPreferences,
        uploader = pushTokenRegistrationUploader,
        mainScope = mainScope,
        onEligibilityChanged = favoriteLiveUpdateCoordinator::onEligibilityChanged,
        onSyncRequested = favoriteLiveUpdateCoordinator::retry,
      )
    }
  } else {
    null
  }
  val notificationSettingsController = notificationPermissionProvider?.let { provider ->
    remember(notificationPreferences, provider, pushTokenRegistrationCoordinator) {
      LiveMatchNotificationSettingsController(
        repository = notificationPreferences,
        provider = provider,
        onAuthorizationChanged = { authorization ->
          pushTokenRegistrationCoordinator?.onAuthorizationChanged(authorization)
        },
      )
    }
  }
  DisposableEffect(pushTokenRegistrationCoordinator) {
    pushTokenRegistrationCoordinator?.start()
    onDispose { pushTokenRegistrationCoordinator?.stop() }
  }
  LaunchedEffect(lifecycleState, pushTokenRegistrationCoordinator) {
    if (lifecycleState == Lifecycle.State.RESUMED) {
      pushTokenRegistrationCoordinator?.onForeground()
    }
  }
  val cleanupPreferences = koinInject<CacheCleanupPreferencesRepository>()
  val cleanupRepository = koinInject<CacheCleanupRepository>()
  val autoCleanupEnabled by cleanupPreferences.enabled.collectAsStateWithLifecycle()
  LaunchedEffect(lifecycleState, autoCleanupEnabled, networkStatus) {
    if (lifecycleState == Lifecycle.State.RESUMED && autoCleanupEnabled && networkStatus == NetworkStatus.Online) {
      if (initialFavoriteProfilesRefresh.awaitInitialRefresh().isSuccess) {
        cleanupRepository.cleanupIfDue(Clock.System.now().toEpochMilliseconds())
      }
    }
  }
  val viewModel = vlrViewModel<AppearanceViewModel>()
  val appearance by viewModel.appearance.collectAsStateWithLifecycle()
  val isDark = appearance.isDark(isSystemInDarkTheme())
  val variant = if (isDark) PrismVariant.Dark else PrismVariant.Light
  val family = when (appearance.family) {
    ThemeFamily.Brutalist -> PrismThemeFamily.Brutalist
    ThemeFamily.Catppuccin -> PrismThemeFamily.Catppuccin
    ThemeFamily.Console -> PrismThemeFamily.Console
  }
  val flavour = when (appearance.catppuccinFlavour) {
    CatppuccinFlavour.Latte -> PrismCatppuccinFlavour.Latte
    CatppuccinFlavour.Frappe -> PrismCatppuccinFlavour.Frappe
    CatppuccinFlavour.Macchiato -> PrismCatppuccinFlavour.Macchiato
    CatppuccinFlavour.Mocha -> PrismCatppuccinFlavour.Mocha
  }
  val mascotCharacter = when (appearance.mascot) {
    MascotPreference.Lynx -> MascotCharacter.Lynx
    MascotPreference.Rosie -> MascotCharacter.Rosie
    MascotPreference.Off -> null
  }
  PrismTheme(variant = variant, family = family, catppuccinFlavour = flavour) {
    PublishUpcomingMatchesWidget(
      monospace = appearance.family == ThemeFamily.Console,
      onSnapshotChanged = onWidgetSnapshotChanged,
    )
    ApplyPlatformAppearance(
      isDark = isDark,
      followSystem = appearance.family != ThemeFamily.Catppuccin && appearance.mode == null,
    )
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = Prism.color.background,
      contentColor = Prism.color.contentPrimary,
    ) {
      CompositionLocalProvider(
        LocalMascotCharacter provides mascotCharacter,
        LocalIsOnline provides (networkStatus != NetworkStatus.Offline),
        LocalLiveMatchNotificationSettingsController provides notificationSettingsController,
        LocalSpoilerMode provides SpoilerMode(enabled = spoilersHidden, onToggle = spoilerPreferences::toggle),
      ) {
        val appState = rememberVlrAppState()
        val activeDeepLinkHandler = remember(deepLinkHandler) { deepLinkHandler ?: AppDeepLinkHandler() }
        val deepLinkState by activeDeepLinkHandler.state.collectAsStateWithLifecycle()
        val deepLinkRequest = deepLinkState.pendingRequest
        LaunchedEffect(deepLinkRequest?.id) {
          deepLinkRequest?.let { request ->
            request.navigate(appState)
            activeDeepLinkHandler.consume(request.id)
          }
        }
        ProvideCardMascots(
          screenKey = appState.backStack.lastOrNull().toString(),
          isActive = lifecycleState == Lifecycle.State.RESUMED,
          probabilityPercent = appearance.mascotVisitFrequency.probabilityPercent,
        ) {
          AppNavHost(appState = appState)
        }
      }
    }
  }
}
