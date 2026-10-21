/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import dev.staticvar.vlr.android.widget.PrismTextTone
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetFrame
import dev.staticvar.vlr.shared.widget.favorite.FormMark
import dev.staticvar.vlr.shared.widget.favorite.LogoMark
import dev.staticvar.vlr.shared.widget.favorite.NeutralFormMark
import dev.staticvar.vlr.shared.widget.favorite.NeutralTeamHeroText
import dev.staticvar.vlr.shared.widget.favorite.NeutralTeamHistoryText
import dev.staticvar.vlr.shared.widget.favorite.PillText
import dev.staticvar.vlr.shared.widget.favorite.PillTone
import dev.staticvar.vlr.shared.widget.favorite.ResultTone
import dev.staticvar.vlr.shared.widget.favorite.TeamDetail
import dev.staticvar.vlr.shared.widget.favorite.TeamDisplayBody
import dev.staticvar.vlr.shared.widget.favorite.TeamHeroText
import dev.staticvar.vlr.shared.widget.favorite.TeamLiveBelow
import dev.staticvar.vlr.shared.widget.favorite.TeamPresentation

internal fun teamPresentationFor(
  frame: FavoriteWidgetFrame,
  size: DpSize,
  typography: FavoriteWidgetTypography,
): FavoriteWidgetPresentation {
  val measure = TeamWidgetMeasure(frame, size, typography)
  return presentationFor(size, measure.rowCount, measure::rowHeight, measure::height)
}

