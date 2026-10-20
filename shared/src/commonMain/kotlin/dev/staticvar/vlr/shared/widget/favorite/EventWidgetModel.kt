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
internal enum class ReportedEventStatus {
  @SerialName("Upcoming")
  Upcoming,

  @SerialName("Ongoing")
  Ongoing,

  @SerialName("Paused")
  Paused,

  @SerialName("Completed")
  Completed,

  @SerialName("Unknown")
  Unknown,
}

internal enum class EventPhase { NotStarted, InProgress, Finished }

@Serializable
internal data class EventMetadata(
  val id: String,
  val title: String,
  val logo: String?,
  val location: String,
  val prizePool: String?,
  val dates: EventDates,
  val reportedStatus: ReportedEventStatus,
)

@Serializable
internal data class EventFixture(
  val id: String,
  val participants: WidgetMatchPair,
  val start: Instant?,
  val floatingCalendarDay: LocalDate?,
  val round: String,
  val stage: String,
  val format: WidgetSeriesFormat?,
)

@Serializable
internal sealed interface EventMatchPhase {
  @Serializable
  @SerialName("upcoming")
  data object Upcoming : EventMatchPhase

  @Serializable
  @SerialName("live")
  data class Live(val score: WidgetScore?) : EventMatchPhase

  @Serializable
  @SerialName("completed")
  data class Completed(val score: WidgetScore?) : EventMatchPhase

  @Serializable
  @SerialName("unknown")
  data object Unknown : EventMatchPhase
}

@Serializable
internal data class EventMatch(val fixture: EventFixture, val phase: EventMatchPhase)

@Serializable
internal data class EventStanding(val team: WidgetTeam, val wins: Int, val losses: Int, val ties: Int, val maps: Int)

@Serializable
internal data class EventGroup(val name: String, val rows: List<EventStanding>)

@Serializable
internal data class EventPrize(val position: String, val amount: String, val team: WidgetTeam)

@Serializable
internal data class EventDetail(val matchId: String, val maps: List<WidgetMap>)

@Serializable
internal data class EventData(
  val event: EventMetadata,
  val matches: List<EventMatch>,
  val groups: List<EventGroup>,
  val prizes: List<EventPrize>,
  val detail: EventDetail?,
)

internal enum class ScheduledRole { First, Next }
internal enum class ListHeading { Today, UpNext, Earlier }
internal enum class NeutralPhase { Upcoming, Live, Ended }
internal data class EventMatchList(val heading: ListHeading, val rows: List<EventMatch>)
internal data class NeutralEventMatch(val fixture: EventFixture, val phase: NeutralPhase)
internal data class NeutralEventList(val heading: ListHeading, val rows: List<NeutralEventMatch>)
internal data class EventGroupRow(val rank: Int, val standing: EventStanding, val favorite: Boolean)

internal sealed interface EventUpcomingList {
  data class Matches(val list: EventMatchList) : EventUpcomingList
  data class Group(val name: String, val rows: List<EventGroupRow>) : EventUpcomingList
}
internal sealed interface EventFinishedList {
  data class Matches(val list: EventMatchList) : EventFinishedList
  data class Prizes(val rows: List<EventPrize>) : EventFinishedList
}
internal data class EventVictory(
  val matchId: String,
  val winner: WidgetTeam,
  val loser: WidgetTeam,
  val winnerFirstScore: WidgetScore,
  val round: String,
  val scheduledStart: Instant?,
)
internal sealed interface EventGap {
  data class NotListed(val start: LocalDate?) : EventGap
  data object NextUnlisted : EventGap
}
internal data class EventEnded(val end: LocalDate?, val prizePool: String?)
internal sealed interface EventFinish {
  data class Champion(val team: WidgetTeam, val final: EventVictory?) : EventFinish
  data class Unconfirmed(val ended: EventEnded) : EventFinish
}

internal sealed interface VisibleEventState {
  data class Scheduled(val role: ScheduledRole, val hero: EventFixture, val list: EventUpcomingList) : VisibleEventState
  data class Live(
    val hero: EventFixture,
    val score: WidgetScore?,
    val maps: List<WidgetMap>,
    val otherLive: Int,
    val list: EventMatchList,
  ) : VisibleEventState
  data class Between(val last: EventVictory, val next: EventFixture, val list: EventMatchList) : VisibleEventState
  data class Gap(val hero: EventGap, val list: EventMatchList) : VisibleEventState
  data class Finished(val hero: EventFinish, val list: EventFinishedList) : VisibleEventState
}

internal sealed interface HiddenEventState {
  data class Scheduled(val role: ScheduledRole, val hero: EventFixture, val list: NeutralEventList) : HiddenEventState
  data class Live(val hero: EventFixture, val list: NeutralEventList) : HiddenEventState
  data class Gap(val hero: EventGap, val list: NeutralEventList) : HiddenEventState
  data class Finished(val ended: EventEnded, val list: NeutralEventList) : HiddenEventState
}
internal sealed interface EventBody {
  data class Visible(val state: VisibleEventState) : EventBody
  data class Hidden(val state: HiddenEventState) : EventBody
}
internal sealed interface EventSubtitle {
  data class Starts(val day: LocalDate?, val location: String) : EventSubtitle
  data class Ended(val day: LocalDate?, val location: String) : EventSubtitle
  data class Stage(val stage: String, val location: String) : EventSubtitle
  data class Dates(val bounds: EventDates, val location: String) : EventSubtitle
}
internal data class EventContent(
  val event: EventMetadata,
  val subtitle: EventSubtitle,
  val progress: Float?,
  val hasLiveMatch: Boolean,
  val body: EventBody,
)
