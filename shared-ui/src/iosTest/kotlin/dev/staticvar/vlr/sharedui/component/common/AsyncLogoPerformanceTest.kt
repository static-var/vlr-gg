/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.clearSkikoComposeImplementation
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import coil3.asImage
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSThread
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import kotlin.time.TimeSource

@OptIn(InternalComposeUiApi::class)
class AsyncLogoPerformanceTest {
  @BeforeTest fun setUp() = registerSkikoComposeImplementation()

  @AfterTest fun tearDown() = clearSkikoComposeImplementation()

  @Test fun processorRasterCallbacksLeaveTheCallerThread() = runBlocking {
    val callerThread = NSThread.currentThread
    var analysisThread: NSThread? = null
    var renderingThread: NSThread? = null
    val source = darkLogoFixture()
    val processor = LogoTreatmentProcessor(
      analyze = { image ->
        analysisThread = NSThread.currentThread
        analyzeLogoImage(image)
      },
      render = { image, size, radius, color ->
        renderingThread = NSThread.currentThread
        renderOutlinedLogo(image, size, radius, color)
      },
    )

    try {
      val treatment = processor.prepare(request(source, 96))

      assertTrue(treatment is LogoTreatment.Outlined)
      assertNotNull(analysisThread)
      assertNotNull(renderingThread)
      assertNotSame(callerThread, analysisThread)
      assertNotSame(callerThread, renderingThread)
    } finally {
      processor.close()
    }
  }

  @Test fun reportsBitmapAndProcessorOverhead() = runBlocking {
    val source = darkLogoFixture()
    println(
      "ASYNC_LOGO_BENCH environment=iOS-simulator build=debug warmup=30 samples=200 " +
        "source=256px density=3 outline=1dp",
    )
    benchmark("bitmap-analysis32") { analyzeLogoImage(source.image) }
    for (size in listOf(96, 192)) {
      benchmark("bitmap-outline${size}px") {
        renderOutlinedLogo(source.image, IntSize(size, size), 3f, Color.White)
      }
      benchmarkSuspend("processor-cold${size}px") {
        val processor = LogoTreatmentProcessor()
        try {
          processor.prepare(request(source, size))
        } finally {
          processor.close()
        }
      }

      val cachedProcessor = LogoTreatmentProcessor()
      val logoRequest = request(source, size)
      try {
        cachedProcessor.prepare(logoRequest)
        benchmark("processor-cached-peek${size}px") {
          checkNotNull(cachedProcessor.cached(logoRequest))
        }
      } finally {
        cachedProcessor.close()
      }
    }
  }

  private fun benchmark(name: String, operation: () -> Unit) {
    repeat(Warmups) { operation() }
    val samples = DoubleArray(Samples) {
      val start = TimeSource.Monotonic.markNow()
      operation()
      start.elapsedNow().inWholeNanoseconds / 1_000.0
    }
    samples.sort()
    println("ASYNC_LOGO_BENCH $name p50_us=${samples[99]} p95_us=${samples[189]} max_us=${samples.last()}")
  }

  private suspend fun benchmarkSuspend(name: String, operation: suspend () -> Unit) {
    repeat(Warmups) { operation() }
    val samples = DoubleArray(Samples) {
      val start = TimeSource.Monotonic.markNow()
      operation()
      start.elapsedNow().inWholeNanoseconds / 1_000.0
    }
    samples.sort()
    println("ASYNC_LOGO_BENCH $name p50_us=${samples[99]} p95_us=${samples[189]} max_us=${samples.last()}")
  }

  private fun darkLogoFixture(): LogoSource {
    val bitmap = ImageBitmap(SourceSize, SourceSize)
    CanvasDrawScope().draw(
      density = Density(1f),
      layoutDirection = LayoutDirection.Ltr,
      canvas = Canvas(bitmap),
      size = Size(SourceSize.toFloat(), SourceSize.toFloat()),
    ) {
      drawRect(
        color = Color.Black,
        topLeft = Offset(size.width / 4, size.height / 4),
        size = size / 2f,
      )
    }
    return LogoSource(bitmap.asSkiaBitmap().asImage())
  }

  private fun request(source: LogoSource, size: Int) = LogoTreatmentRequest(
    source = source,
    pixelSize = IntSize(size, size),
    radiusPx = 3f,
    background = Color.Black,
  )

  private companion object {
    const val Warmups = 30
    const val Samples = 200
    const val SourceSize = 256
  }
}
