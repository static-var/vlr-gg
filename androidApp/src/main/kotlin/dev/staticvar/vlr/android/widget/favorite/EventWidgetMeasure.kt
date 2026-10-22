/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetFrame

internal fun eventPresentationFor(
  frame: FavoriteWidgetFrame,
  size: DpSize,
  typography: FavoriteWidgetTypography,
): FavoriteWidgetPresentation {
  val measure = EventWidgetMeasure(frame, size, typography)
  val parts = setOf(
    FavoriteWidgetPart.Context,
    FavoriteWidgetPart.Pills,
    FavoriteWidgetPart.Progress,
    FavoriteWidgetPart.Subtitle,
    FavoriteWidgetPart.Beside,
    FavoriteWidgetPart.OtherLive,
  )
  return presentationFor(size, measure.list?.count ?: 0, measure::rowHeight, measure::height, parts, parts)
}

internal val FavoriteWidgetPresentation.pairBox: DpSize
  get() = if (compact) DpSize(48.dp, 44.dp) else DpSize(64.dp, 56.dp)
internal val FavoriteWidgetPresentation.pairLogo: Dp get() = if (compact) 28.dp else 36.dp
internal val FavoriteWidgetPresentation.championLogo: Dp get() = if (compact) 40.dp else 52.dp
internal val EventRowLogo = 24.dp
internal val EventMatchLogo = 18.dp
internal val EventBesideGap = 10.dp
internal val EventProgressWidth = 56.dp
private val EventRowInset = 8.dp

internal val EventLiveSize = 13.dp
internal val EventLiveGap = 6.dp

internal fun eventProgressBlock(live: Boolean): Dp =
  12.dp + EventProgressWidth + if (live) EventLiveSize + EventLiveGap else 0.dp

