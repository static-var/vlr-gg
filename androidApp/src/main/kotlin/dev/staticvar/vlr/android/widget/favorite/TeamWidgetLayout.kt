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
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetFrame
import dev.staticvar.vlr.shared.widget.favorite.FormMark
import dev.staticvar.vlr.shared.widget.favorite.LogoMark
import dev.staticvar.vlr.shared.widget.favorite.NeutralFormMark
import dev.staticvar.vlr.shared.widget.favorite.NeutralTeamHeroText
import dev.staticvar.vlr.shared.widget.favorite.NeutralTeamHistoryText
import dev.staticvar.vlr.shared.widget.favorite.PillText
import dev.staticvar.vlr.shared.widget.favorite.ResultTone
import dev.staticvar.vlr.shared.widget.favorite.TeamDetail
import dev.staticvar.vlr.shared.widget.favorite.TeamDisplayBody
import dev.staticvar.vlr.shared.widget.favorite.TeamHeroText
import dev.staticvar.vlr.shared.widget.favorite.TeamLiveBelow
import dev.staticvar.vlr.shared.widget.favorite.TeamPresentation
import dev.staticvar.vlr.shared.widget.favorite.TeamRowText
import dev.staticvar.vlr.shared.widget.favorite.WidgetTap

internal fun teamPresentationFor(
  frame: FavoriteWidgetFrame,
  size: DpSize,
  typography: FavoriteWidgetTypography,
): FavoriteWidgetPresentation {
  val measure = TeamWidgetMeasure(frame, size, typography)
  return presentationFor(size, measure.rowCount, measure::rowHeight, measure::height)
}

private val FavoriteWidgetPresentation.opponentLogo: Dp get() = if (compact) 40.dp else 52.dp
private val TeamRowLogo = 24.dp
private val TeamRowGap = 10.dp
private val TeamRowInset = 8.dp

