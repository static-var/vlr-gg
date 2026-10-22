/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.annotation.SuppressLint
import android.content.Context
import androidx.concurrent.futures.await
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.await
import androidx.work.impl.WorkManagerImpl
import androidx.work.testing.WorkManagerTestInitHelper
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetFrame
import dev.staticvar.vlr.shared.widget.favorite.PlaceholderText
import dev.staticvar.vlr.shared.widget.favorite.WidgetKey
import dev.staticvar.vlr.shared.widget.favorite.WidgetKind
import dev.staticvar.vlr.shared.widget.favorite.WidgetProjection
import dev.staticvar.vlr.shared.widget.favorite.WidgetSchedule
import dev.staticvar.vlr.shared.widget.favorite.WidgetTap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class FavoriteWidgetSchedulerTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private val now = Instant.parse("2026-09-17T12:00:00Z")
  private val key = requireNotNull(WidgetKey.parse(WidgetKind.Team, "team", "client"))
  private val executor = Executors.newFixedThreadPool(2)
  private val started = CompletableDeferred<UUID>()
  private val release = CompletableDeferred<Unit>()
  private lateinit var work: WorkManager
  private lateinit var scheduler: FavoriteWidgetScheduler

  @Before
  fun initializeWorkManager() = runBlocking {
    val factory = object : WorkerFactory() {
      override fun createWorker(
        context: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
      ): ListenableWorker = object : CoroutineWorker(context, workerParameters) {
        override suspend fun doWork(): Result {
          if (workerClassName == FavoriteEntityFetchWorker::class.java.name) {
            started.complete(id)
            release.await()
          }
          return Result.success()
        }
      }
    }
    WorkManagerTestInitHelper.initializeTestWorkManager(
      context,
      Configuration.Builder().setExecutor(executor).setWorkerFactory(factory)
        .setClock { now.toEpochMilliseconds() }.build(),
    )
    work = WorkManager.getInstance(context)
    scheduler = FavoriteWidgetScheduler(work) { now }
  }

  @After
  @SuppressLint("RestrictedApi")
  fun closeWorkManager() = runBlocking {
    release.complete(Unit)
    try {
      work.cancelAllWork().await()
      WorkManagerTestInitHelper.closeWorkDatabase()
    } finally {
      WorkManagerImpl.setDelegate(null)
      executor.shutdownNow()
      assertTrue("WorkManager executor did not stop", executor.awaitTermination(5, TimeUnit.SECONDS))
    }
  }

  @Test
  fun renderUsesMinimumBoundaryAndReplacesItsSingleUnconstrainedRequest() = runBlocking {
    scheduler.reconcile { listOf(projection(render = 120), projection(render = 30)) }
    val first = work.getWorkInfosForUniqueWork(FavoriteWidgetScheduler.RenderName).await().single()
    assertEquals(30_000L, first.initialDelayMillis)
    assertEquals(now.toEpochMilliseconds() + 30_000L, first.nextScheduleTimeMillis)
    assertEquals(NetworkType.NOT_REQUIRED, first.constraints.requiredNetworkType)

    scheduler.reconcile { listOf(projection(render = 90), projection(render = 45)) }
    val replacement = work.getWorkInfosForUniqueWork(FavoriteWidgetScheduler.RenderName).await().single()
    assertNotEquals(first.id, replacement.id)
    assertEquals(WorkInfo.State.ENQUEUED, replacement.state)
    assertEquals(45_000L, replacement.initialDelayMillis)
    assertEquals(NetworkType.NOT_REQUIRED, replacement.constraints.requiredNetworkType)
  }

  @Test
  fun fetchChangesQueuedDeadlineWithoutCancellingTheRunningFetchAndKeepsOneSuccessor() = runBlocking {
    val name = FavoriteWidgetScheduler.fetchName(key)
    scheduler.reconcile { listOf(projection(fetch = 60)) }
    val first = work.getWorkInfosForUniqueWork(name).await().single()
    assertEquals(NetworkType.CONNECTED, first.constraints.requiredNetworkType)
    assertEquals(60_000L, first.initialDelayMillis)

    scheduler.reconcile { listOf(projection(fetch = 120)) }
    val updated = work.getWorkInfosForUniqueWork(name).await().single()
    assertEquals(first.id, updated.id)
    assertEquals(now.toEpochMilliseconds() + 120_000L, updated.nextScheduleTimeMillis)
    assertEquals(1, updated.generation)
    scheduler.reconcile { listOf(projection(fetch = 120)) }
    assertEquals(1, work.getWorkInfosForUniqueWork(name).await().single().generation)

    val driver = requireNotNull(WorkManagerTestInitHelper.getTestDriver(context))
    driver.setInitialDelayMet(first.id)
    driver.setAllConstraintsMet(first.id)
    assertEquals(first.id, withTimeout(5_000) { started.await() })
    scheduler.reconcile { listOf(projection(fetch = 180)) }
    val running = requireNotNull(work.getWorkInfoById(first.id).await())
    assertEquals(WorkInfo.State.RUNNING, running.state)
    assertEquals(WorkInfo.STOP_REASON_NOT_STOPPED, running.stopReason)
    val successor = work.getWorkInfosForUniqueWork(name).await().single { it.state == WorkInfo.State.BLOCKED }
    assertEquals(180_000L, successor.initialDelayMillis)

    scheduler.reconcile { listOf(projection(fetch = 240)) }
    val next = work.getWorkInfosForUniqueWork(name).await().single { it.state == WorkInfo.State.BLOCKED }
    assertEquals(successor.id, next.id)
    assertEquals(240_000L, next.initialDelayMillis)
    release.complete(Unit)
    val completed = withTimeout(5_000) { work.getWorkInfoByIdFlow(first.id).first { it?.state?.isFinished == true } }
    assertEquals(WorkInfo.State.SUCCEEDED, completed?.state)
    val pending = withTimeout(5_000) {
      work.getWorkInfoByIdFlow(successor.id).first { it?.state == WorkInfo.State.ENQUEUED }
    }
    assertTrue(requireNotNull(pending).nextScheduleTimeMillis >= now.toEpochMilliseconds() + 240_000L)
    assertFalse(work.getWorkInfosForUniqueWork(name).await().any { it.state == WorkInfo.State.CANCELLED })
  }

  private fun projection(render: Int? = null, fetch: Int? = null) = WidgetProjection(
    FavoriteWidgetFrame.Placeholder(key, PlaceholderText("Team", "Loading", ""), WidgetTap.App),
    key,
    1,
    WidgetSchedule(render?.let { now + it.seconds }, fetch?.let { now + it.seconds }),
  )
}
