package dev.staticvar.vlr.data

import dev.staticvar.vlr.utils.Constants
import dev.staticvar.vlr.utils.Endpoints
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import java.io.File

class VlrMockEngine(private var sendISR: Boolean = false) {

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
          if (!request.headers.contains(Constants.APPLICATION_HEADER))
            return@addHandler respond(
              "Verification failed - Application Header missing",
              HttpStatusCode.Forbidden
            )
          if (!request.headers.contains(HttpHeaders.Authorization))
            return@addHandler respond(
              "Verification failed - Token missing",
              HttpStatusCode.Forbidden
            )
          println(request.url.encodedPath.removePrefix("/"))
          when (request.url.encodedPath.removePrefix("/")) {
            Endpoints.NEWS ->
              respond(readJson("news.json"), HttpStatusCode.OK, responseHeaders).also {
                println(it)
              }
            Endpoints.MATCHES_OVERVIEW -> respond(readJson("matches.json"), HttpStatusCode.OK)
            Endpoints.EVENTS_OVERVIEW -> respond(readJson("events.json"), HttpStatusCode.OK)
            Endpoints.matchDetails("107742") ->
              respond(readJson("match_details.json"), HttpStatusCode.OK)
            Endpoints.eventDetails("800") ->
              respond(readJson("event_details.json"), HttpStatusCode.OK)
            Endpoints.teamDetails("2291") ->
              respond(readJson("team_details.json"), HttpStatusCode.OK)
            else -> respond("Unknown request ${request.url.encodedPath}", HttpStatusCode.BadGateway)
          }
        }
      }
    }

  private fun readJson(path: String): String {
    println("Reading json $path")
    return getJson(this@VlrMockEngine, path)
  }

  private fun getJson(clazz: Any, fileName: String): String {
    val uri = clazz::class.java.classLoader?.getResource(fileName) ?: error("Unable to parse JSON")
    val file = File(uri.path)
    return String(file.readBytes())
  }
}