internal class TeamWidgetMeasure(
  private val frame: FavoriteWidgetFrame,
  size: DpSize,
  private val typography: FavoriteWidgetTypography,
) {
  private val width = size.width - 32.dp
  private val inner = width - FavoriteWidgetCardPadding * 2
  private val body = (frame as? FavoriteWidgetFrame.Team)?.large?.body
  private val detail = (body as? TeamDisplayBody.Visible)?.detail
  private val history = (body as? TeamDisplayBody.Hidden)?.history
  val rowCount = when (detail) {
    is TeamDetail.Maps -> detail.rows.size
    is TeamDetail.History -> detail.rows.size
    null -> history?.rows?.size ?: if (frame is FavoriteWidgetFrame.Team) 0 else 4
  }
  private fun text(
    text: String,
    sp: Int,
    available: Dp,
    properName: Boolean = false,
    weight: FavoriteWidgetWeight = FavoriteWidgetWeight.Normal,
  ) = typography.height(text, sp, available, properName = properName, weight = weight)

  fun rowHeight(rows: Int): Dp = when (detail) {
    is TeamDetail.Maps -> detail.rows.take(rows).maxOfOrNull { row ->
      val score = teamMapScoreWidth(typography, row.score, inner, 0)
      maxOf(
        if (detail.compact) 40.dp else 48.dp,
        TeamRowInset + maxOf(
          text(row.name, FavoriteWidgetFont.Row, inner - score - TeamRowGap, true, FavoriteWidgetWeight.Medium) +
            (row.subtitle?.let { text(it, FavoriteWidgetFont.Body, inner - score - TeamRowGap) } ?: 0.dp),
          typography.height(row.score, FavoriteWidgetFont.Title, score, maxLines = 1, weight = FavoriteWidgetWeight.Medium),
        ),
      )
    } ?: 0.dp

    is TeamDetail.History -> detail.rows.take(rows).maxOfOrNull { row ->
      val columns = teamHistoryColumns(typography, row, inner, 0)
      maxOf(
        40.dp,
        TeamRowInset + maxOf(
          columns.chip.height,
          TeamRowLogo,
          text(row.name, FavoriteWidgetFont.Row, columns.name, properName = true),
          text(row.date, FavoriteWidgetFont.Label, columns.date),
          typography.height(row.score, FavoriteWidgetFont.Title, columns.score, maxLines = 1, weight = FavoriteWidgetWeight.Medium),
        ),
      )
    } ?: 0.dp

    null -> history?.rows?.take(rows)?.maxOfOrNull { row ->
      val date = typography.column(row.date, FavoriteWidgetFont.Label, inner, .3f)
      maxOf(
        40.dp,
        TeamRowInset + maxOf(
          TeamRowLogo,
          text(row.name, FavoriteWidgetFont.Row, inner - TeamRowLogo - TeamRowGap * 2 - date, properName = true),
          text(row.date, FavoriteWidgetFont.Label, date),
        ),
      )
    } ?: 40.dp
  }

  fun height(fit: FavoriteWidgetPresentation): Dp {
    fun text(
      text: String,
      sp: Int,
      available: Dp = width,
      properName: Boolean = false,
      weight: FavoriteWidgetWeight = FavoriteWidgetWeight.Normal,
    ) = typography.height(text, fit.textSize(sp), available, properName = properName, weight = weight)
    val logoSize = fit.headerLogo
    val headerText = width - logoSize - FavoriteWidgetHeaderGap
    val main = when (frame) {
      is FavoriteWidgetFrame.Setup -> {
        val subtitle = frame.text.subtitle.takeIf { FavoriteWidgetPart.Context in fit }
        val header = maxOf(
          logoSize,
          text(frame.text.title, FavoriteWidgetFont.Title, headerText, weight = FavoriteWidgetWeight.Medium) +
            (subtitle?.let { text(it, FavoriteWidgetFont.Body, headerText) } ?: 0.dp),
        )
        header + fit.heroGap + text(frame.text.lead, FavoriteWidgetFont.Body, weight = FavoriteWidgetWeight.Medium) +
          typography.height(frame.text.action, fit.heroSize, width, maxLines = 1) +
          if (FavoriteWidgetPart.Context in fit) text(frame.text.instruction, FavoriteWidgetFont.Body) else 0.dp
      }

      is FavoriteWidgetFrame.Placeholder ->
        maxOf(logoSize, text(frame.text.title, FavoriteWidgetFont.Title, headerText, weight = FavoriteWidgetWeight.Medium)) +
          fit.heroGap + typography.height(frame.text.value, fit.heroSize, width, maxLines = 1)

      is FavoriteWidgetFrame.Team -> {
        val content = if (fit.large) frame.large else frame.medium
        val formCount = if (FavoriteWidgetPart.Form in fit) content.body.formSize else 0
        val headerWidth =
          headerText - typography.roundedToPixel(FavoriteWidgetDotSize + FavoriteWidgetDotGap) * formCount
        val rank = if (FavoriteWidgetPart.RegionalRank in fit) content.header.ranking else frame.medium.header.ranking
        val rankHeight =
          if (rank.isEmpty()) 0.dp else typography.height(rank, fit.textSize(FavoriteWidgetFont.Body), headerWidth, maxLines = 1)
        val header = maxOf(
          logoSize,
          text(content.header.title, FavoriteWidgetFont.Title, headerWidth, properName = true, weight = FavoriteWidgetWeight.Medium) +
            rankHeight,
        )
        val hero = teamHero(content.body)
        val heroWidth = width - if (hero.opponent == null) 0.dp else fit.opponentLogo + 12.dp
        val labelWidth = typography.column(
          hero.caption,
          fit.textSize(FavoriteWidgetFont.Body),
          heroWidth,
          .45f,
          FavoriteWidgetWeight.Medium,
        )
        val heroHeight = typography.heroHeight(
          FavoriteWidgetHeroMeasure(
            hero.lead,
            hero.value,
            hero.caption,
            heroWidth,
            labelWidth,
            fit.heroSize,
            if (hero.opponent == null) 0.dp else fit.opponentLogo,
            hero.opponent != null,
          ),
          fit.textStep,
        )
        val below = if (hero.pills != null && FavoriteWidgetPart.Pills in fit) {
          typography.pillsHeight(hero.pills, width, fit.textStep)
        } else if (hero.context != null && FavoriteWidgetPart.Context in fit) {
          text(hero.context, FavoriteWidgetFont.Body)
        } else {
          0.dp
        }
        header + fit.heroGap + heroHeight + below
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
      fit.heroGap + typography.cardChrome(heading, inner, fit.rows, fit.textStep) + rowHeight(fit.rows) * fit.rows
    } else {
      0.dp
    }
    return fit.verticalPadding * 2 + main + listHeight
  }
}

private data class TeamHeroParts(
  val lead: String,
  val value: String,
  val opponent: LogoMark?,
  val caption: String?,
  val live: Boolean,
  val context: String?,
  val pills: List<PillText>?,
  val tap: WidgetTap.Entity?,
)

private val TeamDisplayBody.formSize: Int
  get() = when (this) {
    is TeamDisplayBody.Visible -> form.size
    is TeamDisplayBody.Hidden -> form.size
  }

private fun teamHero(body: TeamDisplayBody): TeamHeroParts = when (body) {
  is TeamDisplayBody.Visible -> when (val hero = body.hero) {
    is TeamHeroText.Upcoming ->
      TeamHeroParts(hero.lead, hero.value, hero.opponent, hero.day, false, hero.context, null, hero.tap)
    is TeamHeroText.Live -> TeamHeroParts(
      hero.lead,
      hero.value,
      hero.opponent,
      hero.liveLabel,
      true,
      (hero.below as? TeamLiveBelow.Context)?.text,
      (hero.below as? TeamLiveBelow.Pills)?.items,
      hero.tap,
    )
    is TeamHeroText.Idle -> TeamHeroParts(hero.lead, hero.value, hero.opponent, null, false, hero.context, null, hero.tap)
  }

  is TeamDisplayBody.Hidden -> when (val hero = body.hero) {
    is NeutralTeamHeroText.Upcoming ->
      TeamHeroParts(hero.lead, hero.value, hero.opponent, hero.day, false, hero.context, null, hero.tap)
    is NeutralTeamHeroText.Live ->
      TeamHeroParts(hero.lead, hero.value, hero.opponent, hero.liveLabel, true, hero.context, null, hero.tap)
    is NeutralTeamHeroText.Idle ->
      TeamHeroParts(hero.lead, hero.value, hero.opponent, null, false, hero.context, null, hero.tap)
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
            frame.text.subtitle.takeIf { FavoriteWidgetPart.Context in presentation },
            presentation.headerLogo,
          )
          FavoriteWidgetHeroSpacer(presentation)
          FavoriteWidgetText(frame.text.lead, FavoriteWidgetFont.Body, FavoriteWidgetSecondary, FavoriteWidgetWeight.Medium)
          FavoriteWidgetText(frame.text.action, presentation.heroSize, textStep = 0, maxLines = 1)
          if (FavoriteWidgetPart.Context in presentation) {
            FavoriteWidgetText(frame.text.instruction, FavoriteWidgetFont.Body, FavoriteWidgetSecondary)
          }
          TeamGhostHistory(frame.text.ghost, presentation)
        }

        is FavoriteWidgetFrame.Placeholder -> {
          FavoriteWidgetEmptyHeader(frame.text.title, null, presentation.headerLogo)
          FavoriteWidgetHeroSpacer(presentation)
          FavoriteWidgetText(frame.text.value, presentation.heroSize, textStep = 0, maxLines = 1)
          TeamGhostHistory(frame.text.ghost, presentation)
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
private fun ColumnScope.TeamGhostHistory(heading: String, presentation: FavoriteWidgetPresentation) {
  if (presentation.rows == 0) return
  Spacer(GlanceModifier.height(presentation.heroGap))
  FavoriteWidgetCard(
    heading,
    GlanceModifier.defaultWeight(),
    listOf(118.dp, 84.dp, 140.dp, 96.dp).take(presentation.rows).map { barWidth ->
      FavoriteWidgetCardRow(null) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
          Box(
            GlanceModifier.size(TeamRowLogo).cornerRadius(6.dp).background(FavoriteWidgetCardOutline),
          ) {}
          Spacer(GlanceModifier.width(TeamRowGap))
          Box(
            GlanceModifier.width(barWidth).height(10.dp).cornerRadius(5.dp)
              .background(FavoriteWidgetCardOutline),
          ) {}
        }
      }
    },
  )
}

