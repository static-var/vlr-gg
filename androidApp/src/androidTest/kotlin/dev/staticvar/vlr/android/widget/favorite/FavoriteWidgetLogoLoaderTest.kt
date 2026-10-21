/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.asImage
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetFrame
import dev.staticvar.vlr.shared.widget.favorite.LogoMark
import dev.staticvar.vlr.shared.widget.favorite.TeamDisplayBody
import dev.staticvar.vlr.shared.widget.favorite.TeamHeaderText
import dev.staticvar.vlr.shared.widget.favorite.TeamHeroText
import dev.staticvar.vlr.shared.widget.favorite.TeamPresentation
import dev.staticvar.vlr.shared.widget.favorite.WidgetKey
import dev.staticvar.vlr.shared.widget.favorite.WidgetKind
import dev.staticvar.vlr.shared.widget.favorite.WidgetTap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class FavoriteWidgetLogoLoaderTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private lateinit var testImageLoader: ImageLoader

  @After
  fun closeImageLoader() {
    if (::testImageLoader.isInitialized) testImageLoader.shutdown()
  }

  @Test
  fun sourcesKeepPriorityAndDuplicatesLoadOnce() = runBlocking {
    val requested = mutableListOf<String>()
    val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
    installLoader { chain ->
      requested += requireNotNull(chain.request.memoryCacheKey)
      assertFalse(chain.request.allowHardware)
      assertEquals(Size(96, 96), chain.size)
      SuccessResult(bitmap.asImage(), chain.request)
    }

    val result = FavoriteWidgetLogoLoader(context, testImageLoader).load(frame(listOf(logo(2), logo(1), logo(2))))

    assertEquals(listOf(logo(2), logo(1)), result.bySource.keys.toList())
    assertEquals(listOf("favorite-widget:${logo(2)}", "favorite-widget:${logo(1)}"), requested)
    assertSame(bitmap, result.bySource[logo(2)])
  }

  @Test
  fun disallowedSourcesNeverReachCoil() = runBlocking {
    var requests = 0
    installLoader { chain ->
      requests++
      SuccessResult(Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).asImage(), chain.request)
    }

    val result = FavoriteWidgetLogoLoader(context, testImageLoader).load(
      frame(listOf("http://owcdn.net/logo.png", "https://unapproved.example/logo.png", "file:///logo.png")),
    )

    assertEquals(emptyMap<String, Bitmap>(), result.bySource)
    assertEquals(0, requests)
  }

  @Test
  fun allocationBudgetKeepsEarlierSourcesAndAllowsASmallerLogo() = runBlocking {
    installLoader { chain ->
      val size = if (chain.request.memoryCacheKey == "favorite-widget:${logo(7)}") 32 else 96
      SuccessResult(Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).asImage(), chain.request)
    }

    val result = FavoriteWidgetLogoLoader(context, testImageLoader).load(frame((0..7).map(::logo)))

    assertEquals(listOf(logo(0), logo(1), logo(2), logo(3), logo(4), logo(7)), result.bySource.keys.toList())
    assertEquals(188416, result.bySource.values.sumOf { it.allocationByteCount })
  }

  @Test
  fun allocationLimitUsesRetainedAllocationAfterReconfigure() = runBlocking {
    val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
    bitmap.reconfigure(96, 96, Bitmap.Config.ARGB_8888)
    installLoader { chain -> SuccessResult(bitmap.asImage(), chain.request) }

    val result = FavoriteWidgetLogoLoader(context, testImageLoader).load(frame(listOf(logo(0))))

    assertEquals(emptyMap<String, Bitmap>(), result.bySource)
  }

  @Test(timeout = 7000)
  fun frameTimeoutReturnsCompletedLogosAndStopsFurtherRequests() = runBlocking {
    val requested = mutableListOf<String>()
    installLoader { chain ->
      requested += requireNotNull(chain.request.memoryCacheKey)
      if (requested.size == 1) {
        SuccessResult(Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).asImage(), chain.request)
      } else {
        awaitCancellation()
      }
    }

    val result = FavoriteWidgetLogoLoader(context, testImageLoader).load(frame((0..2).map(::logo)))

    assertEquals(listOf(logo(0)), result.bySource.keys.toList())
    assertEquals(listOf("favorite-widget:${logo(0)}", "favorite-widget:${logo(1)}"), requested)
  }

  @Test(timeout = 5000)
  fun cancelledFrameRethrowsCancellation() = runBlocking {
    val started = CompletableDeferred<Unit>()
    installLoader {
      started.complete(Unit)
      awaitCancellation()
    }
    val loading = async { FavoriteWidgetLogoLoader(context, testImageLoader).load(frame(listOf(logo(0)))) }
    started.await()

    loading.cancel()

    try {
      loading.await()
      fail("Cancelled frame returned bitmaps")
    } catch (_: CancellationException) {
      assertEquals(true, loading.isCancelled)
    }
  }

  private fun installLoader(intercept: suspend (Interceptor.Chain) -> ImageResult) {
    testImageLoader = ImageLoader.Builder(context).components { add(Interceptor(intercept)) }.build()
  }

  private fun logo(index: Int): String = "https://owcdn.net/widget-$index.png"

  private fun frame(sources: List<String>): FavoriteWidgetFrame {
    val presentation = TeamPresentation(
      TeamHeaderText(LogoMark.Named("Team", "TEAM", null, "TEA"), "Team", ""),
      TeamDisplayBody.Visible(TeamHeroText.Idle("", "", "", null), emptyList(), null),
    )
    return FavoriteWidgetFrame.Team(
      requireNotNull(WidgetKey.parse(WidgetKind.Team, "team", "client")),
      presentation,
      presentation,
      WidgetTap.App,
      sources,
    )
  }
}
