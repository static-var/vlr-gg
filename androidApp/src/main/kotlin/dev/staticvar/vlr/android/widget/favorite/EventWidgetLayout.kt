/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.TextAlign
import dev.staticvar.vlr.shared.widget.favorite.EventHeaderText
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetFrame
import dev.staticvar.vlr.shared.widget.favorite.LogoMark
import dev.staticvar.vlr.shared.widget.favorite.LoserSide

@Composable
internal fun EventWidgetLayout(frame: FavoriteWidgetFrame, logos: WidgetBitmaps) {
  val size = LocalSize.current
  val typography = FavoriteWidgetTypography(LocalContext.current)
  val fit = eventPresentationFor(frame, size, typography)
  val measure = EventWidgetMeasure(frame, size, typography)
  CompositionLocalProvider(LocalFavoriteWidgetTextStep provides fit.textStep) {
    Column(GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = fit.verticalPadding)) {
      when (frame) {
        is FavoriteWidgetFrame.Setup -> {
          FavoriteWidgetEmptyHeader(
            frame.text.title,
            frame.text.subtitle.takeIf { FavoriteWidgetPart.Context in fit },
            fit.headerLogo,
          )
          FavoriteWidgetHeroSpacer(fit)
          FavoriteWidgetText(frame.text.lead, 14, FavoriteWidgetSecondary, FavoriteWidgetWeight.Medium)
          FavoriteWidgetText(frame.text.action, fit.heroSize, textStep = 0, maxLines = 1)
          if (FavoriteWidgetPart.Context in fit) {
            FavoriteWidgetText(frame.text.instruction, 13, FavoriteWidgetSecondary)
          }
        }

        is FavoriteWidgetFrame.Placeholder -> {
          FavoriteWidgetEmptyHeader(frame.text.title, null, fit.headerLogo)
          FavoriteWidgetHeroSpacer(fit)
          FavoriteWidgetText(frame.text.value, fit.heroSize, textStep = 0, maxLines = 1)
        }

        is FavoriteWidgetFrame.Event -> {
          val value = if (fit.large) frame.large else frame.medium
          val hero = value.content().hero
          EventHeader(value.header, hero.liveLabel, logos, measure.width, fit)
          FavoriteWidgetHeroSpacer(fit)
          EventHero(hero, logos, measure.width, fit, measure)
        }

        is FavoriteWidgetFrame.Team -> Unit
      }
      val list = measure.list
      if (fit.rows > 0 && list != null) {
        Spacer(GlanceModifier.height(fit.heroGap))
        EventList(list, logos, measure.inner, fit)
      }
    }
  }
}

@Composable
private fun EventHeader(
  header: EventHeaderText,
  liveLabel: String?,
  logos: WidgetBitmaps,
  width: Dp,
  fit: FavoriteWidgetPresentation,
) {
  val logo = fit.headerLogo
  val progress = if (FavoriteWidgetPart.Progress in fit) eventProgressBlock(header.live) else 0.dp
  Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
    FavoriteWidgetLogo(header.event, logos, logo)
    Spacer(GlanceModifier.width(FavoriteWidgetHeaderGap))
    Column(GlanceModifier.defaultWeight()) {
      FavoriteWidgetText(
        header.title,
        16,
        weight = FavoriteWidgetWeight.Medium,
        properName = true,
        width = width - logo - FavoriteWidgetHeaderGap - progress,
      )
      if (FavoriteWidgetPart.Subtitle in fit) {
        FavoriteWidgetText(header.subtitle, 13, FavoriteWidgetSecondary, maxLines = 1)
      }
    }
    if (FavoriteWidgetPart.Progress in fit) {
      Spacer(GlanceModifier.width(12.dp))
      if (header.live && liveLabel != null) FavoriteWidgetDot(liveLabel, FavoriteWidgetDotStyle.Live)
      Box(
        GlanceModifier.width(EventProgressWidth).height(6.dp).cornerRadius(3.dp)
          .background(GlanceTheme.colors.surfaceVariant),
      ) {
        header.progress?.takeIf { it > 0f }?.let {
          Box(
            GlanceModifier.width(maxOf(6.dp, EventProgressWidth * it)).height(6.dp).cornerRadius(3.dp)
              .background(GlanceTheme.colors.primary),
          ) {}
        }
      }
    }
  }
}

