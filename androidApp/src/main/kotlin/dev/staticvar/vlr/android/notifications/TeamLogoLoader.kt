/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.util.LruCache
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.Uri
import coil3.annotation.ExperimentalCoilApi
import coil3.fetch.Fetcher
import coil3.network.CacheStrategy
import coil3.network.ConcurrentRequestStrategy
import coil3.network.ConnectivityChecker
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.allowHardware
import dev.staticvar.vlr.sharedui.component.common.withNeutralLogoOutline
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.headers
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import java.io.IOException
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okio.Buffer

/** Shares Coil's bounded caches without doing disk or network work while posting scores. */
internal class TeamLogoLoader(
  private val context: Context,
  private val imageLoader: ImageLoader = SingletonImageLoader.get(context),
  private val fetcherFactory: Fetcher.Factory<Uri> = TeamLogoFetcherFactory(),
) {
  /** Reads decoded logos only; missing images are fetched later by WorkManager. */
  fun cached(update: LiveMatchUpdate): TeamLogos = TeamLogos(
    cached(update.teams[0].imageUrl),
    cached(update.teams[1].imageUrl),
  )

  /** Loads both logos concurrently with a total deadline, preserving worker cancellation. */
  suspend fun load(update: LiveMatchUpdate): TeamLogos? = withTimeoutOrNull(DownloadTimeoutMillis) {
    coroutineScope {
      val first = async { load(update.teams[0].imageUrl) }
      val second = async { load(update.teams[1].imageUrl) }
      TeamLogos(first.await(), second.await())
    }
  }

  private fun cached(source: String?): Icon? = allowedTeamLogoUrl(source)?.let { url ->
    preparedLogos[cacheKey(url)]?.let(Icon::createWithBitmap)
  }

  private suspend fun load(source: String?): Icon? {
    val url = allowedTeamLogoUrl(source) ?: return null
    val key = cacheKey(url)
    preparedLogos[key]?.let { return Icon.createWithBitmap(it) }
    val request = ImageRequest.Builder(context)
      .data(url)
      .size(LogoSizePx)
      .allowHardware(false)
      .memoryCacheKey(key)
      .diskCacheKey(key)
      .fetcherFactory(fetcherFactory)
      .build()
    val bitmap = (imageLoader.execute(request).image as? BitmapImage)?.bitmap ?: return null
    val prepared = withContext(Dispatchers.Default) { bitmap.withNeutralLogoOutline(OutlineRadiusPx) }
    preparedLogos.put(key, prepared)
    return Icon.createWithBitmap(prepared)
  }

  private companion object {
    const val LogoSizePx = 192
    const val OutlineRadiusPx = 3f
    const val DownloadTimeoutMillis = 10_000L
    const val PreparedCacheBytes = 2 * 1024 * 1024

    val preparedLogos = object : LruCache<String, Bitmap>(PreparedCacheBytes) {
      override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    fun cacheKey(url: String): String = "notification-logo-$LogoSizePx:$url"
  }
}

/** Restricts notification logo requests without changing the shared loader's other requests. */
@OptIn(ExperimentalCoilApi::class)
internal class TeamLogoFetcherFactory(
  private val networkClient: NetworkClient = TeamLogoNetworkClient(),
) : Fetcher.Factory<Uri> {
  override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
    val url = allowedTeamLogoUrl(data.toString()) ?: return null
    return NetworkFetcher(
      url = url,
      options = options,
      networkClient = lazyOf(networkClient),
      diskCache = lazy { imageLoader.diskCache },
      cacheStrategy = lazyOf(CacheStrategy.DEFAULT),
      connectivityChecker = lazyOf(ConnectivityChecker(options.context)),
      concurrentRequestStrategy = lazyOf(ConcurrentRequestStrategy.UNCOORDINATED),
    )
  }
}

