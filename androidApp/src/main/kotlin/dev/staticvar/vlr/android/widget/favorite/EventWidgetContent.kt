/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import dev.staticvar.vlr.shared.widget.favorite.EndedText
import dev.staticvar.vlr.shared.widget.favorite.EventDisplayBody
import dev.staticvar.vlr.shared.widget.favorite.EventPresentation
import dev.staticvar.vlr.shared.widget.favorite.FinishedEventText
import dev.staticvar.vlr.shared.widget.favorite.FinishedListText
import dev.staticvar.vlr.shared.widget.favorite.GapText
import dev.staticvar.vlr.shared.widget.favorite.GroupRowText
import dev.staticvar.vlr.shared.widget.favorite.HiddenEventText
import dev.staticvar.vlr.shared.widget.favorite.LogoMark
import dev.staticvar.vlr.shared.widget.favorite.LogoPair
import dev.staticvar.vlr.shared.widget.favorite.LoserSide
import dev.staticvar.vlr.shared.widget.favorite.MatchListText
import dev.staticvar.vlr.shared.widget.favorite.NeutralListText
import dev.staticvar.vlr.shared.widget.favorite.PillText
import dev.staticvar.vlr.shared.widget.favorite.PrizeRowText
import dev.staticvar.vlr.shared.widget.favorite.ScheduledEventText
import dev.staticvar.vlr.shared.widget.favorite.StageText
import dev.staticvar.vlr.shared.widget.favorite.UpcomingListText
import dev.staticvar.vlr.shared.widget.favorite.VisibleEventText
import dev.staticvar.vlr.shared.widget.favorite.WidgetTap

internal data class EventWidgetHero(
  val lead: String,
  val value: String,
  val context: String? = null,
  val day: String? = null,
  val liveLabel: String? = null,
  val otherLive: String? = null,
  val beside: StageText? = null,
  val pills: List<PillText> = emptyList(),
  val pair: LogoPair? = null,
  val champion: LogoMark.Named? = null,
  val tap: WidgetTap.Entity? = null,
)

internal sealed interface EventWidgetList {
  val heading: String
  val count: Int
  data class Matches(override val heading: String, val rows: List<EventWidgetMatch>) : EventWidgetList {
    override val count: Int get() = rows.size
  }
  data class Group(override val heading: String, val rows: List<GroupRowText>) : EventWidgetList {
    override val count: Int get() = rows.size
  }
  data class Prizes(override val heading: String, val rows: List<PrizeRowText>) : EventWidgetList {
    override val count: Int get() = rows.size
  }
  data class Ghost(override val heading: String) : EventWidgetList {
    override val count: Int = 3
  }
}

internal data class EventWidgetMatch(
  val day: String,
  val time: String,
  val left: LogoMark,
  val right: LogoMark,
  val leftName: String,
  val rightName: String,
  val round: String,
  val leftScore: String? = null,
  val rightScore: String? = null,
  val loser: LoserSide? = null,
  val live: Boolean,
  val tap: WidgetTap.Entity?,
)

internal data class EventWidgetContent(val hero: EventWidgetHero, val list: EventWidgetList?)

internal fun EventPresentation.content(): EventWidgetContent = when (val body = body) {
  is EventDisplayBody.Visible -> when (val state = body.state) {
    is VisibleEventText.Scheduled -> EventWidgetContent(
      state.hero.content(),
      when (val list = state.list) {
        is UpcomingListText.Group -> EventWidgetList.Group(list.heading, list.rows)
        is UpcomingListText.Matches -> list.value.content()
        null -> null
      },
    )

    is VisibleEventText.Live -> EventWidgetContent(
      state.hero.let {
        EventWidgetHero(
          it.lead,
          it.score,
          liveLabel = it.liveLabel,
          otherLive = it.otherLive,
          beside = it.beside,
          pills = it.pills,
          pair = it.pair,
          tap = it.tap,
        )
      },
      state.list?.content(),
    )

    is VisibleEventText.Between -> EventWidgetContent(
      state.hero.let {
        EventWidgetHero(it.lead, it.score, it.context, pair = it.pair, tap = it.tap)
      },
      state.list?.content(),
    )

    is VisibleEventText.Gap -> EventWidgetContent(state.hero.content(), state.list?.content())

    is VisibleEventText.Finished -> EventWidgetContent(
      when (val hero = state.hero) {
        is FinishedEventText.Champion -> EventWidgetHero(hero.label, hero.name, hero.context, champion = hero.logo, tap = hero.tap)
        is FinishedEventText.Unconfirmed -> hero.ended.content()
      },
      when (val list = state.list) {
        is FinishedListText.Matches -> list.value.content()
        is FinishedListText.Prizes -> EventWidgetList.Prizes(list.heading, list.rows)
        null -> null
      },
    )
  }

  is EventDisplayBody.Hidden -> when (val state = body.state) {
    is HiddenEventText.Scheduled -> EventWidgetContent(state.hero.content(), state.list?.content())

    is HiddenEventText.Live -> EventWidgetContent(
      state.hero.let {
        EventWidgetHero(it.lead, it.label, it.context, liveLabel = it.liveLabel, pair = it.pair, tap = it.tap)
      },
      state.list?.content(),
    )

    is HiddenEventText.Gap -> EventWidgetContent(state.hero.content(), state.list?.content())

    is HiddenEventText.Finished -> EventWidgetContent(state.hero.content(), state.list?.content())
  }
}

private fun ScheduledEventText.content() = EventWidgetHero(lead, time, context, day, pair = pair, tap = tap)
private fun GapText.content() = EventWidgetHero(lead, value, context)
private fun EndedText.content() = EventWidgetHero(lead, date, context)
private fun MatchListText.content() = EventWidgetList.Matches(
  heading,
  rows.map {
    EventWidgetMatch(
      it.day, it.time, it.left, it.right, it.leftName, it.rightName, it.round,
      it.leftScore, it.rightScore, it.loser, it.live, it.tap,
    )
  },
)
private fun NeutralListText.content() = EventWidgetList.Matches(
  heading,
  rows.map {
    EventWidgetMatch(it.day, it.time, it.left, it.right, it.leftName, it.rightName, it.round, live = it.live, tap = it.tap)
  },
)
