/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.notifications

import dev.staticvar.vlr.core.identity.UserIdentityRepository
import dev.staticvar.vlr.core.settings.PushTokenRegistrationPreferencesRepository
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.remotesource.liveupdates.FavoriteLiveUpdateDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal enum class LiveUpdateEligibility {
  Pending,
  Enabled,
  Disabled,
}

internal class FavoriteLiveUpdateCoordinator(
  identityRepository: UserIdentityRepository,
  favoritesRepository: FavoritesRepository,
  private val tokenPreferences: PushTokenRegistrationPreferencesRepository,
  private val dataSource: FavoriteLiveUpdateDataSource,
  private val appScope: CoroutineScope,
) {
  private val actions = Channel<Action>(Channel.UNLIMITED)
  private var eligibility = LiveUpdateEligibility.Pending
  private var snapshot: ClientFavorites? = null
  private var revision: Long = 0
  private var inFlight: Upload? = null
  private val attemptedRequests = mutableSetOf<Request>()
  private var lastSuccessfulRequest: Request? = null
  private val serverClients = linkedSetOf<String>().apply {
    addAll(tokenPreferences.possiblySyncedFavoriteClients())
    tokenPreferences.preferences.value.uploadedClientId?.let(::add)
  }
  private val synced = MutableStateFlow<SyncedFavorites?>(null)
  val syncedFavorites: StateFlow<SyncedFavorites?> = synced

  init {
    appScope.launch {
      combine(identityRepository.id, favoritesRepository.observeDirectFavorites()) { clientId, favorites ->
        ClientFavorites(clientId.toString(), FavoriteIds.from(favorites))
      }
        .distinctUntilChanged()
        .collect { actions.send(Action.SnapshotChanged(it)) }
    }
    appScope.launch {
      for (action in actions) handle(action)
    }
  }

  fun onEligibilityChanged(value: LiveUpdateEligibility) {
    actions.trySend(Action.EligibilityChanged(value))
  }

  fun retry() {
    actions.trySend(Action.Retry)
  }

  private fun handle(action: Action) {
    when (action) {
      is Action.EligibilityChanged -> {
        if (eligibility == action.value) return
        eligibility = action.value
        advanceRevision()
        uploadIfNeeded()
        publishCurrentAck()
      }

      is Action.SnapshotChanged -> {
        snapshot = action.value
        advanceRevision()
        uploadIfNeeded()
        publishCurrentAck()
      }

      Action.Retry -> {
        advanceRevision()
        uploadIfNeeded()
      }

      is Action.UploadCompleted -> {
        if (inFlight != action.upload) return
        if (action.success) {
          lastSuccessfulRequest = action.upload.request
          if (action.upload.request.favorites == FavoriteIds.Empty) {
            serverClients.remove(action.upload.request.clientId)
            tokenPreferences.markFavoriteClientCleared(action.upload.request.clientId)
          } else {
            serverClients.add(action.upload.request.clientId)
          }
        }
        inFlight = null
        uploadIfNeeded()
        publishCurrentAck()
      }
    }
  }

  private fun uploadIfNeeded() {
    if (inFlight != null) return
    val current = snapshot ?: return
    val candidates = when (eligibility) {
      LiveUpdateEligibility.Pending -> emptyList()
      LiveUpdateEligibility.Enabled -> buildList {
        serverClients.filterTo(this) { it != current.clientId }
        add(current.clientId)
      }

      LiveUpdateEligibility.Disabled -> serverClients.toList()
    }.map { clientId ->
      if (eligibility == LiveUpdateEligibility.Enabled && clientId == current.clientId) {
        Request(clientId, current.favorites)
      } else {
        Request(clientId, FavoriteIds.Empty)
      }
    }
    val request = candidates.firstOrNull { candidate ->
      candidate != lastSuccessfulRequest && candidate !in attemptedRequests
    } ?: return
    val upload = Upload(request, revision)

    inFlight = upload
    attemptedRequests += request
    if (request != lastSuccessfulRequest) {
      lastSuccessfulRequest = null
      synced.value = null
    }
    if (request.favorites != FavoriteIds.Empty) {
      tokenPreferences.markFavoriteUploadAttempt(request.clientId)
      serverClients.add(request.clientId)
    }
    appScope.launch {
      val success = try {
        dataSource.replace(
          clientId = request.clientId,
          teams = request.favorites.teams,
          matches = request.favorites.matches,
          players = request.favorites.players,
          events = request.favorites.events,
        )
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (_: Exception) {
        false
      }
      actions.send(Action.UploadCompleted(upload, success))
    }
  }

  private fun advanceRevision() {
    revision++
    attemptedRequests.clear()
  }

  private fun publishCurrentAck() {
    val current = snapshot
    synced.value = if (eligibility == LiveUpdateEligibility.Enabled && inFlight == null && current != null &&
      lastSuccessfulRequest == Request(current.clientId, current.favorites)
    ) {
      SyncedFavorites(current.clientId, current.favorites)
    } else {
      null
    }
  }

  private data class ClientFavorites(val clientId: String, val favorites: FavoriteIds)

  internal data class FavoriteIds(
    val teams: List<String>,
    val matches: List<String>,
    val players: List<String>,
    val events: List<String>,
  ) {
    companion object {
      val Empty = FavoriteIds(emptyList(), emptyList(), emptyList(), emptyList())

      fun from(snapshot: DirectFavoriteSnapshot): FavoriteIds = FavoriteIds(
        teams = snapshot.teams.map { it.id }.distinct().sorted(),
        matches = snapshot.matches.map { it.id }.distinct().sorted(),
        players = snapshot.players.map { it.id }.distinct().sorted(),
        events = snapshot.events.map { it.id }.distinct().sorted(),
      )
    }
  }

  private data class Request(val clientId: String, val favorites: FavoriteIds)

  internal data class SyncedFavorites(val clientId: String, val favorites: FavoriteIds)

  private data class Upload(val request: Request, val revision: Long)

  private sealed interface Action {
    data class EligibilityChanged(val value: LiveUpdateEligibility) : Action

    data class SnapshotChanged(val value: ClientFavorites) : Action

    data class UploadCompleted(val upload: Upload, val success: Boolean) : Action

    data object Retry : Action
  }
}
