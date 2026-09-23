/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.identity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSUbiquitousKeyValueStore
import platform.Foundation.NSUbiquitousKeyValueStoreAccountChange
import platform.Foundation.NSUbiquitousKeyValueStoreChangeReasonKey
import platform.Foundation.NSUbiquitousKeyValueStoreChangedKeysKey
import platform.Foundation.NSUbiquitousKeyValueStoreDidChangeExternallyNotification
import platform.Foundation.NSUbiquitousKeyValueStoreInitialSyncChange
import platform.Foundation.NSUbiquitousKeyValueStoreServerChange

/** Reconciles the local user UUID with iCloud key-value storage. */
internal class ICloudIdentitySync(repository: UserIdentityRepository) {
  private val store = NSUbiquitousKeyValueStore.defaultStore
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private val reconciler = ICloudIdentityReconciler(
    repository = repository,
    readCloudId = { store.stringForKey(UserIdentityRepository.IdentityKey) },
    writeCloudId = { store.setString(it, UserIdentityRepository.IdentityKey) },
    scheduleRetry = { delayMillis, action ->
      val job = scope.launch {
        delay(delayMillis)
        action()
      }
      ({ job.cancel() })
    },
  )
  private val observer = NSNotificationCenter.defaultCenter.addObserverForName(
    NSUbiquitousKeyValueStoreDidChangeExternallyNotification,
    store,
    NSOperationQueue.mainQueue,
  ) { notification ->
    when ((notification?.userInfo?.get(NSUbiquitousKeyValueStoreChangeReasonKey) as? NSNumber)?.longLongValue) {
      NSUbiquitousKeyValueStoreAccountChange -> reconciler.onAccountChange()
      NSUbiquitousKeyValueStoreInitialSyncChange -> reconciler.onInitialSyncChange()
      NSUbiquitousKeyValueStoreServerChange -> {
        val changedKeys = notification.userInfo?.get(NSUbiquitousKeyValueStoreChangedKeysKey) as? List<*>
        if (changedKeys?.contains(UserIdentityRepository.IdentityKey) == true) reconciler.onServerChange()
      }
    }
  }

  init {
    store.synchronize()
    reconciler.seed()
  }

  /**
   * Removes the iCloud change observer when this synchronization owner is released.
   */
  fun close() {
    NSNotificationCenter.defaultCenter.removeObserver(observer)
    reconciler.close()
    scope.cancel()
  }
}
