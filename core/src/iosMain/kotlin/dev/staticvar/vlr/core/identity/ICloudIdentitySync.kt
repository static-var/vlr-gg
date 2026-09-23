/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.identity

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
internal class ICloudIdentitySync(private val repository: UserIdentityRepository) {
  private val store = NSUbiquitousKeyValueStore.defaultStore
  private var accountChanged = false
  private val observer = NSNotificationCenter.defaultCenter.addObserverForName(
    NSUbiquitousKeyValueStoreDidChangeExternallyNotification,
    store,
    NSOperationQueue.mainQueue,
  ) { notification ->
    when ((notification?.userInfo?.get(NSUbiquitousKeyValueStoreChangeReasonKey) as? NSNumber)?.longLongValue) {
      NSUbiquitousKeyValueStoreAccountChange -> accountChanged = true
      NSUbiquitousKeyValueStoreInitialSyncChange -> {
        if (!accountChanged) {
          repository.restoreFromBackup(store.stringForKey(UserIdentityRepository.IdentityKey), confirmedByCloud = false)
        }
      }
      NSUbiquitousKeyValueStoreServerChange -> {
        val changedKeys = notification.userInfo?.get(NSUbiquitousKeyValueStoreChangedKeysKey) as? List<*>
        if (!accountChanged && changedKeys?.contains(UserIdentityRepository.IdentityKey) == true) {
          repository.restoreFromBackup(store.stringForKey(UserIdentityRepository.IdentityKey))
        }
      }
    }
  }

  init {
    store.synchronize()
    seedBackup()
  }

  /**
   * Uses an available cloud identity before seeding iCloud with the local value.
   * Later cloud notifications reconcile a backup that has not arrived yet.
   */
  private fun seedBackup() {
    val backedUpId = store.stringForKey(UserIdentityRepository.IdentityKey)
    repository.restoreFromBackup(backedUpId, confirmedByCloud = false)
    if (UserIdentityRepository.parseIdentity(backedUpId) != repository.id.value) {
      // Initial/server notifications reconcile a cloud value that arrives after this local seed.
      store.setString(repository.id.value.toString(), UserIdentityRepository.IdentityKey)
    }
  }

  /**
   * Removes the iCloud change observer when this synchronization owner is released.
   */
  fun close() {
    NSNotificationCenter.defaultCenter.removeObserver(observer)
  }
}
