/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android

import androidx.lifecycle.ViewModel
import java.lang.ref.WeakReference
import leakcanary.AppWatcher

internal val viewModelLeakObserver: ((ViewModel) -> Unit) = { viewModel ->
  if (viewModel.getCloseable<AutoCloseable>(LeakWatcherKey) == null) {
    val reference = WeakReference(viewModel)
    viewModel.addCloseable(LeakWatcherKey) {
      reference.get()?.let { clearedViewModel ->
        AppWatcher.objectWatcher.expectWeaklyReachable(
          clearedViewModel,
          "${clearedViewModel.javaClass.name} was cleared",
        )
      }
    }
  }
}

private const val LeakWatcherKey = "dev.staticvar.vlr.ViewModelLeakWatcher"