internal class TeamWidgetMeasure(
  private val frame: FavoriteWidgetFrame,
  size: DpSize,
  private val typography: FavoriteWidgetTypography,
) {
  private val width = size.width - 32.dp
  private val body = (frame as? FavoriteWidgetFrame.Team)?.large?.body
  private val detail = (body as? TeamDisplayBody.Visible)?.detail
  private val history = (body as? TeamDisplayBody.Hidden)?.history
  val rowCount = when (detail) {
    is TeamDetail.Maps -> detail.rows.size
    is TeamDetail.History -> detail.rows.size
    null -> history?.rows?.size ?: if (frame is FavoriteWidgetFrame.Team) 0 else 4
  }
  private fun text(text: String, sp: Int, available: Dp = width, properName: Boolean = false) =
    typography.height(text, sp, available, properName = properName)
  fun rowHeight(rows: Int): Dp = when (detail) {
    is TeamDetail.Maps -> detail.rows.take(rows).maxOfOrNull { row ->
      val score = typography.column(row.score, 17, width, .35f)
      maxOf(
        if (detail.compact) 30.dp else 48.dp,
        text(row.name, 15, width - score, properName = true) +
          (row.subtitle?.let { text(it, 12, width - score) } ?: 0.dp),
        typography.height(row.score, 17, score, maxLines = 1),
      )
    } ?: 0.dp

    is TeamDetail.History -> detail.rows.take(rows).maxOfOrNull { row ->
      val columns = teamHistoryColumns(typography, row.mark, row.date, row.score, width, 0)
      maxOf(
        34.dp,
        text(row.name, 15, columns.name, properName = true),
        typography.height(row.mark, 12, columns.mark, maxLines = 1),
        typography.height(row.score, 17, columns.score, maxLines = 1),
        text(row.date, 12, columns.date),
      )
    } ?: 0.dp

    null -> history?.rows?.take(rows)?.maxOfOrNull { row ->
      val date = typography.column(row.date, 12, width, .3f)
      maxOf(34.dp, text(row.name, 15, width - date - 38.dp, properName = true), text(row.date, 12, date))
    } ?: 34.dp
  }
  fun height(fit: FavoriteWidgetPresentation): Dp {
    fun text(text: String, sp: Int, available: Dp = width, properName: Boolean = false) =
      typography.height(text, fit.textSize(sp), available, properName = properName)
    val logoSize = if (fit.large) 40.dp else 28.dp
    val main = when (frame) {
      is FavoriteWidgetFrame.Setup -> {
        val subtitle = frame.text.subtitle.takeIf { FavoriteWidgetPart.Context in fit }
        val headerWidth = width - logoSize - 8.dp
        val header = maxOf(
          logoSize,
          text(frame.text.title, 15, headerWidth) +
            (subtitle?.let { text(it, 12, headerWidth) } ?: 0.dp),
        )
        header + 4.dp + text(frame.text.lead, 13) +
          typography.height(frame.text.action, fit.heroSize, width, maxLines = 1) +
          if (FavoriteWidgetPart.Context in fit) text(frame.text.instruction, 12) else 0.dp
      }

      is FavoriteWidgetFrame.Placeholder -> maxOf(logoSize, text(frame.text.title, 15, width - logoSize - 8.dp)) +
        4.dp + typography.height(frame.text.value, fit.heroSize, width, maxLines = 1)

      is FavoriteWidgetFrame.Team -> {
        val content = if (fit.large) frame.large else frame.medium
        val formCount = if (FavoriteWidgetPart.Form !in fit) {
          0
        } else {
          when (val b = content.body) {
            is TeamDisplayBody.Visible -> b.form.size
            is TeamDisplayBody.Hidden -> b.form.size
          }
        }
        val headerWidth = width - logoSize - 8.dp - typography.roundedToPixel(12.dp) * formCount
        val rank = if (FavoriteWidgetPart.RegionalRank in fit) content.header.ranking else frame.medium.header.ranking
        val rankHeight =
          if (rank.isEmpty()) 0.dp else typography.height(rank, fit.textSize(12), headerWidth, maxLines = 1)
        val header = maxOf(logoSize, text(content.header.title, 15, headerWidth, properName = true) + rankHeight)
        val visible = (content.body as? TeamDisplayBody.Visible)?.hero
        val hidden = (content.body as? TeamDisplayBody.Hidden)?.hero
        val lead = visible?.lead ?: requireNotNull(hidden).lead
        val heroValue = visible?.value ?: requireNotNull(hidden).value
        val opponent = visible?.opponent ?: hidden?.opponent
        val heroWidth = width - if (opponent == null) 0.dp else 52.dp
        val day = (visible as? TeamHeroText.Upcoming)?.day ?: (hidden as? NeutralTeamHeroText.Upcoming)?.day
        val live = (visible as? TeamHeroText.Live)?.liveLabel ?: (hidden as? NeutralTeamHeroText.Live)?.liveLabel
        val labelWidth = typography.column(day ?: live, fit.textSize(12), heroWidth, .45f)
        val hero = typography.heroHeight(
          FavoriteWidgetHeroMeasure(
            lead,
            heroValue,
            day ?: live,
            heroWidth,
            labelWidth,
            fit.heroSize,
            if (opponent == null) 0.dp else 44.dp,
            opponent != null,
          ),
          fit.textStep,
        )
        val context = when (visible) {
          is TeamHeroText.Upcoming -> visible.context
          is TeamHeroText.Idle -> visible.context
          is TeamHeroText.Live -> (visible.below as? TeamLiveBelow.Context)?.text
          null -> hidden?.context
        }
        val pills = ((visible as? TeamHeroText.Live)?.below as? TeamLiveBelow.Pills)?.items
        val below = if (pills != null && FavoriteWidgetPart.Pills in fit) {
          8.dp +
            (
              pills.maxOfOrNull {
                typography.height(
                  it.text,
                  fit.textSize(11),
                  (width - (pills.size * 10).dp) / pills.size,
                  maxLines = 1,
                )
              }
                ?: 0.dp
              )
        } else if (context != null && FavoriteWidgetPart.Context in fit) {
          text(context, 12)
        } else {
          0.dp
        }
        header + 4.dp + hero + below
      }

      is FavoriteWidgetFrame.Event -> 0.dp
    }
    val heading = when (detail) {
      is TeamDetail.Maps -> detail.heading

      is TeamDetail.History -> detail.heading

      null -> history?.heading ?: when (frame) {
        is FavoriteWidgetFrame.Setup -> frame.text.ghost
        is FavoriteWidgetFrame.Placeholder -> frame.text.ghost
        else -> ""
      }
    }
    val listHeight = if (fit.rows > 0) {
      6.dp + text(heading, 12) + rowHeight(fit.rows) * fit.rows
    } else {
      0.dp
    }
    return fit.verticalPadding * 2 + main + listHeight
  }
}

