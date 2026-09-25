/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.network.NetworkClient
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import dev.staticvar.vlr.sharedui.component.common.withNeutralLogoOutline
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamLogoLoaderTest {
  @Test
  fun neutralOutlineMakesBlackLogoEdgesVisible() {
    val logo = transparentLogo(Color.BLACK)

    val prepared = logo.withNeutralLogoOutline(radiusPx = 2f)

    assertNotSame(logo, prepared)
    assertEquals(24, prepared.width)
    assertEquals(24, prepared.height)
    assertTrue(prepared.containsOpaqueColor(Color.BLACK))
    assertTrue(prepared.containsOpaqueColor(Color.WHITE))
  }

  @Test
  fun neutralOutlineLeavesLightLogoUnchanged() {
    val logo = transparentLogo(Color.WHITE)

    assertSame(logo, logo.withNeutralLogoOutline(radiusPx = 2f))
  }

  @Test
  fun urlPolicyMatchesTheLiveActivityHosts() {
    listOf(
      "https://owcdn.net/img/team.png",
      "https://www.vlr.gg/img/team.png",
      "https://owcdn.net:443/img/team.png",
    ).forEach { assertNotNull(it, allowedTeamLogoUrl(it)) }

    listOf(
      "http://owcdn.net/img/team.png",
      "https://example.com/team.png",
      "https://owcdn.net.evil.example/team.png",
      "https://evil-owcdn.net/team.png",
      "https://www.vlr.gg.evil.example/team.png",
      "https://owcdn%2Enet/team.png",
      "https://owcdn.net./team.png",
      "https://127.0.0.1/team.png",
      "https://[::1]/team.png",
      "https://user@owcdn.net/team.png",
      "https://user:password@www.vlr.gg/team.png",
      "https://owcdn.net:444/team.png",
      "file:///tmp/team.png",
    ).forEach { assertNull(it, allowedTeamLogoUrl(it)) }
  }

  @Test
  fun redirectsStayInsideTheLogoHostPolicy() {
    val source = "https://owcdn.net/img/team.png"
    assertEquals("https://owcdn.net/img/next.png", allowedTeamLogoRedirect(source, "next.png"))
    assertEquals("https://www.vlr.gg/img/team.png", allowedTeamLogoRedirect(source, "https://www.vlr.gg/img/team.png"))
    listOf(
      "http://owcdn.net/img/team.png",
      "https://example.com/team.png",
      "https://127.0.0.1/team.png",
      "https://user@owcdn.net/team.png",
      "https://owcdn.net:444/team.png",
    ).forEach { assertNull(it, allowedTeamLogoRedirect(source, it)) }
  }

  @Test
  fun networkClientFollowsOnlyValidatedRedirects() = runBlocking {
    val visited = mutableListOf<String>()
    val allowedEngine = MockEngine { request ->
      visited += request.url.toString()
      if (visited.size == 1) {
        respond(
          content = ByteReadChannel.Empty,
          status = HttpStatusCode.Found,
          headers = headersOf(HttpHeaders.Location, "/img/final.png"),
        )
      } else {
        respond(content = ByteReadChannel(byteArrayOf(1, 2, 3)), status = HttpStatusCode.OK)
      }
    }
    val allowedClient = TeamLogoNetworkClient(HttpClient(allowedEngine) { followRedirects = false })
    val size = allowedClient.executeRequest(NetworkRequest("https://owcdn.net/img/team.png")) { response ->
      val sink = Buffer()
      requireNotNull(response.body).writeTo(sink)
      sink.size
    }
    assertEquals(3L, size)
    assertEquals(listOf("https://owcdn.net/img/team.png", "https://owcdn.net/img/final.png"), visited)

    var rejectedRequests = 0
    val rejectedEngine = MockEngine {
      rejectedRequests += 1
      respond(
        content = ByteReadChannel.Empty,
        status = HttpStatusCode.Found,
        headers = headersOf(HttpHeaders.Location, "https://example.com/team.png"),
      )
    }
    val rejectedClient = TeamLogoNetworkClient(HttpClient(rejectedEngine) { followRedirects = false })
    var rejected = false
    try {
      rejectedClient.executeRequest(NetworkRequest("https://owcdn.net/img/team.png")) { }
    } catch (_: IOException) {
      rejected = true
    }
    assertTrue(rejected)
    assertEquals(1, rejectedRequests)
  }

  @Test
  fun responseBodyStopsAtOneMiB() = runBlocking {
    val exact = ByteArray(MaxDownloadBytes.toInt()) { 1 }
    assertEquals(exact.size, readBoundedTeamLogoBytes(ByteReadChannel(exact), MaxDownloadBytes).size)

    val oversized = ByteArray(MaxDownloadBytes.toInt() + 1) { 1 }
    var rejected = false
    try {
      readBoundedTeamLogoBytes(ByteReadChannel(oversized), MaxDownloadBytes)
    } catch (_: IOException) {
      rejected = true
    }
    assertTrue(rejected)
  }

  @Test
  fun decodedLogoReusesTheSingletonStyleMemoryCache() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val png = ByteArrayOutputStream().use { output ->
      Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
        eraseColor(0xff00ff00.toInt())
      }.compress(Bitmap.CompressFormat.PNG, 100, output)
      output.toByteArray()
    }
    var requests = 0
    val networkClient = object : NetworkClient {
      override suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (response: NetworkResponse) -> T,
      ): T {
        requests += 1
        return block(
          NetworkResponse(
            headers = NetworkHeaders.Builder().apply { this["content-type"] = "image/png" }.build(),
            body = NetworkResponseBody(Buffer().write(png)),
          ),
        )
      }
    }
    val imageLoader = ImageLoader.Builder(context)
      .memoryCache { MemoryCache.Builder().maxSizeBytes(2L * 1024L * 1024L).build() }
      .build()
    val loader = TeamLogoLoader(context, imageLoader, TeamLogoFetcherFactory(networkClient))
    val update = LiveMatchUpdate(
      matchId = "991000001",
      observedAt = 1,
      terminal = false,
      teams = listOf(
        LiveMatchTeam("Team Liquid", "https://owcdn.net/img/team.png", 1),
        LiveMatchTeam("Paper Rex", null, 0),
      ),
      currentMap = LiveMatchMap("Ascent", listOf(8, 6)),
    )

    assertNotNull(loader.load(update)?.first)
    assertNotNull(loader.cached(update).first)
    assertNotNull(loader.load(update)?.first)
    assertEquals(1, requests)
  }

  private fun transparentLogo(color: Int): Bitmap =
    Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply {
      for (y in 6..13) {
        for (x in 6..13) setPixel(x, y, color)
      }
    }

  private fun Bitmap.containsOpaqueColor(color: Int): Boolean {
    for (y in 0 until height) {
      for (x in 0 until width) {
        if (getPixel(x, y) == color) return true
      }
    }
    return false
  }
}
