/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.identity

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Stores one stable UUID per installation and tracks a superseded cloud token for cleanup. */
public class UserIdentityRepository(
  private val storage: Settings,
  backupId: String? = null,
  trackCloudBackup: Boolean = false,
) {
  private val localId = parseIdentity(storage.getStringOrNull(IdentityKey))
  private val cachedBackupId = parseIdentity(backupId)
  private var awaitingCloudBackup = if (localId != null) {
    storage.getBoolean(AwaitingCloudBackupKey, false)
  } else {
    trackCloudBackup && cachedBackupId == null
  }
  private var supersededCloudId = parseIdentity(storage.getStringOrNull(SupersededCloudIdKey))

  public val id: StateFlow<Uuid>
    field = MutableStateFlow(localId ?: cachedBackupId ?: generateIdentity())

  /** A previous cloud UUID whose token must be removed after the active token is registered. */
  public val pendingTokenCleanup: StateFlow<Uuid?>
    field = MutableStateFlow(
      parseIdentity(storage.getStringOrNull(PendingTokenCleanupKey))?.takeUnless { it == id.value },
    )

  init {
    storage.putString(IdentityKey, id.value.toString())
    storage.putBoolean(AwaitingCloudBackupKey, awaitingCloudBackup)
    if (pendingTokenCleanup.value == null) storage.remove(PendingTokenCleanupKey)
  }

  /**
   * Records a late cloud UUID only when this installation generated its UUID before cloud download.
   * Returns whether iCloud should be updated with the retained local UUID.
   */
  public fun observeCloudIdentity(value: String?, confirmedByCloud: Boolean = false): Boolean {
    val cloudId = parseIdentity(value) ?: return false
    if (cloudId == id.value) {
      if (confirmedByCloud) {
        awaitingCloudBackup = false
        supersededCloudId = null
        storage.putBoolean(AwaitingCloudBackupKey, false)
        storage.remove(SupersededCloudIdKey)
      }
      return false
    }
    if (supersededCloudId == cloudId) return true
    if (!awaitingCloudBackup) return false
    storage.putString(PendingTokenCleanupKey, cloudId.toString())
    storage.putString(SupersededCloudIdKey, cloudId.toString())
    storage.putBoolean(AwaitingCloudBackupKey, false)
    supersededCloudId = cloudId
    pendingTokenCleanup.value = cloudId
    awaitingCloudBackup = false
    return true
  }

  /** Removes a completed cleanup only if it still names the pending, inactive UUID. */
  public fun markTokenCleanupComplete(value: Uuid) {
    if (value == id.value || pendingTokenCleanup.value != value) return
    storage.remove(PendingTokenCleanupKey)
    pendingTokenCleanup.value = null
  }

  /** Prevents a future account's cloud UUID from being treated as this install's old backup. */
  public fun stopAwaitingCloudBackup() {
    awaitingCloudBackup = false
    storage.putBoolean(AwaitingCloudBackupKey, false)
  }

  /** Defines identity storage keys and helpers for validating and generating UUIDs. */
  public companion object {
    public const val IdentityKey: String = "identity.uuid"
    private const val AwaitingCloudBackupKey: String = "identity.awaitingCloudBackup"
    private const val PendingTokenCleanupKey: String = "identity.pendingTokenCleanup"
    private const val SupersededCloudIdKey: String = "identity.supersededCloudId"

    internal fun parseIdentity(value: String?): Uuid? = value?.let(Uuid::parseOrNull)
      ?.takeUnless { it == Uuid.NIL || it == Uuid.fromLongs(-1L, -1L) }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateIdentity(): Uuid = Uuid.generateV7()
  }
}