@Composable
internal fun TeamWidgetLayout(frame: FavoriteWidgetFrame, logos: WidgetBitmaps) {
  val typography = FavoriteWidgetTypography(LocalContext.current)
  val presentation = teamPresentationFor(frame, LocalSize.current, typography)
  val large = presentation.large
  val width = LocalSize.current.width - 32.dp
  CompositionLocalProvider(LocalFavoriteWidgetTextStep provides presentation.textStep) {
    Column(GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = presentation.verticalPadding)) {
      when (frame) {
        is FavoriteWidgetFrame.Setup -> {
          FavoriteWidgetEmptyHeader(
            frame.text.title,
            frame.text.subtitle.takeIf {
              FavoriteWidgetPart.Context in presentation
            },
            large,
          )
          Spacer(GlanceModifier.height(4.dp))
          FavoriteWidgetText(frame.text.lead, 13, tone = PrismTextTone.Secondary)
          FavoriteWidgetText(
            frame.text.action,
            presentation.heroSize,
            textStep = 0,
            maxLines = 1,
          )
          if (FavoriteWidgetPart.Context in presentation) {
            FavoriteWidgetText(frame.text.instruction, 12, tone = PrismTextTone.Secondary)
          }
          if (large) {
            Spacer(GlanceModifier.defaultWeight())
            TeamGhostHistory(frame.text.ghost, width, presentation.rows)
          }
        }

        is FavoriteWidgetFrame.Placeholder -> {
          FavoriteWidgetEmptyHeader(frame.text.title, null, large)
          Spacer(GlanceModifier.height(4.dp))
          FavoriteWidgetText(frame.text.value, presentation.heroSize, textStep = 0, maxLines = 1)
          if (large) {
            Spacer(GlanceModifier.defaultWeight())
            TeamGhostHistory(frame.text.ghost, width, presentation.rows)
          }
        }

        is FavoriteWidgetFrame.Team -> TeamPresentationLayout(
          (if (large) frame.large else frame.medium).let { value ->
            if (FavoriteWidgetPart.RegionalRank in presentation) {
              value
            } else {
              value.copy(header = value.header.copy(ranking = frame.medium.header.ranking))
            }
          },
          logos,
          width,
          presentation,
        )

        is FavoriteWidgetFrame.Event -> Unit
      }
    }
  }
}

@Composable
private fun TeamGhostHistory(heading: String, width: Dp, rowCount: Int) {
  val rowHeight = 34.dp
  if (rowCount == 0) return
  Column {
    FavoriteWidgetText(heading, 12, PrismTextTone.Secondary)
    for (barWidth in listOf(118.dp, 84.dp, 140.dp, 96.dp).take(rowCount)) {
      Row(GlanceModifier.fillMaxWidth().height(rowHeight), verticalAlignment = Alignment.Vertical.CenterVertically) {
        Box(GlanceModifier.size(22.dp).cornerRadius(6.dp).background(GlanceTheme.colors.secondaryContainer)) {}
        Spacer(GlanceModifier.width(10.dp))
        Box(
          GlanceModifier.width(barWidth).height(10.dp).cornerRadius(5.dp)
            .background(GlanceTheme.colors.secondaryContainer),
        ) {}
      }
    }
  }
}

@Composable
private fun TeamPresentationLayout(
  value: TeamPresentation,
  logos: WidgetBitmaps,
  width: Dp,
  presentation: FavoriteWidgetPresentation,
) {
  val large = presentation.large
  val logoSize = if (large) 40.dp else 28.dp
  Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
    FavoriteWidgetLogo(value.header.team, logos, logoSize)
    Spacer(GlanceModifier.width(8.dp))
    Column(GlanceModifier.defaultWeight()) {
      FavoriteWidgetText(value.header.title, 15, properName = true)
      if (value.header.ranking.isNotEmpty()) {
        FavoriteWidgetText(value.header.ranking, 12, PrismTextTone.Secondary, maxLines = 1)
      }
    }
    if (FavoriteWidgetPart.Form in presentation) {
      Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        when (val body = value.body) {
          is TeamDisplayBody.Visible -> body.form.forEach {
            FavoriteWidgetDot(
              it.label,
              it.mark != FormMark.Loss,
              it.mark == FormMark.Live,
              empty =
              it.mark == FormMark.Empty,
            )
          }

          is TeamDisplayBody.Hidden -> body.form.forEach {
            FavoriteWidgetDot(it.label, true, it.mark == NeutralFormMark.Live)
          }
        }
      }
    }
  }
  Spacer(GlanceModifier.height(4.dp))
  when (val body = value.body) {
    is TeamDisplayBody.Visible -> {
      TeamVisibleHero(body.hero, logos, width, presentation)
      body.detail?.let { detail ->
        val rowCount = presentation.rows
        if (rowCount > 0) {
          Spacer(GlanceModifier.height(6.dp))
          TeamDetailLayout(detail, logos, width, rowCount, presentation.rowHeight)
        }
      }
    }

    is TeamDisplayBody.Hidden -> {
      TeamHiddenHero(body.hero, logos, width, presentation)
      body.history?.let {
        val rowCount = presentation.rows
        if (rowCount > 0) {
          Spacer(GlanceModifier.height(6.dp))
          TeamNeutralHistory(it, logos, width, rowCount, presentation.rowHeight)
        }
      }
    }
  }
}

