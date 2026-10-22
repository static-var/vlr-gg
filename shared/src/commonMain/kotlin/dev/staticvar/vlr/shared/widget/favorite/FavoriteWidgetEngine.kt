/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import dev.staticvar.vlr.shared.widget.UpcomingMatchesSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

class FavoriteWidgetEngine internal constructor(
  private val remote: FavoriteWidgetRemote,
  private val clock: Clock,
  private val json: Json,
) {
  fun decodeSource(json: String): UpcomingMatchesSnapshot? = try {
    this.json.decodeFromString<UpcomingMatchesSnapshot>(json)
  } catch (_: IllegalArgumentException) {
    null
  }

  fun key(request: WidgetRequest): WidgetKey? = (request.gate() as? WidgetGate.Ready)?.key

  fun project(request: WidgetRequest, stored: StoredWidgetRecord?, locale: WidgetLocale): WidgetProjection =
    when (val gate = request.gate()) {
      is WidgetGate.Setup -> WidgetProjection(
        when (gate.kind) {
          WidgetKind.Team -> TeamWidgetText.setup(gate.reason, locale)
          WidgetKind.Event -> EventWidgetText.setup(gate.reason, locale)
        },
        null,
        0,
        WidgetSchedule(null, null),
      )

      is WidgetGate.MissingClient -> WidgetProjection(
        when (gate.kind) {
          WidgetKind.Team -> TeamWidgetText.placeholder(null, locale)
          WidgetKind.Event -> EventWidgetText.placeholder(null, locale, gate.entityId)
        },
        null,
        0,
        WidgetSchedule(null, null),
      )

      is WidgetGate.Ready -> {
        val record = WidgetCacheCodec.decode(stored, gate.key)
        if (record == null) {
          WidgetProjection(
            when (gate.key.kind) {
              WidgetKind.Team -> TeamWidgetText.placeholder(gate.key, locale)
              WidgetKind.Event -> EventWidgetText.placeholder(gate.key, locale, gate.key.entityId)
            },
            gate.key,
            0,
            WidgetSchedule(null, request.now),
          )
        } else {
          val projected = when (gate.key.kind) {
            WidgetKind.Team -> TeamWidgetProgram.project(
              gate.key,
              gate.source,
              record,
              request.now,
              request.timeZone,
              locale,
            )

            WidgetKind.Event -> EventWidgetProgram.project(
              gate.key,
              gate.source,
              record,
              request.now,
              request.timeZone,
              locale,
            )
          }
          WidgetProjection(projected.frame, gate.key, record.revision, projected.schedule)
        }
      }
    }

  /** [WidgetRequest.now] chooses what to fetch; the clock stamps the record. */
  suspend fun refresh(request: WidgetRequest, stored: StoredWidgetRecord?): WidgetRefreshResult {
    val gate = when (val gate = request.gate()) {
      is WidgetGate.Setup, is WidgetGate.MissingClient -> return WidgetRefreshResult.Refused
      is WidgetGate.Ready -> gate
    }
    val key = gate.key
    val previous = WidgetCacheCodec.decode(stored, key)
    val fetched: FavoriteWidgetContent? = try {
      when (key.kind) {
        WidgetKind.Team -> remote.team(key)
        WidgetKind.Event -> remote.event(key, gate.source.favorites.teamIds.toSet(), request.now, request.timeZone)
      }
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      null
    }
    currentCoroutineContext().ensureActive()
    val attemptedAt = clock.now()
    val value = when (fetched) {
      is TeamContent -> CachedValue.Team(fetched, attemptedAt)
      is EventData -> CachedValue.Event(fetched, attemptedAt)
      null -> previous?.value ?: CachedValue.NoData
    }
    val origin = if (fetched == null) RecordOrigin.CachedFallback else RecordOrigin.Fresh
    val record = WidgetCacheRecord(
      version = WidgetCacheCodec.VERSION,
      key = key,
      revision = (previous?.revision ?: 0) + 1,
      attemptedAt = attemptedAt,
      fetchAfter = when (key.kind) {
        WidgetKind.Team -> TeamWidgetProgram.fetchAfter(value, origin, attemptedAt)
        WidgetKind.Event -> EventWidgetProgram.fetchAfter(value, gate.source, attemptedAt, request.timeZone)
      },
      origin = origin,
      value = value,
    )
    currentCoroutineContext().ensureActive()
    return WidgetRefreshResult.Store(key, WidgetCacheCodec.encode(record))
  }
}

private sealed interface WidgetGate {
  data class Setup(val kind: WidgetKind, val reason: SetupReason) : WidgetGate
  data class MissingClient(val kind: WidgetKind, val entityId: String) : WidgetGate
  data class Ready(val key: WidgetKey, val source: UpcomingMatchesSnapshot) : WidgetGate
}

private fun WidgetRequest.gate(): WidgetGate {
  val kind = when (val selected = selection) {
    is WidgetSelection.None -> selected.kind
    is WidgetSelection.Chosen -> selected.kind
  }
  val current = source ?: return WidgetGate.Setup(kind, SetupReason.NoFavorites)
  val favorites = when (kind) {
    WidgetKind.Team -> current.favoriteTeams.map { it.id }.toSet()
    WidgetKind.Event -> current.favoriteEvents.map { it.id }.toSet()
  }
  if (favorites.isEmpty()) return WidgetGate.Setup(kind, SetupReason.NoFavorites)
  return when (val selected = selection) {
    is WidgetSelection.None -> WidgetGate.Setup(kind, SetupReason.Choose)

    is WidgetSelection.Chosen -> {
      if (selected.entityId !in favorites) {
        WidgetGate.Setup(kind, SetupReason.Choose)
      } else {
        val key = WidgetKey.parse(kind, selected.entityId, current.clientId)
        if (key == null) WidgetGate.MissingClient(kind, selected.entityId) else WidgetGate.Ready(key, current)
      }
    }
  }
}
