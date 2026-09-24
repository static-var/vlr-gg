/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.notifications

import dev.staticvar.vlr.core.identity.UserIdentityRepository
import dev.staticvar.vlr.core.notifications.DismissedLiveUpdateProvider
import dev.staticvar.vlr.core.notifications.LiveUpdateStateProvider
import dev.staticvar.vlr.core.notifications.PushPlatform
import dev.staticvar.vlr.core.settings.PushTokenRegistrationPreferencesRepository
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.repository.FavoriteScheduleRepository
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.remotesource.liveupdates.LiveActivityStartDataSource
import dev.staticvar.vlr.remotesource.liveupdates.LiveActivityStartResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Starts Live Activities for live favorite matches after registration and favorite sync.
 * Rechecks device and client state before each request, and records attempts to prevent duplicates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class LiveActivityStartCoordinator(
  private val identity: UserIdentityRepository,
  private val favorites: FavoritesRepository,
  private val schedule: FavoriteScheduleRepository,
  private val tokenPreferences: PushTokenRegistrationPreferencesRepository,
  private val favoriteSync: FavoriteLiveUpdateCoordinator,
  private val dataSource: LiveActivityStartDataSource,
  private val ledger: LiveActivityStartLedger,
  private val mainDispatcher: CoroutineDispatcher,
  private val appScope: CoroutineScope,
) {
  private val eligibility = MutableStateFlow(LiveUpdateEligibility.Pending)
  private val stateProvider = MutableStateFlow<LiveUpdateStateProvider?>(null)
  private val rejected = MutableStateFlow<Set<Pair<String, String>>>(emptySet())
  private val retryRevision = MutableStateFlow(0L)

  private val requestedRestores = MutableStateFlow<Set<Pair<String, String>>>(emptySet())
  private val restorable = MutableStateFlow<Set<String>>(emptySet())
  val restorableMatchIds = restorable.asStateFlow()

  /** Queues an explicit restart only for a currently eligible dismissed favorite. */
  fun restoreNotification(matchId: String) {
    if (matchId !in restorable.value) return
    requestedRestores.update { it + (identity.id.value.toString() to matchId) }
    retryRevision.value++
  }

  init {
    appScope.launch {
      val access = combine(
        identity.id,
        favorites.observeDirectFavorites(),
        tokenPreferences.preferences,
        favoriteSync.syncedFavorites,
        eligibility,
      ) { id, selected, registration, synced, access ->
        Readiness(id.toString(), selected, registration, synced, access)
      }
      val snapshot = combine(access, schedule.observeMatches(), stateProvider) { ready, matches, provider ->
        Triple(ready, matches, provider)
      }
      launch {
        val dismissed = stateProvider.flatMapLatest { provider ->
          (provider as? DismissedLiveUpdateProvider)?.dismissedMatchIds ?: flowOf(emptySet())
        }
        combine(snapshot, dismissed, requestedRestores) { (ready, matches, provider), ids, requested ->
          if (provider?.platform != PushPlatform.Android || !ready.canStart(provider.platform)) emptySet()
          else withContext(mainDispatcher) {
            if (!provider.canRequestStart()) emptySet()
            else matches.filter { it.status == MatchStatus.LIVE && it.id in ids && (ready.clientId to it.id) !in requested }
              .mapTo(mutableSetOf()) { it.id }
          }
        }.collect { restorable.value = it }
      }
      combine(snapshot, retryRevision) { current, _ -> current }.collect { (ready, matches, provider) ->
        if (provider == null || !ready.canStart(provider.platform) || matches.none { it.status == MatchStatus.LIVE }) {
          requestedRestores.value = emptySet()
          return@collect
        }
        requestedRestores.update { requested ->
          requested.filterTo(mutableSetOf()) { (clientId, matchId) ->
            clientId == ready.clientId && matches.any { it.id == matchId && it.status == MatchStatus.LIVE }
          }
        }
        val freshMatches = schedule.observeMatches().first()
        for (match in freshMatches) {
          if (match.status != MatchStatus.LIVE) continue
          if (!ready.canStart(provider.platform) || identity.id.value.toString() != ready.clientId ||
            eligibility.value != LiveUpdateEligibility.Enabled ||
            tokenPreferences.preferences.value != ready.registration ||
            favoriteSync.syncedFavorites.value != ready.synced
          ) return@collect
          val key = ready.clientId to match.id
          val explicitRestore = key in requestedRestores.value && provider is DismissedLiveUpdateProvider
          if (!explicitRestore && (ledger.contains(ready.clientId, match.id) || key in rejected.value)) continue
          val canStartOnDevice = withContext(mainDispatcher) {
            provider.canRequestStart() && (explicitRestore || match.id !in provider.observedMatchIds())
          }
          if (!canStartOnDevice) continue
          if (FavoriteLiveUpdateCoordinator.FavoriteIds.from(favorites.observeDirectFavorites().first()) !=
            FavoriteLiveUpdateCoordinator.FavoriteIds.from(ready.selected)
          ) return@collect
          if (schedule.observeMatches().first().none { it.id == match.id && it.status == MatchStatus.LIVE }) continue
          if (!ready.canStart(provider.platform) || identity.id.value.toString() != ready.clientId ||
            eligibility.value != LiveUpdateEligibility.Enabled ||
            tokenPreferences.preferences.value != ready.registration ||
            favoriteSync.syncedFavorites.value != ready.synced ||
            stateProvider.value !== provider
          ) return@collect
          if (explicitRestore) {
            requestedRestores.update { it - key }
            val restored = withContext(mainDispatcher) {
              (provider as DismissedLiveUpdateProvider).restoreDismissedMatch(match.id)
            }
            if (!restored) continue
          }
          ledger.markAttempt(ready.clientId, match.id)
          val result = try {
            dataSource.start(ready.clientId, match.id)
          } catch (cancellation: CancellationException) {
            throw cancellation
          } catch (_: Exception) {
            LiveActivityStartResult.Unknown
          }
          if (result == LiveActivityStartResult.Rejected) {
            if (explicitRestore) withContext(mainDispatcher) {
              (provider as DismissedLiveUpdateProvider).keepMatchDismissed(match.id)
            }
            ledger.clearAttempt(ready.clientId, match.id)
            rejected.update { it + key }
          }
        }
      }
    }
  }

  fun onEligibilityChanged(value: LiveUpdateEligibility) {
    eligibility.value = value
    if (value != LiveUpdateEligibility.Enabled) {
      rejected.value = emptySet()
      requestedRestores.value = emptySet()
    }
  }

  fun attach(provider: LiveUpdateStateProvider?) {
    stateProvider.value = provider
  }

  /**
   * Allows server-rejected matches to be considered again.
   * Triggers a fresh check even if the schedule and registration have not changed.
   */
  fun retryRejected() {
    rejected.value = emptySet()
    retryRevision.value++
  }

  /** Checks whether registration and synced favorites allow a Live Activity start. */
  private data class Readiness(
    val clientId: String,
    val selected: DirectFavoriteSnapshot,
    val registration: dev.staticvar.vlr.core.settings.PushTokenRegistrationPreferences,
    val synced: FavoriteLiveUpdateCoordinator.SyncedFavorites?,
    val eligibility: LiveUpdateEligibility,
  ) {
    /**
     * Requires an uploaded token for this client and platform.
     * Also requires the server to have confirmed the current favorite selection.
     */
    fun canStart(platform: PushPlatform): Boolean = eligibility == LiveUpdateEligibility.Enabled &&
      registration.tokenPlatform == platform &&
      registration.token?.let { registration.wasUploaded(clientId, platform, it) } == true &&
      synced == FavoriteLiveUpdateCoordinator.SyncedFavorites(
        clientId,
        FavoriteLiveUpdateCoordinator.FavoriteIds.from(selected),
      )
  }
}
