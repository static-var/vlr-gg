/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import dev.staticvar.vlr.shared.widget.favorite.WidgetKey
import dev.staticvar.vlr.shared.widget.favorite.WidgetKind
import kotlinx.coroutines.CancellationException
import java.io.IOException

internal class FavoriteEntityFetchWorker(context: Context, params: WorkerParameters) :
  CoroutineWorker(context, params) {
  override suspend fun doWork(): Result = widgetWork {
    val kind = WidgetKind.entries.firstOrNull { it.name == inputData.getString("kind") } ?: return@widgetWork
    val key = WidgetKey.parse(kind, inputData.getString("entity").orEmpty(), inputData.getString("client").orEmpty())
      ?: return@widgetWork
    FavoriteWidgetRuntime.get().fetch(key)
  }
}

internal class FavoriteEntityRenderWorker(context: Context, params: WorkerParameters) :
  CoroutineWorker(context, params) {
  override suspend fun doWork(): Result = widgetWork { FavoriteWidgetRuntime.get().render() }
}

internal class FavoriteEntityReconcileWorker(context: Context, params: WorkerParameters) :
  CoroutineWorker(context, params) {
  override suspend fun doWork(): Result = widgetWork { FavoriteWidgetRuntime.get().changed() }
}

private suspend fun widgetWork(block: suspend () -> Unit): Result = try {
  block()
  Result.success()
} catch (cancelled: CancellationException) {
  throw cancelled
} catch (failure: IOException) {
  Log.w("FavoriteEntityWidgets", "Could not access widget storage", failure)
  Result.retry()
}
