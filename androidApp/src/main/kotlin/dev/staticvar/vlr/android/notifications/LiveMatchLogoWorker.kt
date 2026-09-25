/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.staticvar.vlr.android.VlrApplication
import kotlin.coroutines.cancellation.CancellationException

/** Fetches optional logos after FCM has posted the score and returned. */
internal class LiveMatchLogoWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
  override suspend fun doWork(): Result {
    val matchId = inputData.getString(MatchIdKey) ?: return Result.failure()
    val generation = inputData.getString(GenerationKey) ?: return Result.failure()
    return try {
      (applicationContext as VlrApplication).liveMatchNotifications.refreshLogos(matchId, generation)
      Result.success()
    } catch (error: CancellationException) {
      throw error
    } catch (_: Exception) {
      Result.failure()
    }
  }

  /** Replaces obsolete image work; a later score update can retry an unavailable image. */
  internal companion object {
    private const val MatchIdKey = "match_id"
    private const val GenerationKey = "generation"

    fun enqueue(context: Context, matchId: String, generation: String) {
      val request = OneTimeWorkRequestBuilder<LiveMatchLogoWorker>()
        .setInputData(workDataOf(MatchIdKey to matchId, GenerationKey to generation))
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
      WorkManager.getInstance(context).enqueueUniqueWork("live-match-logo-$matchId", ExistingWorkPolicy.REPLACE, request)
    }
  }
}