@Composable
private fun EventHero(
  hero: EventWidgetHero,
  logos: WidgetBitmaps,
  width: Dp,
  fit: FavoriteWidgetPresentation,
  measure: EventWidgetMeasure,
) {
  val textWidth = heroWidth(width, hero, fit)
  val captions = captionWidths(FavoriteWidgetTypography(LocalContext.current), hero, fit, textWidth)
  val beside = hero.beside?.takeIf { FavoriteWidgetPart.Beside in fit }
  val valueWidth = if (beside == null) textWidth else textWidth * .48f
  val below = hero.pills.isNotEmpty() && FavoriteWidgetPart.Pills in fit
  val context = hero.context?.takeIf { FavoriteWidgetPart.Context in fit }
  Row(
    GlanceModifier.fillMaxWidth().height(measure.heroHeight(hero, fit)),
    verticalAlignment = Alignment.Vertical.CenterVertically,
  ) {
    Column(GlanceModifier.defaultWeight()) {
      Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        FavoriteWidgetText(
          hero.lead,
          14,
          weight = FavoriteWidgetWeight.Medium,
          properName = hero.pair != null,
          modifier = GlanceModifier.defaultWeight(),
          width = textWidth - captions.total,
        )
        hero.day?.let {
          Spacer(GlanceModifier.width(FavoriteWidgetCaptionGap))
          FavoriteWidgetText(
            it,
            13,
            FavoriteWidgetSecondary,
            modifier = GlanceModifier.width(captions.day),
            align = TextAlign.End,
          )
        }
        hero.liveLabel?.let {
          Spacer(GlanceModifier.width(FavoriteWidgetCaptionGap))
          FavoriteWidgetText(
            it,
            13,
            GlanceTheme.colors.error,
            FavoriteWidgetWeight.Medium,
            modifier = GlanceModifier.width(captions.live),
            description = it,
          )
        }
        if (FavoriteWidgetPart.OtherLive in fit) {
          hero.otherLive?.let {
            Spacer(GlanceModifier.width(FavoriteWidgetCaptionGap))
            FavoriteWidgetText(
              it,
              13,
              FavoriteWidgetSecondary,
              modifier = GlanceModifier.width(captions.other),
              align = TextAlign.End,
            )
          }
        }
      }
      Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        FavoriteWidgetText(
          hero.value,
          fit.heroSize,
          modifier = GlanceModifier.defaultWeight(),
          textStep = 0,
          maxLines = 1,
          properName = hero.champion != null,
        )
        beside?.let {
          Column(GlanceModifier.width(textWidth - valueWidth).padding(start = EventBesideGap)) {
            FavoriteWidgetText(it.stage, 13, weight = FavoriteWidgetWeight.Medium)
            it.format?.let { format -> FavoriteWidgetText(format, 13, FavoriteWidgetSecondary) }
          }
        }
      }
      if (below) {
        FavoriteWidgetMapPills(hero.pills)
      } else {
        context?.let { FavoriteWidgetText(it, 13, FavoriteWidgetSecondary) }
      }
    }
    if (hero.pair != null) {
      Spacer(GlanceModifier.width(12.dp))
      Box(GlanceModifier.width(fit.pairBox.width).height(fit.pairBox.height)) {
        Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
          FavoriteWidgetLogo(hero.pair.left, logos, fit.pairLogo)
        }
        Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
          FavoriteWidgetLogo(hero.pair.right, logos, fit.pairLogo)
        }
      }
    }
    hero.champion?.let {
      Spacer(GlanceModifier.width(12.dp))
      FavoriteWidgetLogo(it, logos, fit.championLogo)
    }
  }
}

