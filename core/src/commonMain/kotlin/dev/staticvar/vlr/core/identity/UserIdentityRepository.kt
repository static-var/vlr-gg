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

public class UserIdentityRepository(
  private val storage: Settings,
  backupId: String? = null,
  allowDelayedRestore: Boolean = false,
) {
  private val localId = parseIdentity(storage.getStringOrNull(IdentityKey))
  private var awaitingBackup = allowDelayedRestore && if (localId != null) {
    storage.getBoolean(RestorePendingKey, false)
  } else {
    parseIdentity(backupId) == null
  }

  public val id: StateFlow<Uuid>
    field = MutableStateFlow(
      localId
        ?: parseIdentity(backupId)
        ?: generateIdentity(),
    )

  init {
    storage.putBoolean(RestorePendingKey, awaitingBackup)
    storage.putString(IdentityKey, id.value.toString())
  }

  public fun restoreFromBackup(value: String?, confirmedByCloud: Boolean = true) {
    if (!awaitingBackup) return
    val restored = parseIdentity(value) ?: return
    storage.putString(IdentityKey, restored.toString())
    id.value = restored
    if (confirmedByCloud) {
      awaitingBackup = false
      storage.putBoolean(RestorePendingKey, false)
    }
  }

  public companion object {
    public const val IdentityKey: String = "identity.uuid"
    private const val RestorePendingKey: String = "identity.restorePending"

    internal fun parseIdentity(value: String?): Uuid? = value?.let(Uuid::parseOrNull)
      ?.takeUnless { it == Uuid.NIL || it == Uuid.fromLongs(-1L, -1L) }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateIdentity(): Uuid = Uuid.generateV7()
  }
}
