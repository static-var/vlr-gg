/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import coil3.Canvas
import coil3.Image
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class LogoTreatmentProcessorTest {
  @Test
  fun identicalRequestsShareOneTreatmentJob() = runTest {
    var analysisCalls = 0
    var renderCalls = 0
    val processor = LogoTreatmentProcessor(
      dispatcher = StandardTestDispatcher(testScheduler),
      analyze = {
        analysisCalls++
        whiteEdges
      },
      render = { _, _, _, _ ->
        renderCalls++
        null
      },
    )
    val image = FakeImage()
    val firstRequest = request(image)
    val equalRequest = request(image)

    try {
      val first = async(start = CoroutineStart.UNDISPATCHED) { processor.prepare(firstRequest) }
      val second = async(start = CoroutineStart.UNDISPATCHED) { processor.prepare(equalRequest) }
      advanceUntilIdle()

      assertSame(LogoTreatment.Original, first.await())
      assertSame(LogoTreatment.Original, second.await())
      assertEquals(1, analysisCalls)
      assertEquals(1, renderCalls)
    } finally {
      processor.close()
    }
  }

  @Test
  fun analysisIsReusedAcrossThemeAndSizeTreatments() = runTest {
    var analysisCalls = 0
    var renderCalls = 0
    val processor = LogoTreatmentProcessor(
      dispatcher = StandardTestDispatcher(testScheduler),
      analyze = {
        analysisCalls++
        whiteEdges
      },
      render = { _, _, _, _ ->
        renderCalls++
        null
      },
    )
    val image = FakeImage()
    val light = request(image, pixelSize = IntSize(32, 32), background = Color.White)
    val dark = request(image, pixelSize = IntSize(96, 96), background = Color.Black)

    try {
      val lightResult = async { processor.prepare(light) }
      advanceUntilIdle()
      val darkResult = async { processor.prepare(dark) }
      advanceUntilIdle()

      assertSame(LogoTreatment.Original, lightResult.await())
      assertSame(LogoTreatment.Original, darkResult.await())
      assertEquals(1, analysisCalls)
      assertEquals(1, renderCalls)
    } finally {
      processor.close()
    }
  }

  @Test
  fun treatmentCacheEvictsToItsByteBudgetAndRecomputesEvictedEntries() = runTest {
    var renderCalls = 0
    val processor = LogoTreatmentProcessor(
      dispatcher = StandardTestDispatcher(testScheduler),
      analyze = { whiteEdges },
      render = { _, _, _, _ ->
        renderCalls++
        null
      },
      cacheBytes = 257,
    )
    val first = request(FakeImage())
    val second = request(FakeImage())
    val third = request(FakeImage())

    try {
      for (request in listOf(first, second, third)) {
        val result = async { processor.prepare(request) }
        advanceUntilIdle()
        assertSame(LogoTreatment.Original, result.await())
      }

      assertEquals(3, renderCalls)
      assertNull(processor.cached(first))
      val repeatedFirst = async { processor.prepare(first) }
      advanceUntilIdle()
      assertSame(LogoTreatment.Original, repeatedFirst.await())
      assertEquals(4, renderCalls)
      assertNull(processor.cached(second))
      assertSame(LogoTreatment.Original, processor.cached(third))
      assertSame(LogoTreatment.Original, processor.cached(first))
    } finally {
      processor.close()
    }
  }

  @Test
  fun cancellingOneReaderDoesNotCancelTheSharedWorker() = runTest {
    val analyzer = BlockingAnalyzer()
    var renderCalls = 0
    val processor = LogoTreatmentProcessor(
      dispatcher = Dispatchers.Default,
      analyze = analyzer::analyze,
      render = { _, _, _, _ ->
        renderCalls++
        null
      },
    )
    val request = request(FakeImage())
    val cancelledReader = async(start = CoroutineStart.UNDISPATCHED) { processor.prepare(request) }
    val survivingReader = async(start = CoroutineStart.UNDISPATCHED) { processor.prepare(request) }

    try {
      assertSame(request.source.image, analyzer.awaitStart())
      cancelledReader.cancel()
      runCurrent()
      cancelledReader.join()
      analyzer.release()

      assertSame(LogoTreatment.Original, survivingReader.await())
      assertEquals(1, renderCalls)
      assertSame(LogoTreatment.Original, processor.cached(request))
    } finally {
      analyzer.releaseAll()
      cancelledReader.cancelAndJoin()
      survivingReader.cancelAndJoin()
      processor.close()
    }
  }

  @Test
  fun cancellingTheLastReaderCancelsAJobWaitingForTheWorker() = runTest {
    val analyzer = BlockingAnalyzer()
    val processor = LogoTreatmentProcessor(
      dispatcher = Dispatchers.Default,
      analyze = analyzer::analyze,
      render = { _, _, _, _ -> null },
    )
    val activeRequest = request(FakeImage())
    val queuedRequest = request(FakeImage())
    val active = async(start = CoroutineStart.UNDISPATCHED) { processor.prepare(activeRequest) }
    var queued: Deferred<LogoTreatment>? = null

    try {
      assertSame(activeRequest.source.image, analyzer.awaitStart())
      val queuedReader = async(start = CoroutineStart.UNDISPATCHED) { processor.prepare(queuedRequest) }
      queued = queuedReader
      settleDefaultDispatcher()
      queuedReader.cancel()
      runCurrent()
      queuedReader.join()
      analyzer.release()
      assertSame(LogoTreatment.Original, active.await())
      settleDefaultDispatcher()

      val staleStart = analyzer.tryStart()
      if (staleStart != null) analyzer.release()
      assertNull(staleStart)
      assertNull(processor.cached(queuedRequest))

      val retried = async(start = CoroutineStart.UNDISPATCHED) { processor.prepare(queuedRequest) }
      assertSame(queuedRequest.source.image, analyzer.awaitStart())
      analyzer.release()
      assertSame(LogoTreatment.Original, retried.await())
    } finally {
      analyzer.releaseAll()
      active.cancelAndJoin()
      queued?.cancelAndJoin()
      processor.close()
    }
  }

  @Test
  fun treatmentWorkersRunOneAtATime() = runTest {
    val analyzer = BlockingAnalyzer()
    val processor = LogoTreatmentProcessor(
      dispatcher = Dispatchers.Default,
      analyze = analyzer::analyze,
      render = { _, _, _, _ -> null },
    )
    val firstRequest = request(FakeImage())
    val secondRequest = request(FakeImage())
    val first = async(start = CoroutineStart.UNDISPATCHED) { processor.prepare(firstRequest) }
    val second = async(start = CoroutineStart.UNDISPATCHED) { processor.prepare(secondRequest) }

    try {
      val firstStart = analyzer.awaitStart()
      settleDefaultDispatcher()
      assertNull(analyzer.tryStart())
      analyzer.release()

      val secondStart = analyzer.awaitStart()
      assertFalse(firstStart === secondStart)
      analyzer.release()
      assertSame(LogoTreatment.Original, first.await())
      assertSame(LogoTreatment.Original, second.await())
    } finally {
      analyzer.releaseAll()
      first.cancelAndJoin()
      second.cancelAndJoin()
      processor.close()
    }
  }

  private suspend fun settleDefaultDispatcher() {
    withContext(Dispatchers.Default) {
      repeat(100) { yield() }
    }
  }

  private class BlockingAnalyzer {
    private val starts = Channel<Image>(Channel.UNLIMITED)
    private val releases = Channel<Unit>(Channel.UNLIMITED)

    fun analyze(image: Image): LogoEdgeAnalysis {
      starts.trySend(image).getOrThrow()
      runBlocking { releases.receive() }
      return whiteEdges
    }

    suspend fun awaitStart(): Image = starts.receive()

    fun tryStart(): Image? = starts.tryReceive().getOrNull()

    fun release() {
      releases.trySend(Unit).getOrThrow()
    }

    fun releaseAll() {
      repeat(4) { releases.trySend(Unit) }
    }
  }

  private class FakeImage(override val size: Long = 0) : Image {
    override val width: Int = 1
    override val height: Int = 1
    override val shareable: Boolean = true

    override fun draw(canvas: Canvas): Nothing = error("The injected processor callbacks must not draw the image")
  }

  private companion object {
    val whiteEdges = LogoEdgeAnalysis(listOf(Color.White))

    fun request(
      image: Image,
      pixelSize: IntSize = IntSize(48, 48),
      background: Color = Color.White,
    ): LogoTreatmentRequest = LogoTreatmentRequest(
      source = LogoSource(image),
      pixelSize = pixelSize,
      radiusPx = 2f,
      background = background,
    )
  }
}
