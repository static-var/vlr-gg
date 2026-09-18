/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.network

import dev.staticvar.vlr.remotesource.jsonHeaders
import dev.staticvar.vlr.remotesource.di.remoteSourceModule
import dev.staticvar.vlr.remotesource.rankings.RankingsDataSourceImpl
import dev.staticvar.vlr.remotesource.testJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AcceptLanguageTransportTest {
  @Test
  fun same_client_reads_current_ordered_language_preferences_per_request() = runTest {
    val provider = MutableAcceptLanguageProvider(listOf("en-US"))
    val observed = mutableListOf<String?>()
    val client = configuredClient(provider) { request ->
      observed += request.headers[HttpHeaders.AcceptLanguage]
      respond("{}", status = HttpStatusCode.OK, headers = jsonHeaders())
    }

    client.get("/one")
    provider.tags = listOf("hi-IN", "en-US")
    client.get("/two")
    provider.tags = listOf("pt-BR", "pt-BR", "en-US")
    client.get("/three")

    assertEquals(listOf<String?>("en-US", "hi-IN,en-US", "pt-BR,en-US"), observed)
    assertEquals("pt-BR", provider.currentLanguageTag())
  }

  @Test
  fun default_headers_remain_present_with_accept_language() = runTest {
    val provider = MutableAcceptLanguageProvider(listOf("ko-KR", "en-US"))
    val headers = mutableMapOf<String, String?>()
    val client = configuredClient(
      provider = provider,
      configuration = NetworkConfiguration(
        host = "api.example",
        defaultHeaders = mapOf("app-name" to "dev.staticvar.vlr", HttpHeaders.Authorization to "fixture-token"),
      ),
    ) { request ->
      headers["app-name"] = request.headers["app-name"]
      headers[HttpHeaders.Authorization] = request.headers[HttpHeaders.Authorization]
      headers[HttpHeaders.AcceptLanguage] = request.headers[HttpHeaders.AcceptLanguage]
      respond("{}", status = HttpStatusCode.OK, headers = jsonHeaders())
    }

    client.get("/headers")

    assertEquals("dev.staticvar.vlr", headers["app-name"])
    assertEquals("fixture-token", headers[HttpHeaders.Authorization])
    assertEquals("ko-KR,en-US", headers[HttpHeaders.AcceptLanguage])
  }

  @Test
  fun localized_payload_keeps_requested_and_server_selected_languages() = runTest {
    val provider = MutableAcceptLanguageProvider(listOf("pt-BR", "en-US"))
    val responseHeaders = Headers.build {
      append(HttpHeaders.ContentType, "application/json")
      append(HttpHeaders.ContentLanguage, "pt-BR")
    }
    val client = configuredClient(provider) {
      respond(
        """[{"region":"Brazil","region_label":"Brasil","teams":[]}]""",
        status = HttpStatusCode.OK,
        headers = responseHeaders,
      )
    }

    val payload = RankingsDataSourceImpl(client).list().getOrThrow()

    assertEquals("pt-BR,en-US", payload.requestedLanguage)
    assertEquals("pt-BR", payload.contentLanguage)
    assertEquals("Brazil", payload.value.single().region)
    assertEquals("Brasil", payload.value.single().regionLabel)
  }

  @Test
  fun koin_uses_injected_language_provider() {
    val provider = MutableAcceptLanguageProvider(listOf("tr-TR"))
    val koin = koinApplication {
      modules(remoteSourceModule(NetworkConfiguration(host = "api.example"), provider))
    }.koin

    assertSame(provider, koin.get<AcceptLanguageProvider>())
  }

  private fun configuredClient(
    provider: AcceptLanguageProvider,
    configuration: NetworkConfiguration = NetworkConfiguration(host = "api.example"),
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
  ): HttpClient = HttpClient(MockEngine) {
    configureHttpClient(testJson(), configuration, provider)
    engine { addHandler(handler) }
  }
}

private class MutableAcceptLanguageProvider(var tags: List<String>) : AcceptLanguageProvider {
  override fun preferredLanguageTags(): List<String> = tags
}
