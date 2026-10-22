/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import androidx.concurrent.futures.await
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import dev.staticvar.vlr.shared.widget.favorite.WidgetKey
import dev.staticvar.vlr.shared.widget.favorite.WidgetProjection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Instant

internal class FavoriteWidgetScheduler(
  private val work: WorkManager,
  private val now: () -> Instant = Clock.System::now,
) {
  suspend fun reconcile(current: suspend () -> List<WidgetProjection>) = serialized {
    val projections = current()
    if (projections.isEmpty()) {
      work.cancelAllWorkByTag(RuntimeTag).await()
      return@serialized
    }
    work.enqueueUniquePeriodicWork(
      ReconcileName,
      ExistingPeriodicWorkPolicy.KEEP,
      PeriodicWorkRequestBuilder<FavoriteEntityReconcileWorker>(30, TimeUnit.MINUTES).addTag(RuntimeTag).build(),
    ).await()
    val fetches = projections.mapNotNull { projection ->
      val key = projection.key ?: return@mapNotNull null
      projection.schedule.fetchAt?.let { key to it }
    }.groupBy({ it.first }, { it.second })
    for ((key, deadlines) in fetches) armFetch(key, deadlines.min())
    val renderAt = projections.mapNotNull { it.schedule.renderAt }.minOrNull()
    if (renderAt == null) {
      work.cancelUniqueWork(RenderName).await()
    } else {
      work.enqueueUniqueWork(
        RenderName,
        ExistingWorkPolicy.REPLACE,
        OneTimeWorkRequestBuilder<FavoriteEntityRenderWorker>()
          .setInitialDelay(delay(renderAt), TimeUnit.MILLISECONDS)
          .addTag(RuntimeTag)
          .build(),
      ).await()
    }
  }

  private suspend fun armFetch(key: WidgetKey, at: Instant) {
    val name = fetchName(key)
    while (true) {
      val live = work.getWorkInfosForUniqueWork(name).await().filterNot { it.state.isFinished }
      val blocked = live.firstOrNull { it.state == WorkInfo.State.BLOCKED }
      val pending = blocked ?: live.firstOrNull { it.state == WorkInfo.State.ENQUEUED }
      if (pending == null) {
        work.enqueueUniqueWork(
          name,
          if (live.isEmpty()) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.APPEND_OR_REPLACE,
          fetchRequest(key, delay(at)),
        ).await()
        return
      }
      if (pending.runAttemptCount > 0) return
      val enqueuedAt = if (blocked == null) {
        pending.nextScheduleTimeMillis - pending.initialDelayMillis
      } else {
        now().toEpochMilliseconds()
      }
      val initialDelay = (at.toEpochMilliseconds() - enqueuedAt).coerceAtLeast(0)
      if (initialDelay == pending.initialDelayMillis) return
      // updateWork also handles an ENQUEUED -> RUNNING race without cancelling HTTP.
      when (work.updateWork(fetchRequest(key, initialDelay, pending.id)).await()) {
        WorkManager.UpdateResult.APPLIED_IMMEDIATELY -> return
        WorkManager.UpdateResult.APPLIED_FOR_NEXT_RUN, WorkManager.UpdateResult.NOT_APPLIED -> continue
      }
    }
  }

  private fun fetchRequest(key: WidgetKey, delay: Long, id: UUID = UUID.randomUUID()): OneTimeWorkRequest =
    OneTimeWorkRequestBuilder<FavoriteEntityFetchWorker>()
      .setId(id)
      .setInputData(workDataOf("kind" to key.kind.name, "entity" to key.entityId, "client" to key.clientId))
      .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
      .setInitialDelay(delay, TimeUnit.MILLISECONDS)
      .addTag(FetchTag)
      .addTag(RuntimeTag)
      .build()

  private fun delay(at: Instant) = (at - now()).inWholeMilliseconds.coerceAtLeast(0)

  companion object {
    const val RenderName = "favorite-entity-widgets-render"
    const val ReconcileName = "favorite-entity-widgets-reconcile"
    const val FetchTag = "favorite-entity-widget-fetch"
    const val RuntimeTag = "favorite-entity-widgets"
    private val armMutex = Mutex()

    fun fetchName(key: WidgetKey): String = "favorite-entity-widget-fetch-${key.stableHash()}"

    suspend fun <T> serialized(block: suspend () -> T): T = armMutex.withLock { block() }
  }
}
