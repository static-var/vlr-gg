package dev.staticvar.vlr.remotesource.network

import io.ktor.http.URLProtocol

private const val DEFAULT_TIMEOUT = 20_000L

/**
 * Declarative definition of the network stack requirements.
 */
data class NetworkConfiguration(
  val host: String,
  val defaultProtocol: URLProtocol = URLProtocol.HTTPS,
  val defaultHeaders: Map<String, String> = emptyMap(),
  val timeoutMillis: Long = DEFAULT_TIMEOUT,
  val enableCompression: Boolean = true,
  val enableNetworkLogs: Boolean = false,
) {
  init {
    require(host.isNotBlank()) { "host must not be blank" }
    require(timeoutMillis > 0) { "timeoutMillis must be positive" }
  }
}
