/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Maps VLR team IDs to the larger team logos mirrored on our CDN.
 *
 * The manifest is kept on disk and refreshed at most once a day; teams missing from it keep VLR's logo.
 */
internal class TeamLogoDirectory(context: Context, private val json: Json) {
  private val file = File(context.applicationContext.filesDir, FileName)

  @Volatile
  private var logos: Map<String, String> = emptyMap()

  fun logoUrl(teamId: String?): String? = teamId?.let { logos[it] }

  /**
   * Loads the saved manifest, then downloads a new one when it is missing or a day old.
   *
   * @return Whether the known logos changed.
   */
  suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
    val before = logos
    if (logos.isEmpty() && file.exists()) {
      runCatching { logos = parse(file.readText()) }
    }
    if (!file.exists() || System.currentTimeMillis() - file.lastModified() > MaxAgeMillis) {
      runCatching {
        val connection = URL(ManifestUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = TimeoutMillis
        connection.readTimeout = TimeoutMillis
        try {
          val body = connection.inputStream.bufferedReader().use { it.readText() }
          logos = parse(body)
          file.writeText(body)
        } finally {
          connection.disconnect()
        }
      }.onFailure { android.util.Log.w("LiveMatchNotifications", "Could not refresh team logos", it) }
    }
    logos != before
  }

  private fun parse(body: String): Map<String, String> = json.parseToJsonElement(body).jsonObject.mapNotNull { (teamId, entry) ->
    val url = entry.jsonObject["logo"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
    url?.takeIf(LiveMatchLogoCache::isAllowedLogoUrl)?.let { teamId to it }
  }.toMap()

  private companion object {
    const val ManifestUrl = "https://files.akhilnarang.dev/cdn/valorant/teams.json"
    const val FileName = "team_logos.json"
    const val MaxAgeMillis = 24 * 60 * 60 * 1_000L
    const val TimeoutMillis = 10_000
  }
}