internal class EventWidgetMeasure(
  private val frame: FavoriteWidgetFrame,
  size: DpSize,
  private val typography: FavoriteWidgetTypography,
) {
  val width = size.width - 32.dp
  val inner = width - FavoriteWidgetCardPadding * 2
  val list = when (frame) {
    is FavoriteWidgetFrame.Event -> frame.large.content().list
    is FavoriteWidgetFrame.Setup -> EventWidgetList.Ghost(frame.text.ghost)
    is FavoriteWidgetFrame.Placeholder -> EventWidgetList.Ghost(frame.text.ghost)
    else -> null
  }
  private fun text(
    value: String,
    sp: Int,
    width: Dp,
    properName: Boolean = false,
    weight: FavoriteWidgetWeight = FavoriteWidgetWeight.Normal,
    maxLines: Int = 2,
  ) = typography.height(value, sp, width, maxLines = maxLines, properName = properName, weight = weight)

  fun rowHeight(rows: Int): Dp = when (val list = list) {
    is EventWidgetList.Matches -> list.rows.take(rows).maxOfOrNull { row ->
      val columns = matchColumns(typography, row, inner, 0)
      fun participant(name: String, score: String?) = maxOf(
        EventMatchLogo,
        text(name, FavoriteWidgetFont.Body, columns.name, true),
        score?.let { text(it, FavoriteWidgetFont.Body, columns.score, weight = FavoriteWidgetWeight.Medium, maxLines = 1) } ?: 0.dp,
      )
      maxOf(
        52.dp,
        EventRowInset + maxOf(
          text(row.day, FavoriteWidgetFont.Label, columns.time) +
            text(row.time, FavoriteWidgetFont.Body, columns.time, weight = FavoriteWidgetWeight.Medium, maxLines = 1),
          participant(row.leftName, row.leftScore) + 4.dp + participant(row.rightName, row.rightScore),
          text(row.round, FavoriteWidgetFont.Label, columns.round),
        ),
      )
    } ?: 52.dp

    is EventWidgetList.Group -> list.rows.take(rows).maxOfOrNull { row ->
      val columns = groupColumns(typography, row, inner, 0)
      maxOf(
        40.dp,
        EventRowInset + maxOf(
          EventRowLogo,
          text(row.team.name, FavoriteWidgetFont.Row, columns.name, true, if (row.favorite) FavoriteWidgetWeight.Medium else FavoriteWidgetWeight.Normal),
          text(row.rank, FavoriteWidgetFont.Body, columns.rank, maxLines = 1),
          text(row.record, FavoriteWidgetFont.Row, columns.record, weight = FavoriteWidgetWeight.Medium, maxLines = 1),
          text(row.maps, FavoriteWidgetFont.Body, columns.maps, maxLines = 1),
        ),
      )
    } ?: 40.dp

    is EventWidgetList.Prizes -> list.rows.take(rows).maxOfOrNull { row ->
      val columns = prizeColumns(typography, row, inner, 0)
      maxOf(
        40.dp,
        EventRowInset + maxOf(
          EventRowLogo,
          text(row.position, FavoriteWidgetFont.Body, columns.position, maxLines = 1),
          text(row.team.name, FavoriteWidgetFont.Row, columns.name, true),
          text(row.amount, FavoriteWidgetFont.Row, columns.amount, weight = FavoriteWidgetWeight.Medium, maxLines = 1),
        ),
      )
    } ?: 40.dp

    is EventWidgetList.Ghost, null -> 48.dp
  }

  fun height(fit: FavoriteWidgetPresentation): Dp {
    fun text(value: String, sp: Int, width: Dp = this.width, weight: FavoriteWidgetWeight = FavoriteWidgetWeight.Normal, properName: Boolean = false, maxLines: Int = 2) =
      typography.height(value, fit.textSize(sp), width, maxLines = maxLines, properName = properName, weight = weight)
    val logo = fit.headerLogo
    val headerText = width - logo - FavoriteWidgetHeaderGap
    val main = when (frame) {
      is FavoriteWidgetFrame.Event -> {
        val value = if (fit.large) frame.large else frame.medium
        val hero = value.content().hero
        val progress = if (FavoriteWidgetPart.Progress in fit) eventProgressBlock(value.header.live) else 0.dp
        val header = maxOf(
          logo,
          text(value.header.title, FavoriteWidgetFont.Title, headerText - progress, FavoriteWidgetWeight.Medium, properName = true) +
            if (FavoriteWidgetPart.Subtitle in fit) text(value.header.subtitle, FavoriteWidgetFont.Body, headerText - progress, maxLines = 1) else 0.dp,
        )
        header + fit.heroGap + heroHeight(hero, fit)
      }

      is FavoriteWidgetFrame.Setup -> maxOf(
        logo,
        text(frame.text.title, FavoriteWidgetFont.Title, headerText, FavoriteWidgetWeight.Medium) +
          if (FavoriteWidgetPart.Context in fit) text(frame.text.subtitle, FavoriteWidgetFont.Body, headerText) else 0.dp,
      ) +
        fit.heroGap + text(frame.text.lead, FavoriteWidgetFont.Body, weight = FavoriteWidgetWeight.Medium) +
        typography.height(frame.text.action, fit.heroSize, width, maxLines = 1) +
        if (FavoriteWidgetPart.Context in fit) text(frame.text.instruction, FavoriteWidgetFont.Body) else 0.dp

      is FavoriteWidgetFrame.Placeholder ->
        maxOf(logo, text(frame.text.title, FavoriteWidgetFont.Title, headerText, FavoriteWidgetWeight.Medium)) +
          fit.heroGap + typography.height(frame.text.value, fit.heroSize, width, maxLines = 1)

      else -> 0.dp
    }
    return fit.verticalPadding * 2 + main + if (fit.rows > 0 && list != null) {
      fit.heroGap + typography.cardChrome(list.heading, inner, fit.rows, fit.textStep) + rowHeight(fit.rows) * fit.rows
    } else {
      0.dp
    }
  }

  fun heroHeight(hero: EventWidgetHero, fit: FavoriteWidgetPresentation): Dp {
    val heroWidth = heroWidth(width, hero, fit)
    fun text(value: String, sp: Int, available: Dp = heroWidth, weight: FavoriteWidgetWeight = FavoriteWidgetWeight.Normal) =
      typography.height(value, fit.textSize(sp), available, weight = weight)
    val captions = captionWidths(typography, hero, fit, heroWidth)
    val lead = maxOf(
      typography.height(
        hero.lead,
        fit.textSize(FavoriteWidgetFont.Body),
        heroWidth - captions.total,
        properName = hero.pair != null,
        weight = FavoriteWidgetWeight.Medium,
      ),
      hero.day?.let { text(it, FavoriteWidgetFont.Body, captions.day) } ?: 0.dp,
      hero.liveLabel?.let { text(it, FavoriteWidgetFont.Body, captions.live, FavoriteWidgetWeight.Medium) } ?: 0.dp,
      hero.otherLive?.takeIf { FavoriteWidgetPart.OtherLive in fit }?.let { text(it, FavoriteWidgetFont.Body, captions.other) } ?: 0.dp,
    )
    val beside = hero.beside?.takeIf { FavoriteWidgetPart.Beside in fit }
    val valueWidth = if (beside == null) heroWidth else heroWidth * .48f
    val value = maxOf(
      typography.height(hero.value, fit.heroSize, valueWidth, maxLines = 1, properName = hero.champion != null),
      beside?.let {
        text(it.stage, FavoriteWidgetFont.Body, heroWidth - valueWidth - EventBesideGap, FavoriteWidgetWeight.Medium) +
          (it.format?.let { format -> text(format, FavoriteWidgetFont.Body, heroWidth - valueWidth - EventBesideGap) } ?: 0.dp)
      }
        ?: 0.dp,
    )
    val below = if (hero.pills.isNotEmpty() && FavoriteWidgetPart.Pills in fit) {
      typography.pillsHeight(hero.pills, heroWidth, fit.textStep)
    } else if (hero.context != null && FavoriteWidgetPart.Context in fit) {
      text(hero.context, FavoriteWidgetFont.Body, heroWidth)
    } else {
      0.dp
    }
    return maxOf(
      lead + value + below,
      when {
        hero.pair != null -> fit.pairBox.height
        hero.champion != null -> fit.championLogo
        else -> 0.dp
      },
    )
  }
}

