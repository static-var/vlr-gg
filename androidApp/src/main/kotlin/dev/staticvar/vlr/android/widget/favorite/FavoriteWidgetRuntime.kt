/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.WorkManager
import dev.staticvar.vlr.android.widget.WidgetSnapshotStore
import dev.staticvar.vlr.shared.widget.UpcomingMatchesSnapshot
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetEngine
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetFrame
import dev.staticvar.vlr.shared.widget.favorite.StoredWidgetRecord
import dev.staticvar.vlr.shared.widget.favorite.WidgetKey
import dev.staticvar.vlr.shared.widget.favorite.WidgetProjection
import dev.staticvar.vlr.shared.widget.favorite.WidgetRefreshResult
import dev.staticvar.vlr.shared.widget.favorite.WidgetRequest
import dev.staticvar.vlr.shared.widget.favorite.WidgetSelection
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import org.koin.mp.KoinPlatform
import kotlin.time.Clock
import kotlin.time.Instant

internal data class PreparedWidget(val frame: FavoriteWidgetFrame, val logos: WidgetBitmaps)

internal class FavoriteWidgetRuntime(
  private val context: Context,
  private val engine: FavoriteWidgetEngine,
  private val bindings: FavoriteWidgetBindings = FavoriteWidgetBindings(context),
  private val records: FavoriteWidgetRecordStore = FavoriteWidgetRecordStore(context),
  private val scheduler: FavoriteWidgetScheduler = FavoriteWidgetScheduler(WorkManager.getInstance(context)),
  private val now: () -> Instant = Clock.System::now,
  private val refresh: suspend (WidgetRequest, StoredWidgetRecord?) -> WidgetRefreshResult = engine::refresh,
) {
  private val logos = FavoriteWidgetLogoLoader(context)

  private val prepared = ConcurrentHashMap<WidgetSelection, Pair<Long, PreparedWidget>>()

  @OptIn(ExperimentalCoroutinesApi::class)
  fun frames(selection: WidgetSelection): Flow<PreparedWidget> = invalidations.mapLatest { version ->
    prepared[selection]?.takeIf { it.first == version }?.second ?: run {
      val projection = project(request(selection, source()))
      PreparedWidget(projection.frame, logos.load(projection.frame)).also { prepared[selection] = version to it }
    }
  }.flowOn(Dispatchers.IO)

  suspend fun picker(appWidgetId: Int): FavoritePicker? = withContext(Dispatchers.IO) {
    bindings.picker(appWidgetId, source())
  }

  suspend fun configure(appWidgetId: Int, entityId: String): ConfigurationResult = withContext(Dispatchers.IO) {
    val result = FavoriteWidgetScheduler.serialized { bindings.choose(appWidgetId, entityId, ::source) }
    if (result == ConfigurationResult.Saved) {
      invalidate()
      bindings.read(appWidgetId)?.let { it.provider.widget.update(context, it.glanceId) }
      updateAll()
      reconcile()
    }
    result
  }

  suspend fun changed() {
    invalidate()
    updateAll()
    reconcile()
  }

  suspend fun render() = withContext(Dispatchers.IO) {
    invalidate()
    updateAll()
    scheduler.reconcile { projections(bindings.installed()) }
  }

  suspend fun reconcile() = withContext(Dispatchers.IO) {
    scheduler.reconcile {
      val installed = bindings.installed()
      prepared.keys.retainAll(installed.mapTo(HashSet()) { it.selection })
      val projections = projections(installed)
      records.retain(projections.mapNotNull { it.key }.toSet())
      projections
    }
  }

  suspend fun fetch(key: WidgetKey) = withContext(Dispatchers.IO) {
    records.withKey(key) {
      val request = matchingRequest(key) ?: return@withKey
      val stored = records.read(key)
      val due = engine.project(request, stored, AndroidWidgetLocale(context)).schedule.fetchAt
      if (due == null || due > request.now) return@withKey
      when (val result = refresh(request, stored)) {
        WidgetRefreshResult.Refused -> Unit

        is WidgetRefreshResult.Store -> FavoriteWidgetScheduler.serialized {
          currentCoroutineContext().ensureActive()
          if (result.key == key && matchingRequest(key) != null) {
            records.commit(key, result.record)
            invalidate()
          }
        }
      }
    }
    updateAll()
    reconcile()
  }

  private suspend fun matchingRequest(key: WidgetKey): WidgetRequest? {
    val source = source()
    for (instance in bindings.installed()) {
      val request = request(instance.selection, source)
      if (engine.key(request) == key) return request
    }
    return null
  }

  private suspend fun projections(installed: List<FavoriteWidgetInstance>): List<WidgetProjection> {
    val source = source()
    return installed.map { project(request(it.selection, source)) }
  }

  private suspend fun project(request: WidgetRequest): WidgetProjection =
    engine.project(request, engine.key(request)?.let { records.read(it) }, AndroidWidgetLocale(context))

  private fun request(selection: WidgetSelection, source: UpcomingMatchesSnapshot?) =
    WidgetRequest(selection, source, now(), TimeZone.currentSystemDefault())

  private fun source(): UpcomingMatchesSnapshot? = WidgetSnapshotStore.read(context)?.let(engine::decodeSource)

  private suspend fun updateAll() {
    for (provider in FavoriteEntityWidgets.providers) provider.widget.updateAll(context)
  }

  companion object {
    val invalidations = MutableStateFlow(0L)

    fun invalidate() {
      invalidations.update { it + 1 }
    }

    fun get(): FavoriteWidgetRuntime = KoinPlatform.getKoin().get()
  }
}
