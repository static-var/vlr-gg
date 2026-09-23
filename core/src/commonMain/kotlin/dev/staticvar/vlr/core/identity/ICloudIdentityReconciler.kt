/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.identity

/**
 * Reconciles an iCloud UUID with the local identity across initial download and server changes.
 * A retry rereads iCloud before writing because a delayed cloud identity may have arrived.
 */
internal class ICloudIdentityReconciler(
  private val repository: UserIdentityRepository,
  private val readCloudId: () -> String?,
  private val writeCloudId: (String) -> Unit,
  private val scheduleRetry: (Long, () -> Unit) -> (() -> Unit),
) {
  private var accountChanged = false
  private var closed = false
  private var retryIndex = 0
  private var cancelRetry: (() -> Unit)? = null

  /** Restores an available cloud UUID, or publishes the local UUID when iCloud has none. */
  fun seed() {
    if (accountChanged || closed) return
    val cloudId = readCloudId()
    repository.restoreFromBackup(cloudId, confirmedByCloud = false)
    if (UserIdentityRepository.parseIdentity(cloudId) == null) {
      writeCloudId(repository.id.value.toString())
    }
  }

  /** Initial sync rejected a write while cloud download was in progress. */
  fun onInitialSyncChange() {
    if (accountChanged || closed) return
    val localId = repository.id.value
    val cloudId = readCloudId()
    repository.restoreFromBackup(cloudId, confirmedByCloud = false)
    val parsedCloudId = UserIdentityRepository.parseIdentity(cloudId)
    if (parsedCloudId != null && parsedCloudId != localId) {
      cancelPendingRetry()
      return
    }
    scheduleNextRetry()
  }

  /** A server change can confirm the restored identity and end the retry sequence. */
  fun onServerChange() {
    if (accountChanged || closed) return
    val cloudId = readCloudId()
    repository.restoreFromBackup(cloudId)
    if (UserIdentityRepository.parseIdentity(cloudId) != null) cancelPendingRetry()
  }

  /** Stops reconciliation when the active iCloud account changes. */
  fun onAccountChange() {
    accountChanged = true
    cancelPendingRetry()
  }

  /** Cancels delayed work when the iCloud synchronization owner closes. */
  fun close() {
    closed = true
    cancelPendingRetry()
  }

  /** Queues the next bounded attempt; the callback checks cloud state before writing. */
  private fun scheduleNextRetry() {
    if (accountChanged || closed || cancelRetry != null || retryIndex == RetryDelaysMillis.size) return
    val delayMillis = RetryDelaysMillis[retryIndex++]
    cancelRetry = scheduleRetry(delayMillis, retry@{
      cancelRetry = null
      if (accountChanged || closed) return@retry

      val localId = repository.id.value
      val cloudId = readCloudId()
      val parsedCloudId = UserIdentityRepository.parseIdentity(cloudId)
      repository.restoreFromBackup(cloudId, confirmedByCloud = false)
      if (parsedCloudId != null && parsedCloudId != localId) return@retry

      // An equal cached value may be the local write rejected during initial download.
      writeCloudId(repository.id.value.toString())
      scheduleNextRetry()
    })
  }

  private fun cancelPendingRetry() {
    cancelRetry?.invoke()
    cancelRetry = null
  }

  /** Delays allow initial cloud download time while bounding repeated writes. */
  private companion object {
    val RetryDelaysMillis = longArrayOf(1_000L, 3_000L, 9_000L)
  }
}
