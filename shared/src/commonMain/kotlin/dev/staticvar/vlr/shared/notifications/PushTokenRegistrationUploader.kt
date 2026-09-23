/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.notifications

import dev.staticvar.vlr.core.identity.UserIdentityRepository
import dev.staticvar.vlr.core.notifications.PushPlatform
import dev.staticvar.vlr.core.settings.PushTokenRegistrationPreferencesRepository
import dev.staticvar.vlr.remotesource.liveupdates.PushTokenRegistrationDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

/** Uploads active push tokens and records successful registration for each client. */
internal class PushTokenRegistrationUploader(
  private val preferences: PushTokenRegistrationPreferencesRepository,
  private val identityRepository: UserIdentityRepository,
  private val dataSource: PushTokenRegistrationDataSource,
  private val appScope: CoroutineScope,
) {
  private val actions = Channel<Action>(Channel.UNLIMITED)
  private var clientId: String = identityRepository.id.value.toString()
  private var activeToken: RegistrationToken? = null
  private var inFlight: RegistrationRequest? = null
  private var lastAttempted: RegistrationRequest? = null
  private var pendingCleanup: Uuid? = identityRepository.pendingTokenCleanup.value
  private var cleanupInFlight: Uuid? = null
  private var lastCleanupAttempted: Uuid? = null

  init {
    appScope.launch {
      identityRepository.id.collect { id -> actions.send(Action.IdentityChanged(id.toString())) }
    }
    appScope.launch {
      identityRepository.pendingTokenCleanup.collect { id -> actions.send(Action.CleanupPending(id)) }
    }
    appScope.launch {
      for (action in actions) handle(action)
    }
  }

  fun activate(platform: PushPlatform, token: String) {
    if (token.isEmpty()) return
    actions.trySend(Action.TokenReceived(RegistrationToken(platform, token)))
  }

  fun deactivate() {
    actions.trySend(Action.Deactivated)
  }

  /** Retries a failed token registration or old-token deletion on the next foreground event. */
  fun retry() {
    actions.trySend(Action.Retry)
  }

  /**
   * Processes identity, token, and upload events one at a time.
   * Records successful requests before checking whether another upload is needed.
   */
  private fun handle(action: Action) {
    when (action) {
      is Action.IdentityChanged -> {
        clientId = action.clientId
        advance()
      }

      is Action.CleanupPending -> {
        pendingCleanup = action.clientId
        if (lastCleanupAttempted != pendingCleanup) lastCleanupAttempted = null
        advance()
      }

      is Action.TokenReceived -> {
        preferences.storeToken(action.token.platform, action.token.value)
        if (activeToken == action.token) return
        activeToken = action.token
        lastAttempted = null
        advance()
      }

      Action.Deactivated -> {
        activeToken = null
        lastAttempted = null
      }

      is Action.UploadCompleted -> {
        if (inFlight != action.request) return
        if (action.success) {
          preferences.markUploaded(
            clientId = action.request.clientId,
            platform = action.request.token.platform,
            token = action.request.token.value,
          )
        }
        inFlight = null
        advance()
      }

      is Action.CleanupCompleted -> {
        if (cleanupInFlight != action.clientId) return
        cleanupInFlight = null
        if (action.success) identityRepository.markTokenCleanupComplete(action.clientId)
        else lastCleanupAttempted = action.clientId
        advance()
      }

      Action.Retry -> {
        lastAttempted = null
        lastCleanupAttempted = null
        advance()
      }
    }
  }

  /** Registers the current token before deleting any token under a superseded UUID. */
  private fun advance() {
    uploadIfNeeded()
    cleanupIfNeeded()
  }

  /**
   * Uploads the active token only when this client registration is not already saved.
   * Avoids concurrent uploads and repeated attempts for the same request.
   */
  private fun uploadIfNeeded() {
    if (cleanupInFlight != null) return
    val token = activeToken ?: return
    val request = RegistrationRequest(clientId, token)
    if (preferences.preferences.value.wasUploaded(clientId, token.platform, token.value)) return
    if (request == inFlight || request == lastAttempted || inFlight != null) return

    inFlight = request
    lastAttempted = request
    appScope.launch {
      val success = try {
        dataSource.register(request.clientId, request.token.platform, request.token.value)
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (_: Exception) {
        false
      }
      actions.send(Action.UploadCompleted(request, success))
    }
  }

  /** Attempts each persisted cleanup once until a later foreground retry. */
  private fun cleanupIfNeeded() {
    val oldId = pendingCleanup ?: return
    if (oldId.toString() == clientId || cleanupInFlight != null || inFlight != null || oldId == lastCleanupAttempted) return
    val token = activeToken ?: preferences.preferences.value.let { saved ->
      val platform = saved.tokenPlatform ?: return
      val value = saved.token ?: return
      RegistrationToken(platform, value)
    }
    if (!preferences.preferences.value.wasUploaded(clientId, token.platform, token.value)) return
    cleanupInFlight = oldId
    lastCleanupAttempted = oldId
    appScope.launch {
      val success = try {
        dataSource.delete(oldId.toString())
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (_: Exception) {
        false
      }
      actions.send(Action.CleanupCompleted(oldId, success))
    }
  }

  /** A push token paired with its platform. */
  private data class RegistrationToken(val platform: PushPlatform, val value: String)

  /** Pairs a client identity with the token to register. */
  private data class RegistrationRequest(val clientId: String, val token: RegistrationToken)

  /** Events processed in order by the token uploader. */
  private sealed interface Action {
    /** Reports the client identity to use for registration. */
    data class IdentityChanged(val clientId: String) : Action

    /** Supplies a token to register for the active client. */
    data class TokenReceived(val token: RegistrationToken) : Action

    /** Reports the outcome of a token registration request. */
    data class UploadCompleted(val request: RegistrationRequest, val success: Boolean) : Action

    /** Reports a persisted UUID whose token should be deleted. */
    data class CleanupPending(val clientId: Uuid?) : Action

    /** Reports an old-token deletion response. */
    data class CleanupCompleted(val clientId: Uuid, val success: Boolean) : Action

    /** Allows one more attempt after foregrounding. */
    data object Retry : Action

    /** Stops further uploads of the active token. */
    data object Deactivated : Action
  }
}
