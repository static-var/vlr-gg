/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.LocaleList
import android.text.format.DateFormat
import android.util.AtomicFile
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.GlanceState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.await
import androidx.work.impl.WorkManagerImpl
import androidx.work.testing.WorkManagerTestInitHelper
import dev.staticvar.vlr.android.widget.WidgetSnapshotStore
import dev.staticvar.vlr.shared.widget.UpcomingMatchesSnapshot
import dev.staticvar.vlr.shared.widget.WidgetFavoriteTeam
import dev.staticvar.vlr.shared.widget.WidgetTheme
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetEngine
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetFrame
import dev.staticvar.vlr.shared.widget.favorite.StoredWidgetRecord
import dev.staticvar.vlr.shared.widget.favorite.TeamDisplayBody
import dev.staticvar.vlr.shared.widget.favorite.TeamHeroText
import dev.staticvar.vlr.shared.widget.favorite.WidgetKey
import dev.staticvar.vlr.shared.widget.favorite.WidgetKind
import dev.staticvar.vlr.shared.widget.favorite.WidgetRefreshResult
import dev.staticvar.vlr.shared.widget.favorite.WidgetRequest
import dev.staticvar.vlr.shared.widget.favorite.WidgetSelection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.TimeZone
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.GlobalContext
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Instant
import android.content.res.Configuration as ResourceConfiguration
import java.util.TimeZone as JavaTimeZone

class FavoriteWidgetRuntimeTest {
  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private val context = object : ContextWrapper(instrumentation.targetContext) {
    override fun getFilesDir(): File = File(instrumentation.targetContext.filesDir, "favorite-widget-runtime-tests")
    override fun getFileStreamPath(name: String): File = File(filesDir, name)
  }
  private val component = ComponentName(context, AppWidgetProvider::class.java)
  private val host = AppWidgetHost(context, 7351)
  private val widgetIds = mutableListOf<Int>()
  private val engine: FavoriteWidgetEngine get() = GlobalContext.get().get()
  private val key = requireNotNull(WidgetKey.parse(WidgetKind.Team, "2", "runtime-client"))
  private val now = Instant.parse("2030-01-01T12:00:00Z")
  private val json = Json { encodeDefaults = true }
  private val directory get() = File(context.filesDir, "favorite-widgets/v1")
  private lateinit var bindings: FavoriteWidgetBindings
  private lateinit var records: FavoriteWidgetRecordStore
  private lateinit var work: WorkManager
  private val executor = Executors.newFixedThreadPool(2)
  private var originalSource: String? = null