@Composable
private fun ColumnScope.TeamPresentationLayout(
  value: TeamPresentation,
  logos: WidgetBitmaps,
  width: Dp,
  presentation: FavoriteWidgetPresentation,
) {
  val logoSize = presentation.headerLogo
  val formCount = if (FavoriteWidgetPart.Form in presentation) value.body.formSize else 0
  val form = FavoriteWidgetTypography(LocalContext.current)
    .roundedToPixel(FavoriteWidgetDotSize + FavoriteWidgetDotGap) * formCount
  Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
    FavoriteWidgetLogo(value.header.team, logos, logoSize)
    Spacer(GlanceModifier.width(FavoriteWidgetHeaderGap))
    Column(GlanceModifier.defaultWeight()) {
      FavoriteWidgetText(
        value.header.title,
        FavoriteWidgetFont.Title,
        weight = FavoriteWidgetWeight.Medium,
        properName = true,
        width = width - logoSize - FavoriteWidgetHeaderGap - form,
      )
      if (value.header.ranking.isNotEmpty()) {
        FavoriteWidgetText(value.header.ranking, FavoriteWidgetFont.Body, FavoriteWidgetSecondary, maxLines = 1)
      }
    }
    if (FavoriteWidgetPart.Form in presentation) {
      Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        when (val body = value.body) {
          is TeamDisplayBody.Visible -> body.form.forEach {
            FavoriteWidgetDot(
              it.label,
              when (it.mark) {
                FormMark.Win -> FavoriteWidgetDotStyle.Win
                FormMark.Loss -> FavoriteWidgetDotStyle.Loss
                FormMark.Live -> FavoriteWidgetDotStyle.Live
                FormMark.Empty -> FavoriteWidgetDotStyle.Empty
              },
            )
          }

          is TeamDisplayBody.Hidden -> body.form.forEach {
            FavoriteWidgetDot(
              it.label,
              if (it.mark == NeutralFormMark.Live) FavoriteWidgetDotStyle.Live else FavoriteWidgetDotStyle.Played,
            )
          }
        }
      }
    }
  }
  val hero = teamHero(value.body)
  FavoriteWidgetHeroSpacer(presentation)
  Column(GlanceModifier.fillMaxWidth().opens(hero.tap)) {
    TeamHero(hero, logos, width, presentation)
    if (hero.pills != null && FavoriteWidgetPart.Pills in presentation) {
      FavoriteWidgetMapPills(hero.pills)
    } else if (hero.context != null && FavoriteWidgetPart.Context in presentation) {
      FavoriteWidgetText(hero.context, FavoriteWidgetFont.Body, FavoriteWidgetSecondary)
    }
  }
  if (presentation.rows == 0) return
  when (val body = value.body) {
    is TeamDisplayBody.Visible -> body.detail?.let { detail ->
      Spacer(GlanceModifier.height(presentation.heroGap))
      TeamDetailCard(detail, logos, width - FavoriteWidgetCardPadding * 2, presentation.rows)
    }

    is TeamDisplayBody.Hidden -> body.history?.let {
      Spacer(GlanceModifier.height(presentation.heroGap))
      TeamNeutralHistory(it, logos, width - FavoriteWidgetCardPadding * 2, presentation.rows)
    }
  }
}

