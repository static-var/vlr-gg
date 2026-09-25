/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.notifications

import dev.staticvar.vlr.core.identity.UserIdentityRepository
import dev.staticvar.vlr.core.settings.PushTokenRegistrationPreferencesRepository
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.repository.FavoriteSyncStateRepository
import dev.staticvar.vlr.domain.repository.FavoriteSyncStatus
import dev.staticvar.vlr.remotesource.liveupdates.FavoriteGroups
import dev.staticvar.vlr.remotesource.liveupdates.FavoriteLiveUpdateDataSource
import dev.staticvar.vlr.remotesource.liveupdates.FavoriteReadResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Whether live updates are allowed or still awaiting an access check. */
internal enum class LiveUpdateEligibility {
  Pending,
  Enabled,
  Disabled,
}

/** Syncs favorites independently of notification access and clears superseded clients. */
internal class FavoriteLiveUpdateCoordinator(
  private val identityRepository: UserIdentityRepository,
  private val favoritesRepository: FavoritesRepository,
  private val syncState: FavoriteSyncStateRepository,
  private val tokenPreferences: PushTokenRegistrationPreferencesRepository,
  private val dataSource: FavoriteLiveUpdateDataSource,
  private val appScope: CoroutineScope,
) : FavoriteSyncStatus {
  private val actions = Channel<Action>(Channel.UNLIMITED)
  private var eligibility = LiveUpdateEligibility.Pending
  private var snapshot: ClientFavorites? = null
  private var reconciliationEpoch = 0L
  private var inFlight: Upload? = null
  private val attemptedRequests = mutableSetOf<Request>()
  private val lastSuccessfulRequests = mutableMapOf<String, Request>()
  private var retryJob: Job? = null
  private var retryDelayMillis = 30_000L
  private val serverClients = linkedSetOf<String>().apply {
    addAll(tokenPreferences.possiblySyncedFavoriteClients())
    tokenPreferences.preferences.value.uploadedClientId?.let(::add)
  }
  private val synced = MutableStateFlow<SyncedFavorites?>(null)
  val syncedFavorites: StateFlow<SyncedFavorites?> = synced
  private val generation = MutableStateFlow(0L)
  override val syncGeneration: StateFlow<Long> = generation

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

  fun reconcile() {
    actions.trySend(Action.Reconcile)
  }

  /**
   * Processes changes and upload results one at a time.
   * Updates server subscription tracking before choosing the next request.
   */
  private suspend fun handle(action: Action) {
    when (action) {
      is Action.EligibilityChanged -> {
        if (eligibility == action.value) return
        eligibility = action.value
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

      Action.Reconcile -> {
        reconciliationEpoch++
        advanceRevision()
        uploadIfNeeded()
        publishCurrentAck()
      }

      is Action.UploadCompleted -> {
        if (inFlight != action.upload) return
        if (action.success) {
          if (action.upload.request.clientId != identityRepository.id.value.toString() ||
            action.upload.request.clientId != snapshot?.clientId
          ) {
            if (action.upload.request.favorites == FavoriteIds.Empty) {
              serverClients.remove(action.upload.request.clientId)
              lastSuccessfulRequests.remove(action.upload.request.clientId)
              tokenPreferences.markFavoriteClientCleared(action.upload.request.clientId)
            } else {
              serverClients.add(action.upload.request.clientId)
            }
          } else {
            serverClients.add(action.upload.request.clientId)
            val freshFavorites = FavoriteIds.from(favoritesRepository.observeDirectFavorites().first())
            val currentId = identityRepository.id.value.toString()
            if (currentId == action.upload.request.clientId && freshFavorites == action.upload.request.favorites &&
              syncState.markSynced(currentId, action.upload.localRevision)
            ) {
              lastSuccessfulRequests[currentId] = action.upload.request
              generation.value++
            } else {
              lastSuccessfulRequests.remove(currentId)
              if (currentId == action.upload.request.clientId) snapshot = ClientFavorites(currentId, freshFavorites)
              advanceRevision()
            }
          }
          if (serverClients.none { it != snapshot?.clientId }) retryDelayMillis = 30_000L
        } else {
          scheduleRetry()
        }
        inFlight = null
        uploadIfNeeded()
        publishCurrentAck()
      }
    }
  }

  /**
   * Reconciles the current favorites and clears superseded clients.
   * Allows one request at a time and one attempt per snapshot until a change or retry.
   */
  private suspend fun uploadIfNeeded() {
    if (inFlight != null) return
    val current = snapshot ?: return
    val candidates = buildList {
      add(current.clientId)
      serverClients.filterTo(this) { it != current.clientId }
    }.map { clientId ->
      Request(
        clientId,
        if (clientId == current.clientId) current.favorites else FavoriteIds.Empty,
        if (clientId == current.clientId) reconciliationEpoch else 0L,
      )
    }
    val request = candidates.firstOrNull { candidate ->
      candidate != lastSuccessfulRequests[candidate.clientId] && candidate !in attemptedRequests
    } ?: return
    val localRevision = if (request.clientId == current.clientId) syncState.beginUpload() else -1L
    val upload = Upload(request, localRevision)

    inFlight = upload
    attemptedRequests += request
    if (request.clientId == current.clientId) {
      lastSuccessfulRequests.remove(current.clientId)
      synced.value = null
    }
    tokenPreferences.markFavoriteUploadAttempt(request.clientId)
    serverClients.add(request.clientId)
    appScope.launch {
      val success = try {
        reconcile(upload)
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (_: Exception) {
        false
      }
      actions.send(Action.UploadCompleted(upload, success))
    }
  }

  private suspend fun reconcile(upload: Upload): Boolean {
    val clientId = upload.request.clientId
    val desired = upload.request.favorites.asGroups()
    return when (val result = dataSource.read(clientId)) {
      FavoriteReadResult.Failure -> false
      FavoriteReadResult.NotRegistered -> upload.localRevision < 0L ||
        (dataSource.add(clientId, desired) && verify(clientId, desired, allowNotRegistered = false))
      is FavoriteReadResult.Found -> {
        val missing = desired.minus(result.favorites)
        val stale = result.favorites.minus(desired)
        val removed = stale.isEmpty() || dataSource.remove(clientId, stale)
        val added = missing.isEmpty() || dataSource.add(clientId, missing)
        if (!removed || !added) {
          false
        } else if (missing.isEmpty() && stale.isEmpty()) {
          true
        } else {
          verify(clientId, desired, allowNotRegistered = upload.localRevision < 0L)
        }
      }
    }
  }

  private suspend fun verify(clientId: String, desired: FavoriteGroups, allowNotRegistered: Boolean): Boolean =
    when (val result = dataSource.read(clientId)) {
      is FavoriteReadResult.Found -> result.favorites.sameIds(desired)
      FavoriteReadResult.NotRegistered -> allowNotRegistered && desired.isEmpty()
      FavoriteReadResult.Failure -> false
    }

  private fun FavoriteGroups.minus(other: FavoriteGroups): FavoriteGroups = FavoriteGroups(
    teams = teams.filterNot(other.teams.toSet()::contains),
    matches = matches.filterNot(other.matches.toSet()::contains),
    players = players.filterNot(other.players.toSet()::contains),
    events = events.filterNot(other.events.toSet()::contains),
  )

  private fun FavoriteGroups.isEmpty(): Boolean =
    teams.isEmpty() && matches.isEmpty() && players.isEmpty() && events.isEmpty()

  private fun FavoriteGroups.sameIds(other: FavoriteGroups): Boolean =
    teams.toSet() == other.teams.toSet() && matches.toSet() == other.matches.toSet() &&
      players.toSet() == other.players.toSet() && events.toSet() == other.events.toSet()

  private fun advanceRevision() {
    attemptedRequests.clear()
  }

  private fun scheduleRetry() {
    if (retryJob?.isActive == true) return
    val delayMillis = retryDelayMillis
    retryDelayMillis = (retryDelayMillis * 2).coerceAtMost(30 * 60_000L)
    retryJob = appScope.launch {
      delay(delayMillis)
      actions.send(Action.Retry)
    }
  }

  /**
   * Publishes confirmation only when the server matches the current eligible favorites.
   * Uploads still continue when notification access is disabled.
   */
  private fun publishCurrentAck() {
    val current = snapshot
    synced.value = if (eligibility == LiveUpdateEligibility.Enabled &&
      inFlight?.request?.clientId != current?.clientId && current != null &&
      lastSuccessfulRequests[current.clientId] == Request(current.clientId, current.favorites, reconciliationEpoch)
    ) {
      SyncedFavorites(current.clientId, current.favorites)
    } else {
      null
    }
  }

  /** Pairs the current client identity with its selected favorites. */
  private data class ClientFavorites(val clientId: String, val favorites: FavoriteIds)

  /** Sorted, unique favorite IDs grouped by entity type. */
  internal data class FavoriteIds(
    val teams: List<String>,
    val matches: List<String>,
    val players: List<String>,
    val events: List<String>,
  ) {
    /** Creates empty or normalized favorite ID groups. */
    companion object {
      val Empty = FavoriteIds(emptyList(), emptyList(), emptyList(), emptyList())

      fun from(snapshot: DirectFavoriteSnapshot): FavoriteIds = FavoriteIds(
        teams = snapshot.teams.map { canonicalId(it.id) }.distinct().sorted(),
        matches = snapshot.matches.map { canonicalId(it.id) }.distinct().sorted(),
        players = snapshot.players.map { canonicalId(it.id) }.distinct().sorted(),
        events = snapshot.events.map { canonicalId(it.id) }.distinct().sorted(),
      )

      private fun canonicalId(id: String): String = id.toLongOrNull()?.takeIf { it > 0 }?.toString() ?: id
    }

    fun asGroups(): FavoriteGroups = FavoriteGroups(teams, matches, players, events)
  }

  /** Desired favorites for one client. */
  private data class Request(val clientId: String, val favorites: FavoriteIds, val reconciliationEpoch: Long)

  /** The current client favorites confirmed by the server. */
  internal data class SyncedFavorites(val clientId: String, val favorites: FavoriteIds)

  /** Tracks a favorite upload and the local database revision that started it. */
  private data class Upload(val request: Request, val localRevision: Long)

  /** Events processed in order by the favorite sync coordinator. */
  private sealed interface Action {
    /** Reports a change in access to live updates. */
    data class EligibilityChanged(val value: LiveUpdateEligibility) : Action

    /** Supplies the latest client identity and favorites. */
    data class SnapshotChanged(val value: ClientFavorites) : Action

    /** Reports the outcome of a favorite upload. */
    data class UploadCompleted(val upload: Upload, val success: Boolean) : Action

    /** Requests another attempt to sync the current favorites. */
    data object Retry : Action

    data object Reconcile : Action
  }
}
