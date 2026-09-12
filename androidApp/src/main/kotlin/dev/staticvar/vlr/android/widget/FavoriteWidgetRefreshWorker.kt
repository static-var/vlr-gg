/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.staticvar.vlr.shared.widget.refreshFavoriteWidgetSnapshot
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

internal class FavoriteWidgetRefreshWorker(
  appContext: Context,
  workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
  override suspend fun doWork(): Result {
    val currentJson = WidgetSnapshotStore.read(applicationContext) ?: return Result.success()
    if (parseWidgetSnapshot(currentJson) == null) return Result.failure()

    return try {
      val refreshedJson = refreshFavoriteWidgetSnapshot(currentJson)
      when (WidgetSnapshotStore.replaceIfUnchanged(applicationContext, currentJson, refreshedJson)) {
        SnapshotReplaceResult.WRITTEN -> {
          FavoriteMatchWidgets.updateAll(applicationContext)
          Result.success()
        }
        SnapshotReplaceResult.UNCHANGED,
        SnapshotReplaceResult.STALE,
        -> Result.success()
        SnapshotReplaceResult.FAILED -> Result.retry()
      }
    } catch (error: CancellationException) {
      throw error
    } catch (_: Exception) {
      Result.retry()
    }
  }
}

internal object FavoriteWidgetRefreshScheduler {
  private const val IMMEDIATE_WORK_NAME = "favorite-widget-refresh-now"
  private const val PERIODIC_WORK_NAME = "favorite-widget-refresh-periodic"
  private const val REFRESH_INTERVAL_MINUTES = 30L
  private val networkConstraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()

  fun schedule(context: Context) {
    val request = PeriodicWorkRequest.Builder(
      FavoriteWidgetRefreshWorker::class.java,
      REFRESH_INTERVAL_MINUTES,
      TimeUnit.MINUTES,
    )
      .setInitialDelay(REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES)
      .setConstraints(networkConstraints)
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
      .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
      PERIODIC_WORK_NAME,
      ExistingPeriodicWorkPolicy.KEEP,
      request,
    )
  }

  fun refreshNow(context: Context) {
    val request = OneTimeWorkRequest.Builder(FavoriteWidgetRefreshWorker::class.java)
      .setConstraints(networkConstraints)
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
      .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
      IMMEDIATE_WORK_NAME,
      ExistingWorkPolicy.KEEP,
      request,
    )
  }

  fun cancel(context: Context) {
    val workManager = WorkManager.getInstance(context)
    workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
    workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
  }
}
