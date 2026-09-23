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

  init {
    appScope.launch {
      identityRepository.id.collect { id -> actions.send(Action.IdentityChanged(id.toString())) }
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

  /**
   * Processes identity, token, and upload events one at a time.
   * Records successful requests before checking whether another upload is needed.
   */
  private fun handle(action: Action) {
    when (action) {
      is Action.IdentityChanged -> {
        clientId = action.clientId
        uploadIfNeeded()
      }

      is Action.TokenReceived -> {
        preferences.storeToken(action.token.platform, action.token.value)
        if (activeToken == action.token) return
        activeToken = action.token
        lastAttempted = null
        uploadIfNeeded()
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
        uploadIfNeeded()
      }
    }
  }

  /**
   * Uploads the active token only when this client registration is not already saved.
   * Avoids concurrent uploads and repeated attempts for the same request.
   */
  private fun uploadIfNeeded() {
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

    /** Stops further uploads of the active token. */
    data object Deactivated : Action
  }
}
