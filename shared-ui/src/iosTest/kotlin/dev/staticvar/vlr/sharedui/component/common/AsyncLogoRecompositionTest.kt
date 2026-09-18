/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.clearSkikoComposeImplementation
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import androidx.compose.ui.unit.IntSize
import coil3.Canvas
import coil3.Image
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class, InternalComposeUiApi::class)
class AsyncLogoRecompositionTest {
  @BeforeTest
  fun setUp() = registerSkikoComposeImplementation()

  @AfterTest
  fun tearDown() = clearSkikoComposeImplementation()

  @Test
  fun tenPreparedOutlinesRecomposeOnlyTheirOwningIcons() = runTest {
    val requests = List(10, ::request)
    val outlines = requests.associateWith { outlined() }

    val baselineInputs = emptyInputs(requests.size)
    val baseline = LogoComposition(this, baselineInputs, Sync(outlines::getValue))
    try {
      baseline.recompose()
      val initial = baseline.counts.snapshot()
      publish(baselineInputs, requests)
      baseline.recompose()
      val imageArrivals = baseline.counts.snapshot() - initial

      assertLocalIconCompositions(imageArrivals, expectedPerIcon = 1)
      assertEquals(10, baseline.syncPreparations)

      val provider = ControllableProvider()
      val asyncInputs = emptyInputs(requests.size)
      val async = LogoComposition(this, asyncInputs, Async(provider))
      try {
        async.recompose()
        val asyncInitial = async.counts.snapshot()
        publish(asyncInputs, requests)
        async.recompose()
        val afterImageArrivals = async.counts.snapshot()

        assertEquals(imageArrivals, afterImageArrivals - asyncInitial)
        assertEquals(requests, provider.pendingRequests)

        requests.forEach { request ->
          provider.complete(request, outlines.getValue(request))
          async.recompose()
        }
        val completions = async.counts.snapshot() - afterImageArrivals

        assertLocalIconCompositions(completions, expectedPerIcon = 1)
        outlines.values.forEachIndexed { index, treatment ->
          assertSame(treatment, async.rendered[index])
        }
        println(
          "LOGO_RECOMPOSITION workload=10 image_arrivals " +
            "baseline=${imageArrivals.report()} async_completion=${completions.report()}",
        )
      } finally {
        async.dispose()
      }
    } finally {
      baseline.dispose()
    }
  }

  @Test
  fun untreatedCompletionsDoNotRecompose() = runTest {
    val requests = List(10, ::request)
    val inputs = emptyInputs(requests.size)
    val provider = ControllableProvider()
    val composition = LogoComposition(this, inputs, Async(provider))
    try {
      composition.recompose()
      publish(inputs, requests)
      composition.recompose()
      val beforeCompletions = composition.counts.snapshot()

      requests.forEach { request ->
        provider.complete(request, LogoTreatment.Original)
        composition.recompose()
      }

      assertEquals(CountsSnapshot.zero(requests.size), composition.counts.snapshot() - beforeCompletions)
      composition.rendered.forEach(::assertNull)
      println("LOGO_RECOMPOSITION workload=10 untreated_completion=${CountsSnapshot.zero(10).report()}")
    } finally {
      composition.dispose()
    }
  }

  @Test
  fun changingRequestsDoesNotReuseACachedOutline() = runTest {
    val first = request(1)
    val second = request(2)
    val cachedOutline = outlined()
    val provider = ControllableProvider(
      cached = mapOf(first to cachedOutline, second to LogoTreatment.Original),
    )
    val inputs = listOf(mutableStateOf<LogoTreatmentRequest?>(first))
    val composition = LogoComposition(this, inputs, Async(provider))
    try {
      composition.recompose()
      assertSame(cachedOutline, composition.rendered.single())

      inputs.single().value = second
      composition.recompose()

      assertNull(composition.rendered.single())
      assertEquals(emptyList(), provider.pendingRequests)
    } finally {
      composition.dispose()
    }
  }