@Composable
private fun TeamHero(hero: TeamHeroParts, logos: WidgetBitmaps, width: Dp, presentation: FavoriteWidgetPresentation) {
  val textWidth = width - if (hero.opponent == null) 0.dp else presentation.opponentLogo + 12.dp
  val typography = FavoriteWidgetTypography(LocalContext.current)
  val step = LocalFavoriteWidgetTextStep.current
  val labelWidth = typography.column(hero.caption, FavoriteWidgetFont.Body - step, textWidth, .45f, FavoriteWidgetWeight.Medium)
  val rowHeight = typography.heroHeight(
    FavoriteWidgetHeroMeasure(
      hero.lead,
      hero.value,
      hero.caption,
      textWidth,
      labelWidth,
      presentation.heroSize,
      if (hero.opponent == null) 0.dp else presentation.opponentLogo,
      hero.opponent != null,
    ),
    step,
  )
  val leadWidth = textWidth - if (hero.caption == null) 0.dp else labelWidth + FavoriteWidgetCaptionGap
  Row(GlanceModifier.fillMaxWidth().height(rowHeight), verticalAlignment = Alignment.Vertical.CenterVertically) {
    Column(GlanceModifier.defaultWeight()) {
      Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        FavoriteWidgetText(
          hero.lead,
          FavoriteWidgetFont.Body,
          weight = FavoriteWidgetWeight.Medium,
          properName = hero.opponent != null,
          modifier = GlanceModifier.defaultWeight(),
          width = leadWidth,
        )
        hero.caption?.let {
          Spacer(GlanceModifier.width(FavoriteWidgetCaptionGap))
          FavoriteWidgetText(
            it,
            FavoriteWidgetFont.Body,
            if (hero.live) GlanceTheme.colors.error else FavoriteWidgetSecondary,
            FavoriteWidgetWeight.Medium,
            description = it.takeIf { hero.live },
            modifier = GlanceModifier.width(labelWidth),
            align = if (hero.live) TextAlign.Start else TextAlign.End,
          )
        }
      }
      FavoriteWidgetText(hero.value, presentation.heroSize, textStep = 0, maxLines = 1)
    }
    hero.opponent?.let {
      Spacer(GlanceModifier.width(12.dp))
      FavoriteWidgetLogo(it, logos, presentation.opponentLogo)
    }
  }
}

