package dev.staticvar.vlr.data

import dev.staticvar.vlr.utils.Endpoints
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import java.io.File

internal class GithubMockEngine(private var sendISR: Boolean = false) {
  fun get() = client.engine

  fun nextResponseWithServerError() {
    sendISR = true
  }
  private val responseHeaders =
    headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

  private val client =
    HttpClient(MockEngine) {
      engine {
        addHandler { request ->
          println(sendISR)
          // Return if header is missing
          if (sendISR)
            return@addHandler respond(
                "Internal Server Error - 500",
                HttpStatusCode.InternalServerError
              )
              .also {
                sendISR = false
              } // Respond once with server error and then process other requests normally

          println(request.url.encodedPath)
          when (request.url.toString()) {
            Endpoints.APK_VERSION_PAGE_LINK -> respond("0.2.0", HttpStatusCode.OK, responseHeaders)
            Endpoints.APK_DOWNLOAD_PAGE_LINK ->
              respond(readJson("github_latest_release.html"), HttpStatusCode.OK, responseHeaders)
            else -> respond("Unknown request ${request.url.encodedPath}", HttpStatusCode.BadGateway)
          }
        }
      }
    }

  private fun readJson(path: String): String {
    println("Reading json $path")
    return getJson(this@GithubMockEngine, path)
  }

  private fun getJson(clazz: Any, fileName: String): String {
    val uri = clazz::class.java.classLoader?.getResource(fileName) ?: error("Unable to parse JSON")
    val file = File(uri.path)
    return String(file.readBytes())
  }
}
