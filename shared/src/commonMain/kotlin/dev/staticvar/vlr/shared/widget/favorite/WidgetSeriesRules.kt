/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.serialization.SerialName

internal data class WidgetTeam(val id: String?, val name: String, val tag: String? = null, val logo: String? = null)

internal sealed interface WidgetParticipant {

  data class Team(val value: WidgetTeam) : WidgetParticipant

  data object Undecided : WidgetParticipant

  val team: WidgetTeam?
    get() = (this as? Team)?.value
}

internal data class WidgetMatchPair(val left: WidgetParticipant, val right: WidgetParticipant) {
  val teams: List<WidgetTeam>
    get() = listOfNotNull(left.team, right.team)
}

internal enum class WidgetSide { Left, Right }

internal data class WidgetScore(val team: Int, val opponent: Int) {
  val text: String
    get() = "$team–$opponent"
}

internal enum class WidgetMapStatus {
  @SerialName("Won")
  Won,
  @SerialName("Lost")
  Lost,
  @SerialName("Live")
  Live,
  @SerialName("Next")
  Next,
}

internal sealed interface WidgetMapPick {

  data class Team(val tag: String) : WidgetMapPick

  data class Opponent(val tag: String) : WidgetMapPick

  data object Decider : WidgetMapPick
}

internal data class WidgetMap(
  val name: String,
  val pick: WidgetMapPick,
  val status: WidgetMapStatus,
  val score: WidgetScore?,
)

internal enum class WidgetSeriesFormat(val mapCount: Int) {
  @SerialName("Bo1")
  Bo1(1),
  @SerialName("Bo3")
  Bo3(3),
  @SerialName("Bo5")
  Bo5(5),
  ;

  val label: String
    get() = "Bo$mapCount"
}

internal data class WidgetSeriesEnrichment(val format: WidgetSeriesFormat?, val maps: List<WidgetMap>)

internal data class WidgetMapPill(val text: String, val status: WidgetMapStatus)

internal object WidgetSeriesRules {
  fun format(details: MatchDetails?, preview: String?): WidgetSeriesFormat? {
    val count = details?.totalMaps ?: details?.mapCount
    WidgetSeriesFormat.entries.firstOrNull { it.mapCount == count }?.let { return it }
    val text = preview.orEmpty().replace(" ", "").lowercase()
    if (!text.startsWith("bo")) return null
    val previewCount = text.drop(2).toIntOrNull() ?: return null
    return WidgetSeriesFormat.entries.firstOrNull { it.mapCount == previewCount }
  }

  fun make(
    details: MatchDetails?,
    participants: WidgetMatchPair,
    perspective: WidgetSide,
    previewCurrent: CurrentMap? = null,
    previewFormat: String? = null,
    ownAliases: List<String> = emptyList(),
  ): WidgetSeriesEnrichment = WidgetSeriesEnrichment(
    format = format(details, previewFormat),
    maps = maps(details, participants, perspective, previewCurrent, ownAliases),
  )