/** Fetches one bounded logo response and validates every redirect before following it. */
internal class TeamLogoNetworkClient(
  private val client: HttpClient = teamLogoHttpClient(),
) : NetworkClient {
  override suspend fun <T> executeRequest(
    request: NetworkRequest,
    block: suspend (response: NetworkResponse) -> T,
  ): T = executeRequest(request, redirects = 0, block)

  private suspend fun <T> executeRequest(
    request: NetworkRequest,
    redirects: Int,
    block: suspend (response: NetworkResponse) -> T,
  ): T {
    val requestUrl = allowedTeamLogoUrl(request.url) ?: throw IOException("Logo URL is not allowed")
    return client.prepareRequest {
      url(requestUrl)
      method = HttpMethod.parse(request.method)
      headers {
        request.headers.asMap().forEach { (name, values) -> appendAll(name, values) }
      }
    }.execute responseBlock@{ response ->
      if (response.status.value in RedirectCodes) {
        if (redirects >= MaxRedirects) throw IOException("Too many logo redirects")
        val location = response.headers[HttpHeaders.Location] ?: throw IOException("Logo redirect has no location")
        val redirect = allowedTeamLogoRedirect(requestUrl, location) ?: throw IOException("Logo redirect is not allowed")
        return@responseBlock executeRequest(request.copy(url = redirect), redirects + 1, block)
      }

      val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
      if (contentLength != null && contentLength > MaxDownloadBytes) {
        throw LogoResponseTooLargeException()
      }
      val bytes = readBoundedTeamLogoBytes(response.bodyAsChannel(), MaxDownloadBytes)
      val responseHeaders = NetworkHeaders.Builder().apply {
        response.headers.forEach { name, values -> this[name] = values }
      }.build()
      block(
        NetworkResponse(
          code = response.status.value,
          headers = responseHeaders,
          body = NetworkResponseBody(Buffer().write(bytes)),
        ),
      )
    }
  }
}

private fun teamLogoHttpClient(): HttpClient = HttpClient(OkHttp) {
  followRedirects = false
  install(HttpTimeout) {
    requestTimeoutMillis = DownloadTimeoutMillis
    connectTimeoutMillis = DownloadTimeoutMillis
    socketTimeoutMillis = DownloadTimeoutMillis
  }
}

internal fun allowedTeamLogoUrl(source: String?): String? {
  val uri = source?.let { runCatching { URI(it) }.getOrNull() } ?: return null
  val host = uri.host?.lowercase() ?: return null
  if (!uri.scheme.equals("https", ignoreCase = true) || host !in AllowedLogoHosts) return null
  if (uri.rawUserInfo != null || (uri.port != -1 && uri.port != 443)) return null
  return uri.toASCIIString()
}

internal fun allowedTeamLogoRedirect(source: String, location: String): String? =
  runCatching { URI(source).resolve(location).toASCIIString() }.getOrNull()?.let(::allowedTeamLogoUrl)

internal suspend fun readBoundedTeamLogoBytes(channel: ByteReadChannel, maxBytes: Long): ByteArray {
  val output = Buffer()
  val chunk = ByteArray(8 * 1024)
  var total = 0L
  while (true) {
    val remainingWithSentinel = maxBytes - total + 1
    val read = channel.readAvailable(chunk, 0, minOf(chunk.size.toLong(), remainingWithSentinel).toInt())
    if (read == -1) return output.readByteArray()
    if (total + read > maxBytes) throw LogoResponseTooLargeException()
    output.write(chunk, 0, read)
    total += read
  }
}

private class LogoResponseTooLargeException : IOException("Logo response exceeds $MaxDownloadBytes bytes")

private val AllowedLogoHosts = setOf("owcdn.net", "www.vlr.gg")
private val RedirectCodes = setOf(301, 302, 303, 307, 308)
internal const val MaxDownloadBytes = 1024L * 1024L
private const val MaxRedirects = 5
private const val DownloadTimeoutMillis = 10_000L
