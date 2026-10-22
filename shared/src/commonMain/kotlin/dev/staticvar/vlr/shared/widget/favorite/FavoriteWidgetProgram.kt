/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import dev.staticvar.vlr.shared.widget.UpcomingMatchesSnapshot
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal data class ProgramProjection(val frame: FavoriteWidgetFrame, val schedule: WidgetSchedule)

internal sealed interface FavoriteWidgetContent

internal object TeamWidgetProgram {
  fun project(
    key: WidgetKey,
    source: UpcomingMatchesSnapshot,
    record: WidgetCacheRecord,
    now: Instant,
    zone: TimeZone,
    locale: WidgetLocale,
  ): ProgramProjection {
    val cached = record.value as? CachedValue.Team
    val fallback = record.origin == RecordOrigin.CachedFallback || now >= record.fetchAfter
    val content = cached?.let {
      if (fallback) TeamWidgetRules.expire(it.value, it.fetchedAt, now) else it.value
    }
    val renderAt = cached?.let {
      TeamWidgetRules.renderAt(it.value, it.fetchedAt, fallback, now, zone, record.fetchAfter)
    }
    val fetchAt = record.fetchAfter
    val frame = if (content == null) {
      TeamWidgetText.placeholder(key, locale, key.entityId)
    } else {
      TeamWidgetText.frame(key, content, source.spoilersHidden, now, zone, locale)
    }
    return ProgramProjection(frame, WidgetSchedule(renderAt, fetchAt))
  }

  fun fetchAfter(value: CachedValue, origin: RecordOrigin, now: Instant): Instant {
    val content = (value as? CachedValue.Team)?.let {
      if (origin == RecordOrigin.CachedFallback) TeamWidgetRules.expire(it.value, it.fetchedAt, now) else it.value
    }
    return TeamWidgetRules.refreshAt(content, now)
  }
}

internal object EventWidgetProgram {
  fun project(
    key: WidgetKey,
    source: UpcomingMatchesSnapshot,
    record: WidgetCacheRecord,
    now: Instant,
    zone: TimeZone,
    locale: WidgetLocale,
  ): ProgramProjection {
    val cached = record.value as? CachedValue.Event ?: return ProgramProjection(
      EventWidgetText.placeholder(key, locale, key.entityId),
      WidgetSchedule(null, record.fetchAfter),
    )
    val data = cached.value
    val fetchedAt = cached.fetchedAt
    val content = project(data, fetchedAt, source, now, zone)
    return ProgramProjection(
      EventWidgetText.frame(key, content, now, zone, locale),
      WidgetSchedule(
        EventWidgetRules.renderAt(data, content, fetchedAt, now, zone),
        record.fetchAfter,
      ),
    )
  }

  fun fetchAfter(value: CachedValue, source: UpcomingMatchesSnapshot, now: Instant, zone: TimeZone): Instant {
    val cached = value as? CachedValue.Event ?: return now + 900.seconds
    val data = cached.value
    val fetched = cached.fetchedAt
    return EventWidgetRules.refreshAt(data, project(data, fetched, source, now, zone), fetched, now)
  }

  private fun project(
    data: EventData,
    fetchedAt: Instant,
    source: UpcomingMatchesSnapshot,
    now: Instant,
    zone: TimeZone,
  ): EventContent =
    EventWidgetRules.project(data, fetchedAt, source.favorites.teamIds.toSet(), source.spoilersHidden, now, zone)
}