  fun maps(
    details: MatchDetails?,
    participants: WidgetMatchPair,
    perspective: WidgetSide,
    previewCurrent: CurrentMap? = null,
    ownAliases: List<String> = emptyList(),
  ): List<WidgetMap> {
    if (details == null) return emptyList()
    val ownFirst = perspective == WidgetSide.Left
    val team = (if (ownFirst) participants.left else participants.right).team ?: WidgetTeam(null, "TBD")
    val opponent = (if (ownFirst) participants.right else participants.left).team
    val veto = details.veto.filter { it.action.lowercase() in listOf("pick", "remains") }
    val names = mutableListOf<String>()
    for (name in details.maps.map { it.name } + veto.map { it.map }) {
      if (nameKey(name).isEmpty() || nameKey(name) == "all" || names.any { nameKey(it) == nameKey(name) }) continue
      names += name.trim()
    }
    val current = details.currentMap ?: previewCurrent
    val previewIndex = if (ownFirst) 0 else 1
    val currentTeamIndex = if (details.currentMap == null) previewIndex else index(details.teams, team, previewIndex)
    if (current != null && nameKey(current.name).isNotEmpty() && names.none { nameKey(it) == nameKey(current.name) }) {
      names += current.name.trim()
    }
    return names.map { name ->
      val map = details.maps.firstOrNull { nameKey(it.name) == nameKey(name) }
      val pick = veto.firstOrNull { nameKey(it.map) == nameKey(name) }
      val choice = if (pick != null && pick.action.lowercase() == "pick" && pick.team != null) {
        val ownNames = listOfNotNull(team.id, team.name, team.tag) + ownAliases
        if (ownNames.any { it.isNotEmpty() && it.equals(pick.team, ignoreCase = true) }) {
          WidgetMapPick.Team(team.tag ?: team.name)
        } else {
          WidgetMapPick.Opponent(opponent?.tag ?: pick.team)
        }
      } else {
        WidgetMapPick.Decider
      }
      val orderedTeams = map?.teams.orEmpty()
      val ownIndex = index(orderedTeams, team, previewIndex)
      val otherIndex = if (ownIndex == 0) 1 else 0
      var scores = score(orderedTeams.getOrNull(ownIndex)?.score, orderedTeams.getOrNull(otherIndex)?.score)
      val isCurrent = current != null && nameKey(current.name) == nameKey(name)
      val live = map?.live == true || (isCurrent && map?.live != false)
      if (live && isCurrent && current.scores.size == 2 && currentTeamIndex in 0..1) {
        val currentScore = score(current.scores[currentTeamIndex], current.scores[1 - currentTeamIndex])
        if (currentScore != null) scores = currentScore
      }
      val status = when {
        live -> WidgetMapStatus.Live

        scores != null && scores.team != scores.opponent && maxOf(scores.team, scores.opponent) >= 13 ->
          if (scores.team > scores.opponent) WidgetMapStatus.Won else WidgetMapStatus.Lost

        else -> WidgetMapStatus.Next
      }
      WidgetMap(name, choice, status, scores.takeUnless { status == WidgetMapStatus.Next })
    }
  }

  fun visibleMaps(maps: List<WidgetMap>): List<WidgetMap> {
    val firstNext = maps.indexOfFirst { it.status == WidgetMapStatus.Next }
    return maps.filterIndexed { index, map -> map.status != WidgetMapStatus.Next || index == firstNext }
  }

  fun pillText(map: WidgetMap, compact: Boolean, locale: WidgetLocale): String = when {
    map.status == WidgetMapStatus.Next -> locale.text(WidgetTextKey.MapNext, map.name)
    compact && map.status != WidgetMapStatus.Live -> map.score?.text ?: "–"
    else -> map.name + " " + (map.score?.text ?: "–")
  }

  fun pills(maps: List<WidgetMap>, locale: WidgetLocale): List<WidgetMapPill> {
    val visible = visibleMaps(maps)
    return visible.map { WidgetMapPill(pillText(it, compact = visible.size >= 4, locale = locale), it.status) }
  }

  fun mapSubtitle(map: WidgetMap, locale: WidgetLocale): String {
    val pick = when (val choice = map.pick) {
      is WidgetMapPick.Team -> locale.text(WidgetTextKey.MapPick, choice.tag)
      is WidgetMapPick.Opponent -> locale.text(WidgetTextKey.MapPick, choice.tag)
      WidgetMapPick.Decider -> locale.text(WidgetTextKey.Decider)
    }
    val status = when (map.status) {
      WidgetMapStatus.Won -> WidgetTextKey.Won
      WidgetMapStatus.Lost -> WidgetTextKey.Lost
      WidgetMapStatus.Live -> WidgetTextKey.PlayingNow
      WidgetMapStatus.Next -> WidgetTextKey.Next
    }
    return pick + " · " + locale.text(status)
  }

  fun mapScoreText(map: WidgetMap, locale: WidgetLocale): String = map.score?.text ?: locale.text(WidgetTextKey.Next)

  private fun index(values: List<RemoteTeam>, team: WidgetTeam, fallback: Int): Int {
    if (!team.id.isNullOrEmpty()) {
      val identity = values.indexOfFirst { it.id == team.id }
      if (identity >= 0) return identity
    }
    return values.indexOfFirst { nameKey(it.name) == nameKey(team.name) }.takeIf { it >= 0 } ?: fallback
  }

  private fun score(team: Int?, opponent: Int?): WidgetScore? =
    if (team != null && opponent != null) WidgetScore(team, opponent) else null
}
