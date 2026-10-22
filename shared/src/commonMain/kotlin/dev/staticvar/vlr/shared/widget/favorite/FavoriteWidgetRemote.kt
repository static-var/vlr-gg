/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import dev.staticvar.vlr.remotesource.common.ApiPaths
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.time.Instant

internal class FavoriteWidgetRemote(private val client: HttpClient, private val json: Json) {
  suspend fun team(key: WidgetKey): TeamContent = coroutineScope {
    val ranking =
      async { WidgetWireDecoders.profile(at(ApiPaths.teamRankingProfile(key.entityId.encodeURLPathPart()))) }
    val previews = async {
      WidgetWireDecoders.previews(
        at("${ApiPaths.favoriteMatches(key.clientId.encodeURLPathPart())}?include_results=false"),
      )
    }
    val profile = ranking.await()
    val preview = TeamWidgetRules.choose(key.entityId, previews.await())
    val details = if (preview == null) {
      null
    } else {
      try {
        WidgetWireDecoders.details(at(ApiPaths.match(preview.id.encodeURLPathPart())))
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        null
      }
    }
    TeamWidgetRules.content(profile.copy(team = profile.team.copy(id = key.entityId)), preview, details)
  }

  suspend fun event(key: WidgetKey, favorites: Set<String>, now: Instant, zone: TimeZone): EventData {
    val profile = WidgetWireDecoders.event(at(ApiPaths.event(key.entityId.encodeURLPathPart())), key.entityId, now)
    val data = EventWidgetMapper.normalize(profile)
    val matchId = EventWidgetRules.detailMatchId(data, favorites, now, zone) ?: return data
    val details = try {
      WidgetWireDecoders.details(at(ApiPaths.match(matchId.encodeURLPathPart())))
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      null
    }
    return EventWidgetMapper.enrich(data, matchId, details)
  }

  private suspend fun at(path: String): JsonElement {
    val response = client.get(path)
    return json.parseToJsonElement(response.bodyAsText())
  }
}