@Composable
private fun TeamVisibleHero(
  hero: TeamHeroText,
  logos: WidgetBitmaps,
  width: Dp,
  presentation: FavoriteWidgetPresentation,
) {
  when (hero) {
    is TeamHeroText.Upcoming -> {
      TeamHero(hero.lead, hero.value, hero.opponent, logos, width, presentation.heroSize, day = hero.day)
      if (FavoriteWidgetPart.Context in
        presentation
      ) {
        FavoriteWidgetText(hero.context, 12, PrismTextTone.Secondary)
      }
    }

    is TeamHeroText.Live -> {
      TeamHero(hero.lead, hero.value, hero.opponent, logos, width, presentation.heroSize, liveLabel = hero.liveLabel)
      when (val below = hero.below) {
        is TeamLiveBelow.Pills -> if (FavoriteWidgetPart.Pills in
          presentation
        ) {
          FavoriteWidgetMapPills(below.items)
        }

        is TeamLiveBelow.Context ->
          if (FavoriteWidgetPart.Context in
            presentation
          ) {
            FavoriteWidgetText(below.text, 12, PrismTextTone.Secondary)
          }
      }
    }

    is TeamHeroText.Idle -> {
      TeamHero(hero.lead, hero.value, hero.opponent, logos, width, presentation.heroSize)
      if (FavoriteWidgetPart.Context in
        presentation
      ) {
        FavoriteWidgetText(hero.context, 12, PrismTextTone.Secondary)
      }
    }
  }
}

@Composable
private fun TeamHiddenHero(
  hero: NeutralTeamHeroText,
  logos: WidgetBitmaps,
  width: Dp,
  presentation: FavoriteWidgetPresentation,
) {
  when (hero) {
    is NeutralTeamHeroText.Upcoming -> TeamHero(
      hero.lead,
      hero.value,
      hero.opponent,
      logos,
      width,
      presentation.heroSize,
      day = hero.day,
    )

    is NeutralTeamHeroText.Live -> TeamHero(
      hero.lead,
      hero.value,
      hero.opponent,
      logos,
      width,
      presentation.heroSize,
      liveLabel = hero.liveLabel,
    )

    is NeutralTeamHeroText.Idle -> TeamHero(hero.lead, hero.value, hero.opponent, logos, width, presentation.heroSize)
  }
  if (FavoriteWidgetPart.Context in presentation) FavoriteWidgetText(hero.context, 12, PrismTextTone.Secondary)
}

@Composable
private fun TeamHero(
  lead: String,
  value: String,
  opponent: LogoMark?,
  logos: WidgetBitmaps,
  width: Dp,
  heroSize: Int,
  day: String? = null,
  liveLabel: String? = null,
) {
  val textWidth = width - if (opponent == null) 0.dp else 52.dp
  val typography = FavoriteWidgetTypography(LocalContext.current)
  val labelWidth = typography.column(day ?: liveLabel, 12 - LocalFavoriteWidgetTextStep.current, textWidth, .45f)
  val rowHeight = typography.heroHeight(
    FavoriteWidgetHeroMeasure(
      lead,
      value,
      day ?: liveLabel,
      textWidth,
      labelWidth,
      heroSize,
      if (opponent == null) 0.dp else 44.dp,
      opponent != null,
    ),
    LocalFavoriteWidgetTextStep.current,
  )
  Row(GlanceModifier.fillMaxWidth().height(rowHeight), verticalAlignment = Alignment.Vertical.CenterVertically) {
    Column(GlanceModifier.defaultWeight()) {
      Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        FavoriteWidgetText(
          lead,
          13,
          properName = opponent != null,
          modifier = GlanceModifier.defaultWeight(),
        )
        day?.let {
          Spacer(GlanceModifier.width(FavoriteWidgetCaptionGap))
          FavoriteWidgetText(
            it,
            12,
            PrismTextTone.Secondary,
            modifier = GlanceModifier.width(labelWidth),
            align = androidx.glance.text.TextAlign.End,
          )
        }
        liveLabel?.let {
          Spacer(GlanceModifier.width(FavoriteWidgetCaptionGap))
          FavoriteWidgetText(
            it,
            12,
            PrismTextTone.Accent,
            description = it,
            modifier = GlanceModifier.width(labelWidth),
          )
        }
      }
      FavoriteWidgetText(value, heroSize, textStep = 0)
    }
    opponent?.let {
      Spacer(GlanceModifier.width(8.dp))
      FavoriteWidgetLogo(it, logos, 44.dp)
    }
  }
}