  @Test
  fun aLatePreviousRequestCannotReplaceTheCurrentTreatment() = runTest {
    val first = request(1)
    val second = request(2)
    val firstOutline = outlined()
    val secondOutline = outlined()
    val provider = ControllableProvider()
    val inputs = listOf(mutableStateOf<LogoTreatmentRequest?>(first))
    val composition = LogoComposition(this, inputs, Async(provider))
    try {
      composition.recompose()
      assertEquals(listOf(first), provider.pendingRequests)

      inputs.single().value = second
      composition.recompose()
      assertEquals(listOf(first, second), provider.pendingRequests)

      provider.complete(second, secondOutline)
      composition.recompose()
      assertSame(secondOutline, composition.rendered.single())
      val afterCurrentResult = composition.counts.snapshot()

      provider.complete(first, firstOutline)
      composition.recompose()

      assertSame(secondOutline, composition.rendered.single())
      assertEquals(afterCurrentResult, composition.counts.snapshot())
    } finally {
      composition.dispose()
    }
  }

  private fun TestScope.publish(
    inputs: List<MutableState<LogoTreatmentRequest?>>,
    requests: List<LogoTreatmentRequest>,
  ) {
    Snapshot.withMutableSnapshot {
      inputs.zip(requests).forEach { (input, request) -> input.value = request }
    }
    runCurrent()
  }

  private fun assertLocalIconCompositions(actual: CountsSnapshot, expectedPerIcon: Int) {
    assertEquals(0, actual.parent)
    assertEquals(0, actual.list)
    assertEquals(List(actual.rows.size) { 0 }, actual.rows)
    assertEquals(List(actual.icons.size) { expectedPerIcon }, actual.icons)
  }

  private fun emptyInputs(count: Int): List<MutableState<LogoTreatmentRequest?>> = List(count) { mutableStateOf(null) }

  private fun request(id: Int): LogoTreatmentRequest = LogoTreatmentRequest(
    source = LogoSource(FakeImage(id)),
    pixelSize = IntSize(96, 96),
    radiusPx = 3f,
    background = Color.White,
  )

  private fun outlined(): LogoTreatment.Outlined = LogoTreatment.Outlined(ImageBitmap(102, 102), padding = 3)
}

private sealed interface TreatmentMode

private class Sync(val prepare: (LogoTreatmentRequest) -> LogoTreatment) : TreatmentMode

private class Async(val provider: LogoTreatmentProvider) : TreatmentMode

private class LogoComposition(
  private val scope: TestScope,
  private val inputs: List<State<LogoTreatmentRequest?>>,
  private val mode: TreatmentMode,
) {
  val counts = RecompositionCounts(inputs.size)
  val rendered = MutableList<LogoTreatment?>(inputs.size) { null }
  var syncPreparations = 0
    private set

  private val frameClock = object : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
      onFrame(scope.testScheduler.currentTime * 1_000_000)
  }
  private val recomposer = Recomposer(scope.backgroundScope.coroutineContext + frameClock)
  private val composition = Composition(UnitApplier(), recomposer)

  init {
    scope.backgroundScope.launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }
    composition.setContent {
      MeasuredLogoParent(
        inputs = inputs,
        mode = mode,
        counts = counts,
        rendered = rendered,
        onSyncPreparation = { syncPreparations++ },
      )
    }
  }

  fun recompose() {
    Snapshot.sendApplyNotifications()
    scope.runCurrent()
    Snapshot.sendApplyNotifications()
    scope.runCurrent()
  }

  fun dispose() {
    composition.dispose()
    recomposer.cancel()
  }
}

@Composable
private fun MeasuredLogoParent(
  inputs: List<State<LogoTreatmentRequest?>>,
  mode: TreatmentMode,
  counts: RecompositionCounts,
  rendered: MutableList<LogoTreatment?>,
  onSyncPreparation: () -> Unit,
) {
  SideEffect { counts.parent++ }
  MeasuredLogoList(inputs, mode, counts, rendered, onSyncPreparation)
}

@Composable
private fun MeasuredLogoList(
  inputs: List<State<LogoTreatmentRequest?>>,
  mode: TreatmentMode,
  counts: RecompositionCounts,
  rendered: MutableList<LogoTreatment?>,
  onSyncPreparation: () -> Unit,
) {
  SideEffect { counts.list++ }
  inputs.forEachIndexed { index, input ->
    MeasuredLogoRow(index, input, mode, counts, rendered, onSyncPreparation)
  }
}