private fun teamMapScoreWidth(typography: FavoriteWidgetTypography, score: String, width: Dp, textStep: Int): Dp =
  typography.column(score, FavoriteWidgetFont.Title - textStep, width, .35f, FavoriteWidgetWeight.Medium)

private data class TeamHistoryColumns(val chip: DpSize, val date: Dp, val score: Dp, val name: Dp)

private fun teamHistoryColumns(
  typography: FavoriteWidgetTypography,
  row: TeamRowText,
  width: Dp,
  textStep: Int,
): TeamHistoryColumns {
  val chip = typography.chipSize(row.mark, textStep)
  val dateWidth = typography.column(row.date, FavoriteWidgetFont.Label - textStep, width, .25f)
  val scoreWidth = typography.column(row.score, FavoriteWidgetFont.Title - textStep, width, .25f, FavoriteWidgetWeight.Medium)
  return TeamHistoryColumns(
    chip,
    dateWidth,
    scoreWidth,
    width - chip.width - TeamRowLogo - dateWidth - scoreWidth - TeamRowGap * 2 - 8.dp * 2,
  )
}

@Composable
private fun ColumnScope.TeamDetailCard(detail: TeamDetail, logos: WidgetBitmaps, width: Dp, rowCount: Int) {
  val typography = FavoriteWidgetTypography(LocalContext.current)
  val step = LocalFavoriteWidgetTextStep.current
  when (detail) {
    is TeamDetail.Maps -> FavoriteWidgetCard(
      detail.heading,
      GlanceModifier.defaultWeight(),
      detail.rows.take(rowCount).map { map ->
        FavoriteWidgetCardRow(map.tap) {
          val score = teamMapScoreWidth(typography, map.score, width, step)
          Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
              FavoriteWidgetText(
                map.name,
                FavoriteWidgetFont.Row,
                weight = FavoriteWidgetWeight.Medium,
                properName = true,
                width = width - TeamRowGap - score,
              )
              map.subtitle?.let { FavoriteWidgetText(it, FavoriteWidgetFont.Body, FavoriteWidgetSecondary) }
            }
            Spacer(GlanceModifier.width(TeamRowGap))
            FavoriteWidgetText(
              map.score,
              FavoriteWidgetFont.Title,
              map.tone.color,
              FavoriteWidgetWeight.Medium,
              maxLines = 1,
              modifier = GlanceModifier.width(score),
              align = TextAlign.End,
            )
          }
        }
      },
    )

    is TeamDetail.History -> FavoriteWidgetCard(
      detail.heading,
      GlanceModifier.defaultWeight(),
      detail.rows.take(rowCount).map { row ->
        FavoriteWidgetCardRow(row.tap) {
          val columns = teamHistoryColumns(typography, row, width, step)
          val win = row.tone == ResultTone.Win
          Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            FavoriteWidgetChip(row.mark, win, columns.chip)
            Spacer(GlanceModifier.width(TeamRowGap))
            FavoriteWidgetLogo(row.opponent, logos, TeamRowLogo)
            Spacer(GlanceModifier.width(TeamRowGap))
            FavoriteWidgetText(row.name, FavoriteWidgetFont.Row, modifier = GlanceModifier.defaultWeight(), properName = true)
            Spacer(GlanceModifier.width(8.dp))
            FavoriteWidgetText(
              row.date,
              FavoriteWidgetFont.Label,
              FavoriteWidgetSecondary,
              modifier = GlanceModifier.width(columns.date),
              align = TextAlign.End,
            )
            Spacer(GlanceModifier.width(8.dp))
            FavoriteWidgetText(
              row.score,
              FavoriteWidgetFont.Title,
              if (win) GlanceTheme.colors.onSurface else FavoriteWidgetSecondary,
              FavoriteWidgetWeight.Medium,
              maxLines = 1,
              modifier = GlanceModifier.width(columns.score),
              align = TextAlign.End,
            )
          }
        }
      },
    )
  }
}

@Composable
private fun ColumnScope.TeamNeutralHistory(
  history: NeutralTeamHistoryText,
  logos: WidgetBitmaps,
  width: Dp,
  rowCount: Int,
) {
  val typography = FavoriteWidgetTypography(LocalContext.current)
  val step = LocalFavoriteWidgetTextStep.current
  FavoriteWidgetCard(
    history.heading,
    GlanceModifier.defaultWeight(),
    history.rows.take(rowCount).map { row ->
      FavoriteWidgetCardRow(row.tap) {
        val date = typography.column(row.date, FavoriteWidgetFont.Label - step, width, .3f)
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
          FavoriteWidgetLogo(row.opponent, logos, TeamRowLogo)
          Spacer(GlanceModifier.width(TeamRowGap))
          FavoriteWidgetText(row.name, FavoriteWidgetFont.Row, modifier = GlanceModifier.defaultWeight(), properName = true)
          Spacer(GlanceModifier.width(TeamRowGap))
          FavoriteWidgetText(
            row.date,
            FavoriteWidgetFont.Label,
            FavoriteWidgetSecondary,
            modifier = GlanceModifier.width(date),
            align = TextAlign.End,
          )
        }
      }
    },
  )
}
