/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.IntSize
import coil3.Image
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlin.math.ceil

internal class LogoSource(val image: Image) {
  override fun equals(other: Any?): Boolean = other is LogoSource && image === other.image
  override fun hashCode(): Int = image.hashCode()
}

internal data class LogoTreatmentRequest(
  val source: LogoSource,
  val pixelSize: IntSize,
  val radiusPx: Float,
  val background: Color,
)

internal sealed interface LogoTreatment {
  data object Original : LogoTreatment
  data class Outlined(val bitmap: ImageBitmap, val padding: Int) : LogoTreatment
}

internal interface LogoTreatmentProvider {
  fun cached(request: LogoTreatmentRequest): LogoTreatment?
  suspend fun prepare(request: LogoTreatmentRequest): LogoTreatment
}

internal val sharedLogoTreatments: LogoTreatmentProvider = LogoTreatmentProcessor()

internal class LogoTreatmentProcessor(
  dispatcher: CoroutineDispatcher = Dispatchers.Default,
  private val analyze: (Image) -> LogoEdgeAnalysis = ::analyzeLogoImage,
  private val render: (Image, IntSize, Float, Color) -> ImageBitmap? = ::renderOutlinedLogo,
  cacheBytes: Long = 4L * 1024 * 1024,
) : LogoTreatmentProvider {
  private val scope = CoroutineScope(SupervisorJob() + dispatcher)
  private val mutex = Mutex()
  private val worker = Semaphore(1)
  private val analyses = LogoCache<LogoSource, LogoEdgeAnalysis>(cacheBytes) { key, _ -> key.image.size + 4096 }
  private val treatments = LogoCache<LogoTreatmentRequest, LogoTreatment>(cacheBytes) { key, value ->
    key.source.image.size + 128 + if (value is LogoTreatment.Outlined) {
      value.bitmap.width.toLong() * value.bitmap.height * 4
    } else 0
  }
  private val inFlight = mutableMapOf<LogoTreatmentRequest, PendingTreatment>()

  override fun cached(request: LogoTreatmentRequest): LogoTreatment? {
    if (!mutex.tryLock()) return null
    return try { treatments[request] } finally { mutex.unlock() }
  }

  override suspend fun prepare(request: LogoTreatmentRequest): LogoTreatment {
    val pending = mutex.withLock {
      treatments[request]?.let { return it }
      inFlight.getOrPut(request) {
        PendingTreatment(scope.async(start = CoroutineStart.LAZY) {
          worker.withPermit { process(request) }
        })
      }.also { it.readers++ }
    }
    try {
      return pending.result.await()
    } finally {
      // Mutex sections contain no suspension; cleanup must also run for cancelled readers.
      kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
        mutex.withLock {
          pending.readers--
          if (pending.readers == 0 && inFlight[request] === pending) {
            inFlight.remove(request)
            if (!pending.result.isCompleted) pending.result.cancel()
          }
        }
      }
    }
  }

  private suspend fun process(request: LogoTreatmentRequest): LogoTreatment {
    currentCoroutineContext().ensureActive()
    val source = request.source
    val analysis = mutex.withLock { analyses[source] } ?: analyze(source.image).also {
      currentCoroutineContext().ensureActive()
      mutex.withLock { analyses.put(source, it) }
    }
    val result = if (analysis.needsOutline(request.background)) {
      val color = if (request.background.luminance() > 0.179f) Color.Black else Color.White
      render(source.image, request.pixelSize, request.radiusPx, color)?.let {
        LogoTreatment.Outlined(it, ceil(request.radiusPx).toInt())
      } ?: LogoTreatment.Original
    } else LogoTreatment.Original
    currentCoroutineContext().ensureActive()
    mutex.withLock { treatments.put(request, result) }
    return result
  }

  fun close() = scope.cancel()

  private class PendingTreatment(val result: Deferred<LogoTreatment>, var readers: Int = 0)
}

private class LogoCache<K, V>(
  private val maxBytes: Long,
  private val weight: (K, V) -> Long,
) {
  private val values = LinkedHashMap<K, V>()
  private var bytes = 0L

  operator fun get(key: K): V? = values.remove(key)?.also { values[key] = it }

  fun put(key: K, value: V) {
    values.remove(key)?.let { bytes -= weight(key, it) }
    val size = weight(key, value)
    if (size > maxBytes) return
    values[key] = value
    bytes += size
    while (bytes > maxBytes || values.size > 64) {
      val oldest = values.entries.first()
      bytes -= weight(oldest.key, oldest.value)
      values.remove(oldest.key)
    }
  }
}