@Composable
private fun MeasuredLogoRow(
  index: Int,
  input: State<LogoTreatmentRequest?>,
  mode: TreatmentMode,
  counts: RecompositionCounts,
  rendered: MutableList<LogoTreatment?>,
  onSyncPreparation: () -> Unit,
) {
  SideEffect { counts.rows[index]++ }
  when (mode) {
    is Sync -> MeasuredSyncLogo(index, input, mode.prepare, counts, rendered, onSyncPreparation)
    is Async -> MeasuredAsyncLogo(index, input, mode.provider, counts, rendered)
  }
}

@Composable
private fun MeasuredSyncLogo(
  index: Int,
  input: State<LogoTreatmentRequest?>,
  prepare: (LogoTreatmentRequest) -> LogoTreatment,
  counts: RecompositionCounts,
  rendered: MutableList<LogoTreatment?>,
  onPreparation: () -> Unit,
) {
  val request = input.value
  val treatment = remember(request) {
    request?.let {
      onPreparation()
      prepare(it)
    }
  }
  SideEffect {
    counts.icons[index]++
    rendered[index] = treatment
  }
}

@Composable
private fun MeasuredAsyncLogo(
  index: Int,
  input: State<LogoTreatmentRequest?>,
  provider: LogoTreatmentProvider,
  counts: RecompositionCounts,
  rendered: MutableList<LogoTreatment?>,
) {
  val treatment = rememberLogoTreatment(input.value, provider).value
  SideEffect {
    counts.icons[index]++
    rendered[index] = treatment
  }
}

private class RecompositionCounts(itemCount: Int) {
  var parent = 0
  var list = 0
  val rows = IntArray(itemCount)
  val icons = IntArray(itemCount)

  fun snapshot(): CountsSnapshot = CountsSnapshot(parent, list, rows.toList(), icons.toList())
}

private data class CountsSnapshot(val parent: Int, val list: Int, val rows: List<Int>, val icons: List<Int>) {
  operator fun minus(other: CountsSnapshot): CountsSnapshot = CountsSnapshot(
    parent = parent - other.parent,
    list = list - other.list,
    rows = rows.zip(other.rows, Int::minus),
    icons = icons.zip(other.icons, Int::minus),
  )

  fun report(): String = "parent=$parent,list=$list,rows=${rows.sum()},icons=${icons.sum()}"

  companion object {
    fun zero(itemCount: Int): CountsSnapshot = CountsSnapshot(0, 0, List(itemCount) { 0 }, List(itemCount) { 0 })
  }
}

private class ControllableProvider(private val cached: Map<LogoTreatmentRequest, LogoTreatment> = emptyMap()) :
  LogoTreatmentProvider {
  private val continuations = linkedMapOf<LogoTreatmentRequest, Continuation<LogoTreatment>>()
  val pendingRequests: List<LogoTreatmentRequest> get() = continuations.keys.toList()

  override fun cached(request: LogoTreatmentRequest): LogoTreatment? = cached[request]

  override suspend fun prepare(request: LogoTreatmentRequest): LogoTreatment = suspendCoroutine { continuation ->
    continuations[request] = continuation
  }

  fun complete(request: LogoTreatmentRequest, treatment: LogoTreatment) {
    checkNotNull(continuations.remove(request)) { "No pending treatment for $request" }.resume(treatment)
  }
}

private class FakeImage(private val id: Int) : Image {
  override val size: Long = 96L * 96 * 4
  override val width: Int = 96
  override val height: Int = 96
  override val shareable: Boolean = true
  override fun draw(canvas: Canvas) = Unit
  override fun toString(): String = "FakeImage($id)"
}

private class UnitApplier : AbstractApplier<Unit>(Unit) {
  override fun insertTopDown(index: Int, instance: Unit) = Unit
  override fun insertBottomUp(index: Int, instance: Unit) = Unit
  override fun remove(index: Int, count: Int) = Unit
  override fun move(from: Int, to: Int, count: Int) = Unit
  override fun onClear() = Unit
}