  @Before
  fun setUp() = runBlocking {
    originalSource = WidgetSnapshotStore.read(context)
    assertTrue("Test record directory must be empty", directory.listFiles().orEmpty().isEmpty())
    assertTrue(
      "Remove favorite widgets from the home screen: the test WorkManager stops their Glance sessions",
      FavoriteWidgetBindings(instrumentation.targetContext).installed().isEmpty(),
    )
    context.packageManager.setComponentEnabledSetting(
      component,
      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
      PackageManager.DONT_KILL_APP,
    )
    withTimeout(5_000) {
      while (AppWidgetManager.getInstance(context).installedProviders.none { it.provider == component }) delay(20)
    }
    bindings = FavoriteWidgetBindings(
      context,
      listOf(FavoriteWidgetProvider(AppWidgetProvider::class.java, RuntimeTestWidget())),
    )
    records = FavoriteWidgetRecordStore(context)
    WorkManagerTestInitHelper.initializeTestWorkManager(
      context,
      Configuration.Builder()
        .setExecutor(executor)
        .setClock { now.toEpochMilliseconds() }
        .setWorkerFactory(object : WorkerFactory() {
          override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
          ): ListenableWorker = object : CoroutineWorker(appContext, workerParameters) {
            override suspend fun doWork(): Result = Result.success()
          }
        })
        .build(),
    )
    work = WorkManager.getInstance(context)
    writeSource(listOf(WidgetFavoriteTeam("2", "Sentinels")))
  }

  @After
  @SuppressLint("RestrictedApi")
  fun tearDown() = runBlocking {
    try {
      if (::work.isInitialized) {
        work.cancelAllWork().await()
        WorkManagerTestInitHelper.closeWorkDatabase()
      }
    } finally {
      WorkManagerImpl.setDelegate(null)
      executor.shutdownNow()
      val executorStopped = executor.awaitTermination(5, TimeUnit.SECONDS)
      for (id in widgetIds) {
        GlanceState.deleteStore(context, PreferencesGlanceStateDefinition, "appWidget-$id")
      }
      host.deleteHost()
      context.packageManager.setComponentEnabledSetting(
        component,
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
        PackageManager.DONT_KILL_APP,
      )
      directory.deleteRecursively()
      val source = originalSource
      if (source == null) {
        AtomicFile(context.getFileStreamPath("upcoming_matches_widget.json")).delete()
      } else {
        WidgetSnapshotStore.writeIfChanged(context, source)
      }
      assertTrue("WorkManager executor did not stop", executorStopped)
    }
  }

  @Test
  fun recordStoreRoundTripUsesOneSafeFileAndPreservesOpaqueJson() = runBlocking {
    val unsafeKey = requireNotNull(WidgetKey.parse(WidgetKind.Team, "../team/2?#", "client/../?#"))
    val record = noDataRecord(unsafeKey, 7)

    records.commit(unsafeKey, record)

    assertEquals(record, FavoriteWidgetRecordStore(context).read(unsafeKey))
    val file = directory.listFiles().orEmpty().single()
    assertTrue(file.name, file.name.matches(Regex("[0-9a-f]{64}\\.json")))
    assertEquals(directory.canonicalFile, file.canonicalFile.parentFile)
    val envelope = json.parseToJsonElement(file.readText()).jsonObject
    assertEquals(setOf("key", "record"), envelope.keys)
    assertEquals("../team/2?#", envelope.getValue("key").jsonObject.getValue("entityId").jsonPrimitive.content)
    assertEquals(record.json, envelope.getValue("record").jsonPrimitive.content)
  }

  @Test
  fun recordStoreRejectsAnEnvelopeCopiedToAnotherKeysFile() = runBlocking {
    val otherKey = requireNotNull(WidgetKey.parse(WidgetKind.Event, "2", "runtime-client"))
    val record = noDataRecord(key, 3)
    records.commit(key, record)
    File(directory, "${key.stableHash()}.json").copyTo(File(directory, "${otherKey.stableHash()}.json"))

    assertNull(records.read(otherKey))
    assertEquals(record, records.read(key))
  }

  @Test(timeout = 15000)
  fun concurrentRefreshesCommitOneRevisionAndNonDueRunsDoNotRefresh() = runBlocking {
    choose(installWidget(), "2")
    val entered = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()
    val committed = noDataRecord(key, 1)
    val refresh: suspend (WidgetRequest, StoredWidgetRecord?) -> WidgetRefreshResult = { request, previous ->
      if (!entered.complete(Unit)) throw AssertionError("A duplicate or non-due run attempted HTTP")
      assertEquals(WidgetSelection.Chosen(WidgetKind.Team, "2"), request.selection)
      assertEquals("runtime-client", request.source?.clientId)
      assertNull(previous)
      release.await()
      WidgetRefreshResult.Store(key, committed)
    }
    val firstRuntime = runtime(refresh)
    val secondRuntime = runtime(refresh)
    val first = async(Dispatchers.Default) { firstRuntime.fetch(key) }
    entered.await()
    val secondStarted = CompletableDeferred<Unit>()
    val second = async(Dispatchers.Default) {
      secondStarted.complete(Unit)
      secondRuntime.fetch(key)
    }
    secondStarted.await()
    assertFalse(first.isCompleted)
    release.complete(Unit)
    awaitAll(first, second)

    awaitAll(
      async(Dispatchers.Default) { firstRuntime.fetch(key) },
      async(Dispatchers.Default) { secondRuntime.fetch(key) },
    )

    assertEquals(committed, records.read(key))
    val file = directory.listFiles().orEmpty().single()
    assertEquals(
      "8a32187fadbcf40ae4b25d5a1ef907b890b3f1bda757c61089243864c202cfa5.json",
      file.name,
    )
    val stored = json.parseToJsonElement(file.readText()).jsonObject.getValue("record").jsonPrimitive.content
    assertEquals(committed.json, stored)
    assertEquals("1", json.parseToJsonElement(stored).jsonObject.getValue("revision").jsonPrimitive.content)
    val projection = engine.project(
      WidgetRequest(WidgetSelection.Chosen(WidgetKind.Team, "2"), source(), now, TimeZone.UTC),
      records.read(key),
      AndroidWidgetLocale(context),
    )
    assertEquals(1L, projection.recordRevision)
    assertEquals(Instant.parse("2030-01-01T12:15:00Z"), projection.schedule.fetchAt)
  }

  @Test(timeout = 15000)
  fun openFrameFlowUsesCurrentClockLocaleAndZoneAtRenderBoundaries() = runBlocking {
    choose(installWidget(), "2")
    val record = StoredWidgetRecord(
      """
      {"version":1,"key":${json.encodeToString(key)},"revision":1,
      "attemptedAt":"2030-01-01T12:00:00Z","fetchAfter":"2030-01-01T12:15:00Z","origin":"Fresh",
      "value":{"type":"team","fetchedAt":"2030-01-01T12:00:00Z","value":{
      "header":{"team":{"id":"2","name":"Sentinels"},"rank":null,"region":null,"regionRank":null},
      "form":[],"history":[],"state":{"type":"upcoming","fixture":{
      "id":"match","opponent":{"id":"3","name":"FNATIC"},"start":"2030-01-01T12:01:00Z",
      "event":"Champions","stage":"Final","format":null}}}}}
      """.trimIndent(),
    )
    records.commit(key, record)
    val sourceBefore = WidgetSnapshotStore.read(context)
    val currentTime = AtomicReference(now)
    val resources = AtomicReference(localizedResources(Locale.US))
    val frameContext = object : ContextWrapper(context) {
      override fun getResources(): Resources = resources.get()
    }
    val runtime = FavoriteWidgetRuntime(
      frameContext,
      engine,
      bindings,
      records,
      FavoriteWidgetScheduler(work, currentTime::get),
      now = currentTime::get,
      refresh = { _, _ -> throw AssertionError("Render must not attempt HTTP") },
    )
    val previousZone = JavaTimeZone.getDefault()
    val frames = Channel<PreparedWidget>(Channel.UNLIMITED)
    JavaTimeZone.setDefault(JavaTimeZone.getTimeZone("UTC"))
    val collection = launch {
      runtime.frames(WidgetSelection.Chosen(WidgetKind.Team, "2"))
        .distinctUntilChangedBy { it.frame }.collect(frames::send)
    }
    try {
      suspend fun hero(): TeamHeroText.Upcoming {
        val frame = withTimeout(5_000) { frames.receive() }.frame as FavoriteWidgetFrame.Team
        return (frame.medium.body as TeamDisplayBody.Visible).hero as TeamHeroText.Upcoming
      }
      val use24Hour = DateFormat.is24HourFormat(frameContext)
      val initial = hero()
      assertEquals("Plays FNATIC", initial.lead)
      assertEquals(if (use24Hour) "12:01" else "12:01 PM", initial.value)

      JavaTimeZone.setDefault(JavaTimeZone.getTimeZone("Asia/Kolkata"))
      runtime.render()
      assertEquals(if (use24Hour) "17:31" else "5:31 PM", hero().value)

      resources.set(localizedResources(Locale.FRANCE))
      runtime.render()
      assertEquals("Joue contre FNATIC", hero().lead)

      currentTime.set(Instant.parse("2030-01-01T12:01:00Z"))
      runtime.render()
      assertEquals("D’un instant à l’autre", hero().value)
      assertEquals(record, records.read(key))
      assertEquals(sourceBefore, WidgetSnapshotStore.read(context))
    } finally {
      collection.cancelAndJoin()
      JavaTimeZone.setDefault(previousZone)
    }
  }

  @Test(timeout = 15000)
  fun reconcileRemovesOrphansKeepsSharedRecordsAndCancelsWorkAfterLastDeletion() = runBlocking {
    val first = installWidget()
    val second = installWidget()
    choose(first, "2")
    choose(second, "2")
    val orphan = requireNotNull(WidgetKey.parse(WidgetKind.Team, "orphan", "runtime-client"))
    val shared = noDataRecord(key, 4)
    records.commit(key, shared)
    records.commit(orphan, noDataRecord(orphan, 2))
    val runtime = runtime()

    runtime.reconcile()

    assertNull(records.read(orphan))
    assertEquals(shared, records.read(key))
    assertEquals(listOf(first, second).sorted(), bindings.installed().map { it.appWidgetId }.sorted())
    assertEquals(1, active(FavoriteWidgetScheduler.fetchName(key)).size)
    assertEquals(1, active(FavoriteWidgetScheduler.ReconcileName).size)

    host.deleteAppWidgetId(first)
    runtime.reconcile()

    assertEquals(listOf(second), bindings.installed().map { it.appWidgetId })
    assertEquals(shared, records.read(key))
    assertEquals(1, active(FavoriteWidgetScheduler.fetchName(key)).size)
    work.enqueueUniqueWork(
      FavoriteWidgetScheduler.RenderName,
      ExistingWorkPolicy.REPLACE,
      OneTimeWorkRequestBuilder<FavoriteEntityRenderWorker>()
        .setInitialDelay(1, TimeUnit.DAYS).addTag(FavoriteWidgetScheduler.RuntimeTag).build(),
    ).await()
    assertEquals(1, active(FavoriteWidgetScheduler.RenderName).size)

    host.deleteAppWidgetId(second)
    runtime.reconcile()

    assertEquals(emptyList<FavoriteWidgetInstance>(), bindings.installed())
    assertEquals(emptyList<String>(), directory.listFiles().orEmpty().map { it.name })
    for (name in listOf(
      FavoriteWidgetScheduler.RenderName,
      FavoriteWidgetScheduler.ReconcileName,
      FavoriteWidgetScheduler.fetchName(key),
    )) {
      val infos = work.getWorkInfosForUniqueWork(name).get(5, TimeUnit.SECONDS)
      assertTrue("Missing work history for $name", infos.isNotEmpty())
      assertEquals("Work was not cancelled for $name", setOf(WorkInfo.State.CANCELLED), infos.map { it.state }.toSet())
    }
  }

  @Test(timeout = 15000)
  fun pickerSaveRechecksFreshMembershipAndStoresOnlyTheSelectedEntityId() = runBlocking {
    val id = installWidget()
    val runtime = runtime()
    val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(id)
    assertEquals(listOf(FavoriteChoice("2", "Sentinels")), requireNotNull(runtime.picker(id)).choices)

    writeSource(listOf(WidgetFavoriteTeam("3", "FNATIC")))

    assertEquals(ConfigurationResult.FavoritesChanged, runtime.configure(id, "2"))
    assertEquals(emptyMap<String, Any>(), state(glanceId))
    assertEquals(listOf(FavoriteChoice("3", "FNATIC")), requireNotNull(runtime.picker(id)).choices)

    assertEquals(ConfigurationResult.Saved, runtime.configure(id, "3"))
    assertEquals(mapOf("selected_entity_id" to "3"), state(glanceId))
    assertEquals(WidgetSelection.Chosen(WidgetKind.Team, "3"), requireNotNull(bindings.read(id)).selection)

    writeSource(emptyList())

    assertEquals(ConfigurationResult.FavoritesChanged, runtime.configure(id, "3"))
    assertEquals(mapOf("selected_entity_id" to "3"), state(glanceId))
  }

  @Test(timeout = 15000)
  fun restoreReadsEverySourceBeforeWritingOverlappingIds() = runBlocking {
    writeSource(listOf(WidgetFavoriteTeam("2", "Sentinels"), WidgetFavoriteTeam("3", "FNATIC")))
    val first = installWidget()
    val second = installWidget()
    val unchanged = installWidget()
    choose(first, "2")
    choose(second, "3")
    choose(unchanged, "3")
    val selections = bindings.restoredSelections()

    bindings.restore(intArrayOf(first), intArrayOf(second), selections)
    bindings.restore(intArrayOf(second, unchanged), intArrayOf(first, unchanged), selections)

    assertEquals(WidgetSelection.Chosen(WidgetKind.Team, "3"), requireNotNull(bindings.read(first)).selection)
    assertEquals(WidgetSelection.Chosen(WidgetKind.Team, "2"), requireNotNull(bindings.read(second)).selection)
    assertEquals(WidgetSelection.Chosen(WidgetKind.Team, "3"), requireNotNull(bindings.read(unchanged)).selection)
  }

  private fun installWidget(): Int {
    val id = host.allocateAppWidgetId()
    widgetIds += id
    instrumentation.uiAutomation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
    try {
      assertTrue(
        "Could not bind test widget",
        AppWidgetManager.getInstance(context)
          .bindAppWidgetIdIfAllowed(id, component),
      )
    } finally {
      instrumentation.uiAutomation.dropShellPermissionIdentity()
    }
    assertEquals(component, AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider)
    return id
  }

  private suspend fun choose(id: Int, entityId: String) {
    assertEquals(ConfigurationResult.Saved, bindings.choose(id, entityId, ::source))
  }

  private fun runtime(
    refresh: suspend (WidgetRequest, StoredWidgetRecord?) -> WidgetRefreshResult = { _, _ ->
      throw AssertionError("This test must not attempt HTTP")
    },
  ) = FavoriteWidgetRuntime(
    context,
    engine,
    bindings,
    FavoriteWidgetRecordStore(context),
    FavoriteWidgetScheduler(work) { now },
    now = { now },
    refresh = refresh,
  )

  private fun source(): UpcomingMatchesSnapshot? = WidgetSnapshotStore.read(context)?.let(engine::decodeSource)

  private suspend fun writeSource(teams: List<WidgetFavoriteTeam>) {
    WidgetSnapshotStore.writeIfChanged(
      context,
      json.encodeToString(
        UpcomingMatchesSnapshot(
          savedAtEpochMillis = now.toEpochMilliseconds(),
          clientId = "runtime-client",
          hasFavorites = teams.isNotEmpty(),
          matches = emptyList(),
          theme = WidgetTheme(0, 0, 0, 0, 0, 0, false),
          favoriteTeams = teams,
        ),
      ),
    )
  }

  private suspend fun state(id: GlanceId): Map<String, Any> =
    getAppWidgetState(context, PreferencesGlanceStateDefinition, id).asMap().mapKeys { it.key.name }

  private fun localizedResources(locale: Locale): Resources = instrumentation.targetContext.createConfigurationContext(
    ResourceConfiguration(instrumentation.targetContext.resources.configuration).apply {
      setLocales(LocaleList(locale))
    },
  ).resources

  private fun active(name: String): List<WorkInfo> =
    work.getWorkInfosForUniqueWork(name).get(5, TimeUnit.SECONDS).filterNot { it.state.isFinished }

  private fun noDataRecord(key: WidgetKey, revision: Long) = StoredWidgetRecord(
    """
    {"version":1,"key":${json.encodeToString(key)},"revision":$revision,
    "attemptedAt":"2030-01-01T12:00:00Z","fetchAfter":"2030-01-01T12:15:00Z",
    "origin":"CachedFallback","value":{"type":"no-data"}}
    """.trimIndent(),
  )
}

private class RuntimeTestWidget : FavoriteEntityWidget(WidgetKind.Team) {
  @Composable
  override fun Content(frame: FavoriteWidgetFrame, logos: WidgetBitmaps) {
    Text(frame.toString())
  }
}
