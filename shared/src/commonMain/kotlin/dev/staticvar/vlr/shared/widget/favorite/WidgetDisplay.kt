/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

enum class SetupReason { Choose, NoFavorites }
enum class ResultTone { Win, Loss }
enum class PillTone { Won, Lost, Live, Next }

sealed interface WidgetTap {
  sealed interface Entity : WidgetTap
  data class Match(val id: String) : Entity
  data class Event(val id: String) : Entity
  data class Team(val id: String) : Entity
  data object Chooser : WidgetTap
  data object App : WidgetTap
}

sealed interface LogoMark {
  data class Named(val name: String, val tag: String?, val source: String?, val monogram: String) : LogoMark
  data object Undecided : LogoMark {
    val monogram: String = "?"
  }
}

data class StageText(val stage: String, val format: String?)
data class PillText(val text: String, val tone: PillTone)
data class FormText(val mark: FormMark, val label: String)
data class NeutralFormText(val mark: NeutralFormMark, val label: String)

data class SetupText(
  val title: String,
  val subtitle: String,
  val lead: String,
  val action: String,
  val instruction: String,
  val ghost: String,
)

data class PlaceholderText(val title: String, val value: String, val ghost: String)
data class TeamHeaderText(val team: LogoMark.Named, val title: String, val ranking: String)
data class TeamPresentation(val header: TeamHeaderText, val body: TeamDisplayBody)

sealed interface FavoriteWidgetFrame {
  val tap: WidgetTap
  val logoSources: List<String>

  data class Setup(val kind: WidgetKind, val reason: SetupReason, val text: SetupText) : FavoriteWidgetFrame {
    override val tap: WidgetTap
      get() = when (reason) {
        SetupReason.Choose -> WidgetTap.Chooser
        SetupReason.NoFavorites -> WidgetTap.App
      }
    override val logoSources: List<String> = emptyList()
  }

  data class Placeholder(val key: WidgetKey?, val text: PlaceholderText, override val tap: WidgetTap) :
    FavoriteWidgetFrame {
    override val logoSources: List<String> = emptyList()
  }

  data class Team(
    val key: WidgetKey,
    val medium: TeamPresentation,
    val large: TeamPresentation,
    override val tap: WidgetTap,
    override val logoSources: List<String>,
  ) : FavoriteWidgetFrame

  data class Event(
    val key: WidgetKey,
    val medium: EventPresentation,
    val large: EventPresentation,
    override val tap: WidgetTap,
    override val logoSources: List<String>,
  ) : FavoriteWidgetFrame
}

sealed interface TeamDisplayBody {
  data class Visible(val hero: TeamHeroText, val form: List<FormText>, val detail: TeamDetail?) : TeamDisplayBody
  data class Hidden(
    val hero: NeutralTeamHeroText,
    val form: List<NeutralFormText>,
    val history: NeutralTeamHistoryText?,
  ) : TeamDisplayBody
}

sealed interface TeamHeroText {
  val lead: String
  val value: String
  val opponent: LogoMark?
  val tap: WidgetTap.Entity?

  data class Upcoming(
    override val lead: String,
    override val value: String,
    val context: String,
    override val opponent: LogoMark,
    val day: String?,
    override val tap: WidgetTap.Entity?,
  ) : TeamHeroText

  data class Live(
    override val lead: String,
    override val value: String,
    override val opponent: LogoMark?,
    val liveLabel: String,
    val below: TeamLiveBelow,
    override val tap: WidgetTap.Entity?,
  ) : TeamHeroText

  data class Idle(
    override val lead: String,
    override val value: String,
    val context: String,
    override val opponent: LogoMark?,
    override val tap: WidgetTap.Entity?,
  ) : TeamHeroText
}

sealed interface TeamLiveBelow {
  data class Pills(val items: List<PillText>) : TeamLiveBelow
  data class Context(val text: String) : TeamLiveBelow
}

sealed interface NeutralTeamHeroText {
  val lead: String
  val value: String
  val context: String
  val opponent: LogoMark?
  val tap: WidgetTap.Entity?

  data class Upcoming(
    override val lead: String,
    override val value: String,
    override val context: String,
    override val opponent: LogoMark,
    val day: String?,
    override val tap: WidgetTap.Entity?,
  ) : NeutralTeamHeroText

  data class Live(
    override val lead: String,
    override val value: String,
    override val context: String,
    override val opponent: LogoMark?,
    val liveLabel: String,
    override val tap: WidgetTap.Entity?,
  ) : NeutralTeamHeroText

  data class Idle(
    override val lead: String,
    override val value: String,
    override val context: String,
    override val opponent: LogoMark?,
    override val tap: WidgetTap.Entity?,
  ) : NeutralTeamHeroText
}

