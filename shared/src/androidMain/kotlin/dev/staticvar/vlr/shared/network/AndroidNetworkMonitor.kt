/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.network.NetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

public fun androidNetworkModule(context: Context): Module {
  val applicationContext = context.applicationContext
  return module {
    single { AndroidNetworkMonitor(applicationContext) } withOptions {
      bind<NetworkMonitor>()
      onClose { it?.close() }
    }
  }
}

internal class AndroidNetworkMonitor(context: Context) : NetworkMonitor {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val manager = context.getSystemService(ConnectivityManager::class.java)

  override val status: StateFlow<NetworkStatus> = callbackFlow {
    val state = AndroidNetworkState(this) { trySend(it) }
    val callbackLock = Any()
    fun probe() {
      val network = manager.activeNetwork
      state.update(network?.networkHandle, manager.getNetworkCapabilities(network)?.hasValidatedInternet() == true)
    }
    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) = synchronized(callbackLock) {
        state.update(network.networkHandle, false)
      }
      override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = synchronized(callbackLock) {
        if (network == manager.activeNetwork) {
          state.update(network.networkHandle, capabilities.hasValidatedInternet())
        }
      }
      override fun onLost(network: Network) = synchronized(callbackLock) {
        state.lost(network.networkHandle)
      }
    }
    synchronized(callbackLock) {
      manager.registerDefaultNetworkCallback(callback)
      probe()
    }
    awaitClose {
      state.close()
      manager.unregisterNetworkCallback(callback)
    }
  }.stateIn(scope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), NetworkStatus.Unknown)

  fun close() = scope.cancel()
}

private fun NetworkCapabilities.hasValidatedInternet(): Boolean =
  hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
    hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
