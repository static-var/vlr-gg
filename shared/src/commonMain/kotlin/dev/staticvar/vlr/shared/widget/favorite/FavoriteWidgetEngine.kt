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
        TeamWidgetText.setup(gate.reason, locale),
        null,
        0,
        WidgetSchedule(null, null),
      )

      is WidgetGate.MissingClient -> WidgetProjection(
        TeamWidgetText.placeholder(null, locale),
        null,
        0,
        WidgetSchedule(null, null),
      )

      is WidgetGate.Ready -> {
        val record = WidgetCacheCodec.decode(stored, gate.key)
        if (record == null) {
          WidgetProjection(
            TeamWidgetText.placeholder(gate.key, locale),
            gate.key,
            0,
            WidgetSchedule(null, request.now),
          )
        } else {
          val cached = record.value as? CachedValue.Team
          val fallback = record.origin == RecordOrigin.CachedFallback || request.now >= record.fetchAfter
          val content = cached?.let {
            if (fallback) TeamWidgetRules.expire(it.value, it.fetchedAt, request.now) else it.value
          }
          val renderAt = cached?.let {
            TeamWidgetRules.renderAt(it.value, it.fetchedAt, fallback, request.now, request.timeZone, record.fetchAfter)
          }
          val frame = if (content == null) TeamWidgetText.placeholder(gate.key, locale) else {
            TeamWidgetText.frame(gate.key, content, gate.source.spoilersHidden, request.now, request.timeZone, locale)
          }
          WidgetProjection(frame, gate.key, record.revision, WidgetSchedule(renderAt, record.fetchAfter))
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
    val fetched = try {
      remote.team(key)
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      null
    }
    currentCoroutineContext().ensureActive()
    val attemptedAt = clock.now()
    val value = fetched?.let { CachedValue.Team(it, attemptedAt) } ?: previous?.value ?: CachedValue.NoData
    val origin = if (fetched == null) RecordOrigin.CachedFallback else RecordOrigin.Fresh
    val content = (value as? CachedValue.Team)?.let {
      if (origin == RecordOrigin.CachedFallback) TeamWidgetRules.expire(it.value, it.fetchedAt, attemptedAt) else it.value
    }
    val record = WidgetCacheRecord(
      version = WidgetCacheCodec.VERSION,
      key = key,
      revision = (previous?.revision ?: 0) + 1,
      attemptedAt = attemptedAt,
      fetchAfter = TeamWidgetRules.refreshAt(content, attemptedAt),
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
  val favorites = current.favoriteTeams.map { it.id }.toSet()
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
