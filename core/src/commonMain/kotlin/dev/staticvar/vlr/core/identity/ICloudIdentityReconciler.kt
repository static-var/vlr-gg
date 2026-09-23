/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.identity

/**
 * Publishes the stable local UUID to iCloud and records a late older cloud UUID for token cleanup.
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

  /** Publishes the local UUID when iCloud has none or reports a superseded UUID. */
  fun seed() {
    if (accountChanged || closed) return
    val cloudId = readCloudId()
    val publishLocal = repository.observeCloudIdentity(cloudId)
    if (publishLocal || UserIdentityRepository.parseIdentity(cloudId) == null) {
      writeCloudId(repository.id.value.toString())
      if (publishLocal) scheduleNextRetry()
    }
  }

  /** Initial sync rejected a write while cloud download was in progress. */
  fun onInitialSyncChange() {
    if (accountChanged || closed) return
    val cloudId = readCloudId()
    val publishLocal = repository.observeCloudIdentity(cloudId)
    val parsedCloudId = UserIdentityRepository.parseIdentity(cloudId)
    if (publishLocal) writeCloudId(repository.id.value.toString())
    if (parsedCloudId != null && parsedCloudId != repository.id.value && !publishLocal) {
      cancelPendingRetry()
      return
    }
    scheduleNextRetry()
  }

  /** A server change can confirm the local identity or reveal an older cloud UUID. */
  fun onServerChange() {
    if (accountChanged || closed) return
    val cloudId = readCloudId()
    val publishLocal = repository.observeCloudIdentity(cloudId, confirmedByCloud = true)
    if (publishLocal) {
      writeCloudId(repository.id.value.toString())
      scheduleNextRetry()
    } else if (UserIdentityRepository.parseIdentity(cloudId) != null) {
      cancelPendingRetry()
    }
  }

  /** Stops reconciliation when the active iCloud account changes. */
  fun onAccountChange() {
    accountChanged = true
    repository.stopAwaitingCloudBackup()
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

      val cloudId = readCloudId()
      val parsedCloudId = UserIdentityRepository.parseIdentity(cloudId)
      val publishLocal = repository.observeCloudIdentity(cloudId)
      if (parsedCloudId != null && parsedCloudId != repository.id.value && !publishLocal) return@retry

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
