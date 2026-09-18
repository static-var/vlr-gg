/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders

/** A decoded response whose language-dependent fields can be associated with the server-selected locale. */
data class RemotePayload<T>(
  val value: T,
  val requestedLanguage: String?,
  val contentLanguage: String?,
)

internal suspend inline fun <reified T> HttpResponse.bodyAsRemotePayload(): RemotePayload<T> {
  val requestedLanguage = call.request.headers[HttpHeaders.AcceptLanguage]?.trim()?.takeIf(String::isNotEmpty)
  val contentLanguage = headers[HttpHeaders.ContentLanguage]?.trim()?.takeIf(String::isNotEmpty)
  return RemotePayload(
    value = body(),
    requestedLanguage = requestedLanguage,
    contentLanguage = contentLanguage,
  )
}