sealed interface TeamDetail {
  data class Maps(val heading: String, val rows: List<MapRowText>, val compact: Boolean) : TeamDetail
  data class History(val heading: String, val rows: List<TeamRowText>) : TeamDetail
}

data class MapRowText(val name: String, val subtitle: String?, val score: String, val tone: PillTone, val tap: WidgetTap.Entity?)
data class TeamRowText(
  val opponent: LogoMark.Named,
  val name: String,
  val date: String,
  val mark: String,
  val score: String,
  val tone: ResultTone,
  val tap: WidgetTap.Entity?,
)

data class NeutralTeamRowText(val opponent: LogoMark.Named, val name: String, val date: String, val tap: WidgetTap.Entity?)
data class NeutralTeamHistoryText(val heading: String, val rows: List<NeutralTeamRowText>)

data class LogoPair(val left: LogoMark, val right: LogoMark)
data class EventHeaderText(
  val event: LogoMark.Named,
  val title: String,
  val subtitle: String,
  val progress: Float?,
  val live: Boolean,
)
data class EventPresentation(val header: EventHeaderText, val body: EventDisplayBody)

sealed interface EventDisplayBody {
  data class Visible(val state: VisibleEventText) : EventDisplayBody
  data class Hidden(val state: HiddenEventText) : EventDisplayBody
}

data class ScheduledEventText(
  val lead: String,
  val day: String?,
  val time: String,
  val context: String,
  val pair: LogoPair,
  val tap: WidgetTap.Entity?,
)
data class GapText(val lead: String, val value: String, val context: String)
data class EndedText(val lead: String, val date: String, val context: String)
data class NeutralLiveText(
  val lead: String,
  val label: String,
  val context: String,
  val liveLabel: String,
  val pair: LogoPair,
  val tap: WidgetTap.Entity?,
)
data class LiveEventText(
  val lead: String,
  val score: String,
  val liveLabel: String,
  val otherLive: String?,
  val beside: StageText,
  val pills: List<PillText>,
  val pair: LogoPair,
  val tap: WidgetTap.Entity?,
)
data class VictoryText(val lead: String, val score: String, val context: String, val pair: LogoPair, val tap: WidgetTap.Entity?)

sealed interface FinishedEventText {
  data class Champion(
    val label: String,
    val name: String,
    val context: String,
    val logo: LogoMark.Named,
    val tap: WidgetTap.Entity?,
  ) : FinishedEventText
  data class Unconfirmed(val ended: EndedText) : FinishedEventText
}

sealed interface VisibleEventText {
  data class Scheduled(val hero: ScheduledEventText, val list: UpcomingListText?) : VisibleEventText
  data class Live(val hero: LiveEventText, val list: MatchListText?) : VisibleEventText
  data class Between(val hero: VictoryText, val list: MatchListText?) : VisibleEventText
  data class Gap(val hero: GapText, val list: MatchListText?) : VisibleEventText
  data class Finished(val hero: FinishedEventText, val list: FinishedListText?) : VisibleEventText
}

sealed interface HiddenEventText {
  data class Scheduled(val hero: ScheduledEventText, val list: NeutralListText?) : HiddenEventText
  data class Live(val hero: NeutralLiveText, val list: NeutralListText?) : HiddenEventText
  data class Gap(val hero: GapText, val list: NeutralListText?) : HiddenEventText
  data class Finished(val hero: EndedText, val list: NeutralListText?) : HiddenEventText
}

enum class LoserSide { Left, Right }
data class MatchRowText(
  val day: String,
  val time: String,
  val left: LogoMark,
  val right: LogoMark,
  val leftName: String,
  val rightName: String,
  val round: String,
  val leftScore: String?,
  val rightScore: String?,
  val loser: LoserSide?,
  val live: Boolean,
  val tap: WidgetTap.Entity?,
)
data class NeutralRowText(
  val day: String,
  val time: String,
  val left: LogoMark,
  val right: LogoMark,
  val leftName: String,
  val rightName: String,
  val round: String,
  val live: Boolean,
  val tap: WidgetTap.Entity?,
)
data class MatchListText(val heading: String, val rows: List<MatchRowText>)
data class NeutralListText(val heading: String, val rows: List<NeutralRowText>)
data class GroupRowText(
  val rank: String,
  val team: LogoMark.Named,
  val record: String,
  val maps: String,
  val favorite: Boolean,
  val tap: WidgetTap.Entity?,
)
data class PrizeRowText(val position: String, val team: LogoMark.Named, val amount: String, val tap: WidgetTap.Entity?)

sealed interface UpcomingListText {
  data class Matches(val value: MatchListText) : UpcomingListText
  data class Group(val heading: String, val rows: List<GroupRowText>) : UpcomingListText
}
sealed interface FinishedListText {
  data class Matches(val value: MatchListText) : FinishedListText
  data class Prizes(val heading: String, val rows: List<PrizeRowText>) : FinishedListText
}
