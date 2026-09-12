/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.workers

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.staticvar.vlr.android.widget.LegacyMatchSnapshotStore
import dev.staticvar.vlr.android.widget.LegacyWidgetRefreshScheduler
import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import dev.staticvar.vlr.domain.repository.MatchRepository
import dev.staticvar.vlr.widget.ScoreWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import kotlin.coroutines.cancellation.CancellationException

class WidgetUpdateWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
  override suspend fun doWork(): Result = refreshLock.withLock {
    if (!LegacyWidgetRefreshScheduler.hasInstalledWidgets(applicationContext)) return@withLock Result.success()
    try {
      val koin = KoinPlatform.getKoin()
      val repository = koin.get<MatchRepository>()
      repository.refreshMatches().getOrThrow()
      val matches = repository.getMatches().first()
      withContext(Dispatchers.IO) {
        LegacyMatchSnapshotStore.write(applicationContext, matches, koin.get<SpoilerPreferencesRepository>().enabled.value)
      }
      ScoreWidget().updateAll(applicationContext)
      Result.success()
    } catch (error: CancellationException) {
      throw error
    } catch (_: Exception) {
      Result.retry()
    }
  }

  private companion object {
    val refreshLock = Mutex()
  }
}