private data class TeamHistoryColumns(val mark: Dp, val date: Dp, val score: Dp, val name: Dp)

private fun teamHistoryColumns(
  typography: FavoriteWidgetTypography,
  mark: String,
  date: String,
  score: String,
  width: Dp,
  textStep: Int,
): TeamHistoryColumns {
  val markWidth = typography.column(mark, 12 - textStep, width, .1f)
  val dateWidth = typography.column(date, 12 - textStep, width, .25f)
  val scoreWidth = typography.column(score, 17 - textStep, width, .25f)
  return TeamHistoryColumns(markWidth, dateWidth, scoreWidth, width - markWidth - dateWidth - scoreWidth - 36.dp)
}

@Composable
private fun TeamDetailLayout(detail: TeamDetail, logos: WidgetBitmaps, width: Dp, rowCount: Int, rowHeight: Dp) {
  val typography = FavoriteWidgetTypography(LocalContext.current)
  val step = LocalFavoriteWidgetTextStep.current
  Column {
    when (detail) {
      is TeamDetail.Maps -> {
        FavoriteWidgetText(detail.heading, 12, PrismTextTone.Secondary)
        detail.rows.take(rowCount).forEach { map ->
          val score = typography.column(map.score, 17 - step, width, .35f)
          Row(
            GlanceModifier.fillMaxWidth().height(rowHeight),
            verticalAlignment = Alignment.Vertical.CenterVertically,
          ) {
            Column(GlanceModifier.defaultWeight()) {
              FavoriteWidgetText(map.name, 15, properName = true)
              map.subtitle?.let { FavoriteWidgetText(it, 12, PrismTextTone.Secondary) }
            }
            FavoriteWidgetText(map.score, 17, map.tone.textTone(), maxLines = 1, modifier = GlanceModifier.width(score))
          }
        }
      }

      is TeamDetail.History -> {
        FavoriteWidgetText(detail.heading, 12, PrismTextTone.Secondary)
        detail.rows.take(rowCount).forEach { row ->
          val columns = teamHistoryColumns(typography, row.mark, row.date, row.score, width, step)
          Row(
            GlanceModifier.fillMaxWidth().height(rowHeight),
            verticalAlignment = Alignment.Vertical.CenterVertically,
          ) {
            FavoriteWidgetText(
              row.mark,
              12,
              if (row.tone == ResultTone.Win) PrismTextTone.Content else PrismTextTone.Secondary,
              maxLines = 1,
              modifier = GlanceModifier.width(columns.mark),
            )
            FavoriteWidgetLogo(row.opponent, logos, 22.dp)
            Spacer(GlanceModifier.width(6.dp))
            FavoriteWidgetText(row.name, 15, modifier = GlanceModifier.defaultWeight(), properName = true)
            Spacer(GlanceModifier.width(4.dp))
            FavoriteWidgetText(row.date, 12, PrismTextTone.Secondary, modifier = GlanceModifier.width(columns.date))
            Spacer(GlanceModifier.width(4.dp))
            FavoriteWidgetText(
              row.score,
              17,
              if (row.tone == ResultTone.Win) PrismTextTone.Content else PrismTextTone.Secondary,
              maxLines = 1,
              modifier = GlanceModifier.width(columns.score),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TeamNeutralHistory(
  history: NeutralTeamHistoryText,
  logos: WidgetBitmaps,
  width: Dp,
  rowCount: Int,
  rowHeight: Dp,
) {
  val typography = FavoriteWidgetTypography(LocalContext.current)
  Column {
    FavoriteWidgetText(history.heading, 12, PrismTextTone.Secondary)
    history.rows.take(rowCount).forEach { row ->
      val date = typography.column(row.date, 12 - LocalFavoriteWidgetTextStep.current, width, .3f)
      Row(GlanceModifier.fillMaxWidth().height(rowHeight), verticalAlignment = Alignment.Vertical.CenterVertically) {
        FavoriteWidgetLogo(row.opponent, logos, 22.dp)
        Spacer(GlanceModifier.width(8.dp))
        FavoriteWidgetText(row.name, 15, modifier = GlanceModifier.defaultWeight(), properName = true)
        Spacer(GlanceModifier.width(8.dp))
        FavoriteWidgetText(row.date, 12, PrismTextTone.Secondary, modifier = GlanceModifier.width(date))
      }
    }
  }
}
