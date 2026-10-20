/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

enum class SetupReason { Choose, NoFavorites }
enum class ResultTone { Win, Loss }
enum class PillTone { Won, Lost, Live, Next }

sealed interface WidgetTap {
  data class Match(val id: String) : WidgetTap
  data class Event(val id: String) : WidgetTap
  data object Chooser : WidgetTap
  data object App : WidgetTap
}

sealed interface LogoMark {
  data class Named(val name: String, val tag: String?, val source: String?, val monogram: String) : LogoMark
  data object Undecided : LogoMark {
    val monogram: String = "?"
  }
}

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

  data class Upcoming(
    override val lead: String,
    override val value: String,
    val context: String,
    override val opponent: LogoMark,
    val day: String?,
  ) : TeamHeroText

  data class Live(
    override val lead: String,
    override val value: String,
    override val opponent: LogoMark?,
    val liveLabel: String,
    val below: TeamLiveBelow,
  ) : TeamHeroText

  data class Idle(
    override val lead: String,
    override val value: String,
    val context: String,
    override val opponent: LogoMark?,
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

  data class Upcoming(
    override val lead: String,
    override val value: String,
    override val context: String,
    override val opponent: LogoMark,
    val day: String?,
  ) : NeutralTeamHeroText

  data class Live(
    override val lead: String,
    override val value: String,
    override val context: String,
    override val opponent: LogoMark?,
    val liveLabel: String,
  ) : NeutralTeamHeroText

  data class Idle(
    override val lead: String,
    override val value: String,
    override val context: String,
    override val opponent: LogoMark?,
  ) : NeutralTeamHeroText
}

sealed interface TeamDetail {
  data class Maps(val heading: String, val rows: List<MapRowText>, val compact: Boolean) : TeamDetail
  data class History(val heading: String, val rows: List<TeamRowText>) : TeamDetail
}

data class MapRowText(val name: String, val subtitle: String?, val score: String, val tone: PillTone)
data class TeamRowText(
  val opponent: LogoMark.Named,
  val name: String,
  val date: String,
  val mark: String,
  val score: String,
  val tone: ResultTone,
)

data class NeutralTeamRowText(val opponent: LogoMark.Named, val name: String, val date: String)
data class NeutralTeamHistoryText(val heading: String, val rows: List<NeutralTeamRowText>)
