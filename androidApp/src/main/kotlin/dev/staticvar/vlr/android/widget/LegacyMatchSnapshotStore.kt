/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget

import android.content.Context
import android.util.AtomicFile
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.shared.widget.UpcomingWidgetMatch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.time.Instant

internal object LegacyMatchSnapshotStore {
  private const val FILE_NAME = "all_matches_widget.json"

  fun read(context: Context, json: Json): WidgetSnapshot? = try {
    parseWidgetSnapshot(AtomicFile(context.getFileStreamPath(FILE_NAME)).readFully().toString(Charsets.UTF_8), json)
  } catch (_: IOException) {
    null
  }

  fun write(context: Context, matches: List<MatchPreview>, spoilersHidden: Boolean, json: Json) {
    val rows = matches.filter { it.status == MatchStatus.LIVE || it.status == MatchStatus.UPCOMING }
      .sortedBy { it.status != MatchStatus.LIVE }
      .map { match ->
        UpcomingWidgetMatch(
          id = match.id,
          event = match.event,
          team1 = match.team1.name,
          team2 = match.team2.name,
          startTimeEpochMillis = match.time?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() },
          status = match.status.name,
          score1 = match.team1.score.takeUnless { spoilersHidden },
          score2 = match.team2.score.takeUnless { spoilersHidden },
          stage = match.series,
        )
      }
    val payload = json.encodeToString(LegacyMatchSnapshotDto(System.currentTimeMillis(), spoilersHidden, rows))
    val file = AtomicFile(context.getFileStreamPath(FILE_NAME))
    val output = file.startWrite()
    try {
      output.write(payload.toByteArray(Charsets.UTF_8))
      file.finishWrite(output)
    } catch (error: Exception) {
      file.failWrite(output)
      throw error
    }
  }
}

@Serializable
private data class LegacyMatchSnapshotDto(
  val savedAtEpochMillis: Long,
  val spoilersHidden: Boolean,
  val matches: List<UpcomingWidgetMatch>,
)
