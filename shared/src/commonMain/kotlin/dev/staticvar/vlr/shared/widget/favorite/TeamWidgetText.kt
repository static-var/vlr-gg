/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.datetime.TimeZone
import kotlin.time.Instant

internal object TeamWidgetText {
  fun setup(reason: SetupReason, locale: WidgetLocale): FavoriteWidgetFrame.Setup = FavoriteWidgetFrame.Setup(
    WidgetKind.Team,
    reason,
    SetupText(
      title = locale.text(
        if (reason ==
          SetupReason.Choose
        ) {
          WidgetTextKey.ChooseTeam
        } else {
          WidgetTextKey.NoFavoriteTeams
        },
      ),
      subtitle = "Val Esports",
      lead = locale.text(WidgetTextKey.ToUseWidget),
      action = locale.text(if (reason == SetupReason.Choose) WidgetTextKey.TapToChoose else WidgetTextKey.OpenApp),
      instruction = locale.text(
        if (reason ==
          SetupReason.Choose
        ) {
          WidgetTextKey.TeamPick
        } else {
          WidgetTextKey.TeamFavoriteInstruction
        },
      ),
      ghost = locale.text(WidgetTextKey.TeamGhost),
    ),
  )

  fun placeholder(key: WidgetKey?, locale: WidgetLocale): FavoriteWidgetFrame.Placeholder =
    FavoriteWidgetFrame.Placeholder(
      key,
      PlaceholderText(
        locale.text(WidgetTextKey.TeamTitle),
        locale.text(WidgetTextKey.LoadingTeam),
        locale.text(WidgetTextKey.TeamGhost),
      ),
      WidgetTap.App,
    )

  fun frame(
    key: WidgetKey,
    content: TeamContent,
    hidden: Boolean,
    now: Instant,
    zone: TimeZone,
    locale: WidgetLocale,
  ): FavoriteWidgetFrame.Team {
    val tap = teamTap(content.header.team) ?: WidgetTap.App
    if (hidden) {
      val neutral = TeamWidgetRules.hide(content)
      return FavoriteWidgetFrame.Team(
        key,
        presentation(neutral, large = false, now, zone, locale),
        presentation(neutral, large = true, now, zone, locale),
        tap,
        TeamWidgetRules.logoSources(neutral).distinct(),
      )
    }
    return FavoriteWidgetFrame.Team(
      key,
      presentation(content, large = false, now, zone, locale),
      presentation(content, large = true, now, zone, locale),
      tap,
      TeamWidgetRules.logoSources(content).distinct(),
    )
  }

  private fun presentation(
    content: TeamContent,
    large: Boolean,
    now: Instant,
    zone: TimeZone,
    locale: WidgetLocale,
  ): TeamPresentation = TeamPresentation(
    header(content.header, large, locale),
    TeamDisplayBody.Visible(
      hero(content.state, large, now, zone, locale),
      content.form.map { FormText(it, formLabel(it, locale)) },
      if (large) detail(content, locale) else null,
    ),
  )

  private fun presentation(
    content: HiddenTeamContent,
    large: Boolean,
    now: Instant,
    zone: TimeZone,
    locale: WidgetLocale,
  ): TeamPresentation {
    val rows = if (large) {
      TeamWidgetRules.history(content).map {
        NeutralTeamRowText(logoMark(it.opponent), it.opponent.name, dayText(it.day, locale), WidgetTap.Match(it.id))
      }
    } else {
      emptyList()
    }
    return TeamPresentation(
      header(content.header, large, locale),
      TeamDisplayBody.Hidden(
        hero(content.state, now, zone, locale),
        content.form.map {
          NeutralFormText(
            it,
            if (it ==
              NeutralFormMark.Live
            ) {
              locale.text(WidgetTextKey.Live)
            } else {
              ""
            },
          )
        },
        rows.takeIf { it.isNotEmpty() }?.let {
          NeutralTeamHistoryText(
            locale.text(if (content.state is HiddenTeamState.Idle) WidgetTextKey.BeforeThat else WidgetTextKey.Recent),
            it,
          )
        },
      ),
    )
  }

  private fun header(value: TeamHeader, large: Boolean, locale: WidgetLocale): TeamHeaderText {
    val parts = listOfNotNull(
      value.rank?.let { locale.text(WidgetTextKey.WorldwideRank, locale.integer(it)) },
      if (large && value.regionRank != null && value.region != null) {
        locale.text(WidgetTextKey.RegionalRank, locale.integer(value.regionRank), value.region)
      } else {
        null
      },
    )
    return TeamHeaderText(logoMark(value.team), value.team.name, parts.joinToString(" · "))
  }

  private fun hero(
    state: TeamState,
    large: Boolean,
    now: Instant,
    zone: TimeZone,
    locale: WidgetLocale,
  ): TeamHeroText = when (state) {
    is TeamState.Upcoming -> upcoming(state.fixture, now, zone, locale)

    is TeamState.Live -> {
      val pills = if (large) {
        emptyList()
      } else {
        WidgetSeriesRules.pills(state.maps, locale).map {
          PillText(it.text, pillTone(it.status))
        }
      }
      TeamHeroText.Live(
        lead = locale.text(WidgetTextKey.Playing, opponentName(state.fixture, locale)),
        value = state.score?.text ?: "–",
        opponent = state.fixture.opponent?.let(::logoMark),
        liveLabel = locale.text(WidgetTextKey.Live),
        below = if (pills.isEmpty()) TeamLiveBelow.Context(context(state.fixture)) else TeamLiveBelow.Pills(pills),
        tap = WidgetTap.Match(state.fixture.id),
      )
    }

    is TeamState.Idle -> state.latest?.let {
      TeamHeroText.Idle(
        lead = locale.text(
          if (it.result ==
            ResultKind.Win
          ) {
            WidgetTextKey.Beat
          } else {
            WidgetTextKey.LostTo
          },
          it.opponent.name,
        ),
        value = it.score?.text ?: "–",
        context = dotJoin(dayText(it.day, locale), locale.text(WidgetTextKey.NoMatchScheduled)),
        opponent = logoMark(it.opponent),
        tap = WidgetTap.Match(it.id),
      )
    } ?: emptyHero(locale)
  }

