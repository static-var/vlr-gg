/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
enum class FormMark {
  @SerialName("Win")
  Win,

  @SerialName("Loss")
  Loss,

  @SerialName("Empty")
  Empty,

  @SerialName("Live")
  Live,
}

enum class NeutralFormMark { Neutral, Live }

@Serializable
internal data class TeamHeader(val team: WidgetTeam, val rank: Int?, val region: String?, val regionRank: Int?)

@Serializable
internal enum class ResultKind {
  @SerialName("Win")
  Win,

  @SerialName("Loss")
  Loss,
}

@Serializable
internal data class TeamResult(
  val id: String,
  val day: LocalDate?,
  val opponent: WidgetTeam,
  val result: ResultKind,
  val score: WidgetScore?,
)

internal data class NeutralTeamResult(val id: String, val day: LocalDate?, val opponent: WidgetTeam)

@Serializable
internal data class TeamFixture(
  val id: String,
  val opponent: WidgetTeam?,
  val start: Instant?,
  val event: String,
  val stage: String,
  val format: WidgetSeriesFormat?,
)

@Serializable
internal sealed interface TeamState {
  @Serializable
  @SerialName("upcoming")
  data class Upcoming(val fixture: TeamFixture) : TeamState

  @Serializable
  @SerialName("live")
  data class Live(val fixture: TeamFixture, val score: WidgetScore?, val maps: List<WidgetMap>) : TeamState

  @Serializable
  @SerialName("idle")
  data class Idle(val latest: TeamResult?) : TeamState
}

@Serializable
internal data class TeamContent(
  val header: TeamHeader,
  val form: List<FormMark>,
  val history: List<TeamResult>,
  val state: TeamState,
) : FavoriteWidgetContent

internal sealed interface HiddenTeamState {
  data class Upcoming(val fixture: TeamFixture) : HiddenTeamState

  data class Live(val fixture: TeamFixture) : HiddenTeamState

  data class Idle(val latest: NeutralTeamResult?) : HiddenTeamState
}

internal data class HiddenTeamContent(
  val header: TeamHeader,
  val form: List<NeutralFormMark>,
  val history: List<NeutralTeamResult>,
  val state: HiddenTeamState,
)
