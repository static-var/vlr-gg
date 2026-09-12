/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget

import android.content.Context
import android.util.AtomicFile
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.Instant

internal object LegacyMatchSnapshotStore {
  private const val FILE_NAME = "all_matches_widget.json"

  fun read(context: Context): WidgetSnapshot? = try {
    parseWidgetSnapshot(AtomicFile(context.getFileStreamPath(FILE_NAME)).readFully().toString(Charsets.UTF_8))
  } catch (_: IOException) {
    null
  }

  fun write(context: Context, matches: List<MatchPreview>, spoilersHidden: Boolean) {
    val rows = JSONArray()
    matches.filter { it.status == MatchStatus.LIVE || it.status == MatchStatus.UPCOMING }
      .sortedBy { it.status != MatchStatus.LIVE }
      .forEach { match ->
        rows.put(JSONObject().apply {
          put("id", match.id)
          put("event", match.event)
          put("team1", match.team1.name)
          put("team2", match.team2.name)
          put("startTimeEpochMillis", match.time?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: JSONObject.NULL)
          put("status", match.status.name)
          put("score1", match.team1.score.takeUnless { spoilersHidden } ?: JSONObject.NULL)
          put("score2", match.team2.score.takeUnless { spoilersHidden } ?: JSONObject.NULL)
          put("stage", match.series)
        })
      }
    val json = JSONObject().apply {
      put("savedAtEpochMillis", System.currentTimeMillis())
      put("spoilersHidden", spoilersHidden)
      put("matches", rows)
    }
    val file = AtomicFile(context.getFileStreamPath(FILE_NAME))
    val output = file.startWrite()
    try {
      output.write(json.toString().toByteArray(Charsets.UTF_8))
      file.finishWrite(output)
    } catch (error: Exception) {
      file.failWrite(output)
      throw error
    }
  }
}