internal fun heroWidth(width: Dp, hero: EventWidgetHero, fit: FavoriteWidgetPresentation): Dp = width - when {
  hero.pair != null -> fit.pairBox.width + 12.dp
  hero.champion != null -> fit.championLogo + 12.dp
  else -> 0.dp
}

internal data class EventCaptionWidths(val day: Dp, val live: Dp, val other: Dp) {
  val total: Dp get() = listOf(day, live, other).filter { it > 0.dp }.fold(0.dp) { sum, caption ->
    sum + caption + FavoriteWidgetCaptionGap
  }
}

internal fun captionWidths(
  typography: FavoriteWidgetTypography,
  hero: EventWidgetHero,
  fit: FavoriteWidgetPresentation,
  width: Dp,
): EventCaptionWidths = EventCaptionWidths(
  typography.column(hero.day, fit.textSize(FavoriteWidgetFont.Body), width, .45f),
  typography.column(hero.liveLabel, fit.textSize(FavoriteWidgetFont.Body), width, .3f, FavoriteWidgetWeight.Medium),
  typography.column(hero.otherLive?.takeIf { FavoriteWidgetPart.OtherLive in fit }, fit.textSize(FavoriteWidgetFont.Body), width, .15f),
)

internal data class EventMatchColumns(val time: Dp, val round: Dp, val score: Dp, val name: Dp)
internal fun matchColumns(
  typography: FavoriteWidgetTypography,
  row: EventWidgetMatch,
  width: Dp,
  step: Int,
): EventMatchColumns {
  val time = maxOf(
    typography.column(row.day, FavoriteWidgetFont.Label - step, width, .25f),
    typography.column(row.time, FavoriteWidgetFont.Body - step, width, .25f, FavoriteWidgetWeight.Medium),
  )
  val round = typography.column(row.round, FavoriteWidgetFont.Label - step, width, .24f)
  val score = maxOf(
    typography.column(row.leftScore, FavoriteWidgetFont.Body - step, width, .12f, FavoriteWidgetWeight.Medium),
    typography.column(row.rightScore, FavoriteWidgetFont.Body - step, width, .12f, FavoriteWidgetWeight.Medium),
  )
  val scoreGap = if (score > 0.dp) FavoriteWidgetCaptionGap else 0.dp
  return EventMatchColumns(time, round, score, width - time - round - score - scoreGap - EventMatchLogo - 24.dp)
}

internal data class EventGroupColumns(val rank: Dp, val record: Dp, val maps: Dp, val name: Dp)
internal fun groupColumns(
  typography: FavoriteWidgetTypography,
  row: dev.staticvar.vlr.shared.widget.favorite.GroupRowText,
  width: Dp,
  step: Int,
): EventGroupColumns {
  val rank = typography.column(row.rank, FavoriteWidgetFont.Body - step, width, .1f)
  val record = typography.column(row.record, FavoriteWidgetFont.Row - step, width, .22f, FavoriteWidgetWeight.Medium)
  val maps = typography.column(row.maps, FavoriteWidgetFont.Body - step, width, .18f)
  return EventGroupColumns(rank, record, maps, width - rank - record - maps - EventRowLogo - 38.dp)
}

internal data class EventPrizeColumns(val position: Dp, val amount: Dp, val name: Dp)
internal fun prizeColumns(
  typography: FavoriteWidgetTypography,
  row: dev.staticvar.vlr.shared.widget.favorite.PrizeRowText,
  width: Dp,
  step: Int,
): EventPrizeColumns {
  val position = typography.column(row.position, FavoriteWidgetFont.Body - step, width, .15f)
  val amount = typography.column(row.amount, FavoriteWidgetFont.Row - step, width, .35f, FavoriteWidgetWeight.Medium)
  return EventPrizeColumns(position, amount, width - position - amount - EventRowLogo - 28.dp)
}
