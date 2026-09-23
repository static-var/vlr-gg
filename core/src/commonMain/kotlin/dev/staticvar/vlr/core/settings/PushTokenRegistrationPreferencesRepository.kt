/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.Settings
import dev.staticvar.vlr.core.notifications.PushPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Holds the current push token and the last registration acknowledged by the backend. */
public data class PushTokenRegistrationPreferences(
  val token: String? = null,
  val tokenPlatform: PushPlatform? = null,
  val uploadedToken: String? = null,
  val uploadedPlatform: PushPlatform? = null,
  val uploadedClientId: String? = null,
) {
  public fun wasUploaded(clientId: String, platform: PushPlatform, token: String): Boolean =
    uploadedClientId == clientId && uploadedPlatform == platform && uploadedToken == token
}

/** Persists push registration state and clients whose server favorites may need clearing. */
public class PushTokenRegistrationPreferencesRepository(private val storage: Settings) {
  public val preferences: StateFlow<PushTokenRegistrationPreferences>
    field = MutableStateFlow(readPreferences())

  public fun storeToken(platform: PushPlatform, token: String) {
    storage.putString(TokenKey, token)
    storage.putString(TokenPlatformKey, platform.name)
    preferences.value = preferences.value.copy(token = token, tokenPlatform = platform)
  }

  /**
   * Records the exact client, platform, and token acknowledged by the backend.
   * A rotated token or restored identity must receive its own acknowledgement.
   */
  public fun markUploaded(clientId: String, platform: PushPlatform, token: String) {
    storage.putString(UploadedClientIdKey, clientId)
    storage.putString(UploadedPlatformKey, platform.name)
    storage.putString(UploadedTokenKey, token)
    preferences.value = preferences.value.copy(
      uploadedClientId = clientId,
      uploadedPlatform = platform,
      uploadedToken = token,
    )
  }

  public fun possiblySyncedFavoriteClients(): Set<String> = storage.getStringOrNull(FavoriteClientsKey)
    ?.split(',')
    ?.filter(String::isNotBlank)
    ?.toSet()
    .orEmpty()

  /**
   * Records a possible server write before sending favorites.
   * This keeps opt-out cleanup possible even when the server response is lost.
   */
  public fun markFavoriteUploadAttempt(clientId: String) {
    storage.putString(FavoriteClientsKey, (possiblySyncedFavoriteClients() + clientId).joinToString(","))
  }

  public fun markFavoriteClientCleared(clientId: String) {
    storage.putString(FavoriteClientsKey, (possiblySyncedFavoriteClients() - clientId).joinToString(","))
  }

  private fun readPreferences(): PushTokenRegistrationPreferences = PushTokenRegistrationPreferences(
    token = storage.getStringOrNull(TokenKey),
    tokenPlatform = storage.getStringOrNull(TokenPlatformKey).toPushPlatformOrNull(),
    uploadedToken = storage.getStringOrNull(UploadedTokenKey),
    uploadedPlatform = storage.getStringOrNull(UploadedPlatformKey).toPushPlatformOrNull(),
    uploadedClientId = storage.getStringOrNull(UploadedClientIdKey),
  )

  private fun String?.toPushPlatformOrNull(): PushPlatform? =
    this?.let { stored -> PushPlatform.entries.firstOrNull { it.name == stored } }

  /** Defines preference keys for tokens, acknowledgements, and pending favorites cleanup. */
  private companion object {
    const val TokenKey: String = "notifications.pushToken"
    const val TokenPlatformKey: String = "notifications.pushTokenPlatform"
    const val UploadedTokenKey: String = "notifications.uploadedPushToken"
    const val UploadedPlatformKey: String = "notifications.uploadedPushTokenPlatform"
    const val UploadedClientIdKey: String = "notifications.uploadedPushTokenClientId"
    const val FavoriteClientsKey: String = "notifications.possiblySyncedFavoriteClients"
  }
}