  private fun hero(state: HiddenTeamState, now: Instant, zone: TimeZone, locale: WidgetLocale): NeutralTeamHeroText =
    when (state) {
      is HiddenTeamState.Upcoming -> upcoming(state.fixture, now, zone, locale).let {
        NeutralTeamHeroText.Upcoming(it.lead, it.value, it.context, it.opponent, day = it.day, tap = it.tap)
      }

      is HiddenTeamState.Live -> NeutralTeamHeroText.Live(
        lead = locale.text(WidgetTextKey.Playing, opponentName(state.fixture, locale)),
        value = locale.text(WidgetTextKey.LiveNow),
        context = dotJoin(locale.text(WidgetTextKey.ScoresHidden), state.fixture.format?.label),
        liveLabel = locale.text(WidgetTextKey.Live),
        opponent = state.fixture.opponent?.let(::logoMark),
        tap = WidgetTap.Match(state.fixture.id),
      )

      is HiddenTeamState.Idle -> state.latest?.let {
        NeutralTeamHeroText.Idle(
          locale.text(WidgetTextKey.LastPlayed, it.opponent.name),
          dayText(it.day, locale),
          locale.text(WidgetTextKey.HiddenNoMatchScheduled),
          logoMark(it.opponent),
          WidgetTap.Match(it.id),
        )
      } ?: emptyHero(locale).let { NeutralTeamHeroText.Idle(it.lead, it.value, it.context, null, null) }
    }

  private fun upcoming(
    fixture: TeamFixture,
    now: Instant,
    zone: TimeZone,
    locale: WidgetLocale,
  ): TeamHeroText.Upcoming {
    val kickoff = WidgetTemporalRules.kickoff(fixture.start, now, zone, locale)
    return TeamHeroText.Upcoming(
      lead = locale.text(WidgetTextKey.Plays, opponentName(fixture, locale)),
      day = (kickoff as? Kickoff.Scheduled)?.day,
      value = kickoff.hero(locale),
      context = if (kickoff is Kickoff.Delayed) {
        dotJoin(
          locale.text(WidgetTextKey.WasDue, kickoff.originalTime),
          fixture.event,
        )
      } else {
        context(fixture)
      },
      opponent = fixture.opponent?.let(::logoMark) ?: LogoMark.Undecided,
      tap = WidgetTap.Match(fixture.id),
    )
  }

  private fun detail(content: TeamContent, locale: WidgetLocale): TeamDetail? {
    val live = content.state as? TeamState.Live
    val maps = live?.maps.orEmpty()
    if (live != null && maps.isNotEmpty()) {
      val compact = maps.size > 3
      return TeamDetail.Maps(
        locale.text(WidgetTextKey.Maps),
        maps.take(5).map {
          MapRowText(
            it.name,
            if (compact) null else WidgetSeriesRules.mapSubtitle(it, locale),
            WidgetSeriesRules.mapScoreText(it, locale),
            pillTone(it.status),
            WidgetTap.Match(live.fixture.id),
          )
        },
        compact,
      )
    }
    val rows = TeamWidgetRules.history(content).map {
      TeamRowText(
        logoMark(it.opponent),
        it.opponent.name,
        dayText(it.day, locale),
        locale.text(if (it.result == ResultKind.Win) WidgetTextKey.WinMark else WidgetTextKey.LossMark),
        it.score?.text ?: "–",
        if (it.result == ResultKind.Win) ResultTone.Win else ResultTone.Loss,
        WidgetTap.Match(it.id),
      )
    }
    return rows.takeIf { it.isNotEmpty() }?.let {
      TeamDetail.History(
        locale.text(if (content.state is TeamState.Idle) WidgetTextKey.BeforeThat else WidgetTextKey.Recent),
        it,
      )
    }
  }

  private fun emptyHero(locale: WidgetLocale): TeamHeroText.Idle = TeamHeroText.Idle(
    locale.text(WidgetTextKey.SeasonStartsSoon),
    locale.text(WidgetTextKey.NoMatches),
    locale.text(WidgetTextKey.MatchNotListed),
    null,
    null,
  )

  private fun formLabel(mark: FormMark, locale: WidgetLocale): String = when (mark) {
    FormMark.Win -> locale.text(WidgetTextKey.Win)
    FormMark.Loss -> locale.text(WidgetTextKey.Loss)
    FormMark.Live -> locale.text(WidgetTextKey.Live)
    FormMark.Empty -> ""
  }

  private fun context(fixture: TeamFixture): String = dotJoin(fixture.event, fixture.stage, fixture.format?.label)
  private fun opponentName(fixture: TeamFixture, locale: WidgetLocale): String =
    fixture.opponent?.name ?: locale.text(WidgetTextKey.Tbd)
}