@Composable
private fun ColumnScope.EventList(list: EventWidgetList, logos: WidgetBitmaps, width: Dp, fit: FavoriteWidgetPresentation) {
  val typography = FavoriteWidgetTypography(LocalContext.current)
  val rows: List<@Composable () -> Unit> = when (list) {
    is EventWidgetList.Matches -> list.rows.take(fit.rows).map { row -> @Composable { EventMatchRow(row, logos, width) } }

    is EventWidgetList.Group -> list.rows.take(fit.rows).map { row ->
      @Composable {
        val columns = groupColumns(typography, row, width, fit.textStep)
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
          FavoriteWidgetText(
            row.rank,
            13,
            FavoriteWidgetSecondary,
            maxLines = 1,
            modifier = GlanceModifier.width(columns.rank),
          )
          Spacer(GlanceModifier.width(10.dp))
          FavoriteWidgetLogo(row.team, logos, EventRowLogo)
          Spacer(GlanceModifier.width(10.dp))
          FavoriteWidgetText(
            row.team.name,
            15,
            if (row.favorite) GlanceTheme.colors.primary else GlanceTheme.colors.onSurface,
            if (row.favorite) FavoriteWidgetWeight.Medium else FavoriteWidgetWeight.Normal,
            modifier = GlanceModifier.defaultWeight(),
            properName = true,
            width = columns.name,
          )
          Spacer(GlanceModifier.width(8.dp))
          FavoriteWidgetText(
            row.record,
            15,
            weight = FavoriteWidgetWeight.Medium,
            maxLines = 1,
            modifier = GlanceModifier.width(columns.record),
            align = TextAlign.End,
          )
          Spacer(GlanceModifier.width(10.dp))
          FavoriteWidgetText(
            row.maps,
            13,
            FavoriteWidgetSecondary,
            maxLines = 1,
            modifier = GlanceModifier.width(columns.maps),
            align = TextAlign.End,
          )
        }
      }
    }

    is EventWidgetList.Prizes -> list.rows.take(fit.rows).map { row ->
      @Composable {
        val columns = prizeColumns(typography, row, width, fit.textStep)
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
          FavoriteWidgetText(
            row.position,
            13,
            FavoriteWidgetSecondary,
            maxLines = 1,
            modifier = GlanceModifier.width(columns.position),
          )
          Spacer(GlanceModifier.width(10.dp))
          FavoriteWidgetLogo(row.team, logos, EventRowLogo)
          Spacer(GlanceModifier.width(10.dp))
          FavoriteWidgetText(row.team.name, 15, modifier = GlanceModifier.defaultWeight(), properName = true)
          Spacer(GlanceModifier.width(8.dp))
          FavoriteWidgetText(
            row.amount,
            15,
            weight = FavoriteWidgetWeight.Medium,
            maxLines = 1,
            modifier = GlanceModifier.width(columns.amount),
            align = TextAlign.End,
          )
        }
      }
    }

    is EventWidgetList.Ghost -> List(fit.rows) {
      @Composable {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
          repeat(2) {
            Box(GlanceModifier.size(EventMatchLogo).cornerRadius(5.dp).background(FavoriteWidgetCardOutline)) {}
            Spacer(GlanceModifier.width(8.dp))
            Box(
              GlanceModifier.width(width / 4).height(10.dp).cornerRadius(5.dp)
                .background(FavoriteWidgetCardOutline),
            ) {}
            Spacer(GlanceModifier.width(12.dp))
          }
        }
      }
    }
  }
  FavoriteWidgetCard(list.heading, GlanceModifier.defaultWeight(), rows)
}

@Composable
private fun EventMatchRow(row: EventWidgetMatch, logos: WidgetBitmaps, width: Dp) {
  val columns =
    matchColumns(FavoriteWidgetTypography(LocalContext.current), row, width, LocalFavoriteWidgetTextStep.current)
  Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
    Column(GlanceModifier.width(columns.time)) {
      FavoriteWidgetText(
        row.day,
        12,
        if (row.live) GlanceTheme.colors.error else FavoriteWidgetSecondary,
        if (row.live) FavoriteWidgetWeight.Medium else FavoriteWidgetWeight.Normal,
      )
      FavoriteWidgetText(row.time, 14, weight = FavoriteWidgetWeight.Medium, maxLines = 1)
    }
    Spacer(GlanceModifier.width(10.dp))
    Column(GlanceModifier.defaultWeight()) {
      EventParticipant(row.left, row.leftName, row.leftScore, row.loser == LoserSide.Left, logos, columns.score)
      Spacer(GlanceModifier.height(4.dp))
      EventParticipant(row.right, row.rightName, row.rightScore, row.loser == LoserSide.Right, logos, columns.score)
    }
    Spacer(GlanceModifier.width(8.dp))
    FavoriteWidgetText(
      row.round,
      12,
      FavoriteWidgetSecondary,
      modifier = GlanceModifier.width(columns.round),
      align = TextAlign.End,
    )
  }
}

@Composable
private fun EventParticipant(
  mark: LogoMark,
  name: String,
  score: String?,
  dimmed: Boolean,
  logos: WidgetBitmaps,
  scoreWidth: Dp,
) {
  val color = if (dimmed) FavoriteWidgetSecondary else GlanceTheme.colors.onSurface
  Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
    FavoriteWidgetLogo(mark, logos, EventMatchLogo)
    Spacer(GlanceModifier.width(6.dp))
    FavoriteWidgetText(name, 14, color, modifier = GlanceModifier.defaultWeight(), properName = true)
    score?.let {
      Spacer(GlanceModifier.width(FavoriteWidgetCaptionGap))
      FavoriteWidgetText(
        it,
        14,
        color,
        FavoriteWidgetWeight.Medium,
        maxLines = 1,
        modifier = GlanceModifier.width(scoreWidth),
        align = TextAlign.End,
      )
    }
  }
}
