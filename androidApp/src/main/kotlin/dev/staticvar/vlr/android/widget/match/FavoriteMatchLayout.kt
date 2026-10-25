/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.match

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
import dev.staticvar.vlr.android.MainActivity
import dev.staticvar.vlr.android.R
import dev.staticvar.vlr.android.widget.favorite.FavoriteWidgetFont
import dev.staticvar.vlr.android.widget.favorite.FavoriteWidgetLogo
import dev.staticvar.vlr.android.widget.favorite.FavoriteWidgetText
import dev.staticvar.vlr.android.widget.favorite.FavoriteWidgetTypography
import dev.staticvar.vlr.android.widget.favorite.FavoriteWidgetWeight
import dev.staticvar.vlr.android.widget.favorite.WidgetBitmaps
import dev.staticvar.vlr.android.widget.favorite.opens
import dev.staticvar.vlr.shared.widget.favorite.WidgetTap

internal enum class FavoriteMatchVariant(val horizontalPadding: Dp, val verticalPadding: Dp) {
  Compact(12.dp, 12.dp),
  Wide(16.dp, 12.dp),
  Tall(16.dp, 14.dp),
  ;

  companion object {
    fun of(size: DpSize): FavoriteMatchVariant = when {
      size.width < 250.dp -> Compact
      size.height < 180.dp -> Wide
      else -> Tall
    }
  }
}

@Composable
internal fun FavoriteMatchLayout(copy: FavoriteMatchCopy, logos: WidgetBitmaps) {
  val size = LocalSize.current
  val variant = FavoriteMatchVariant.of(size)
  val context = LocalContext.current
  val root = GlanceModifier.fillMaxSize()
  val tappable = when (copy) {
    is FavoriteMatchCopy.Match -> root.opens(WidgetTap.Match(copy.matchId))
    is FavoriteMatchCopy.Message -> root.clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
  }
  Column(tappable.padding(horizontal = variant.horizontalPadding, vertical = variant.verticalPadding)) {
    val width = size.width - variant.horizontalPadding * 2
    when (copy) {
      is FavoriteMatchCopy.Message -> Message(copy, DpSize(width, size.height - variant.verticalPadding * 2))
      is FavoriteMatchCopy.Match -> when (variant) {
        FavoriteMatchVariant.Compact -> Compact(copy, logos, width)
        FavoriteMatchVariant.Wide -> Wide(copy, logos, width)
        FavoriteMatchVariant.Tall -> Tall(copy, logos, width)
      }
    }
  }
}

@Composable
private fun Message(copy: FavoriteMatchCopy.Message, size: DpSize) {
  val typography = FavoriteWidgetTypography(LocalContext.current)
  val title = typography.height(copy.title, FavoriteWidgetFont.Title, size.width, maxLines = 2, weight = FavoriteWidgetWeight.Medium)
  val caption = typography.height(copy.caption, FavoriteWidgetFont.Body, size.width, maxLines = 3)
  // A narrow widget keeps the title and drops a caption that would be cut off.
  val showCaption = title + MessageGap + caption <= size.height
  Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
    @Composable
    fun Caption() = FavoriteWidgetText(
      copy.caption,
      FavoriteWidgetFont.Body,
      GlanceTheme.colors.onSurfaceVariant,
      maxLines = 3,
      modifier = GlanceModifier.width(size.width),
    )
    if (showCaption && copy.captionFirst) Caption()
    FavoriteWidgetText(
      copy.title,
      FavoriteWidgetFont.Title,
      weight = FavoriteWidgetWeight.Medium,
      maxLines = 2,
      modifier = GlanceModifier.width(size.width),
    )
    if (showCaption && !copy.captionFirst) {
      Spacer(GlanceModifier.height(MessageGap))
      Caption()
    }
  }
}

@Composable
private fun ColumnScope.Tall(copy: FavoriteMatchCopy.Match, logos: WidgetBitmaps, width: Dp) {
  val typography = FavoriteWidgetTypography(LocalContext.current)
  Header(copy.status, copy.event, null, width, typography)
  Spacer(GlanceModifier.defaultWeight())
  val roomy = width >= RoomyTallWidth
  StackedHero(
    copy,
    logos,
    width,
    if (roomy) RoomyTallScore else TallScore,
    if (roomy) RoomyTallLogo else TallLogo,
    TallNameSizes,
    6.dp,
    typography,
  )
  Spacer(GlanceModifier.defaultWeight())
  val next = copy.next
  Box(GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    if (next != null) {
      Box(
        GlanceModifier.cornerRadius(12.dp).background(GlanceTheme.colors.secondaryContainer)
          .padding(horizontal = 10.dp, vertical = 6.dp).opens(WidgetTap.Match(next.matchId)),
        contentAlignment = Alignment.Center,
      ) {
        FavoriteWidgetText(
          next.text,
          FavoriteWidgetFont.Body,
          GlanceTheme.colors.onSecondaryContainer,
          FavoriteWidgetWeight.Medium,
          properName = true,
          width = width - 20.dp,
        )
      }
    } else if (copy.footer != null) {
      FavoriteWidgetText(
        copy.footer,
        FavoriteWidgetFont.Body,
        GlanceTheme.colors.onSurfaceVariant,
        align = TextAlign.Center,
        properName = true,
        width = width,
        modifier = GlanceModifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun ColumnScope.Wide(copy: FavoriteMatchCopy.Match, logos: WidgetBitmaps, width: Dp) {
  val typography = FavoriteWidgetTypography(LocalContext.current)
  Header(copy.status, copy.next?.text ?: copy.event, copy.next?.matchId, width, typography)
  Spacer(GlanceModifier.defaultWeight())
  val center = centerWidth(copy.center, WideScore, typography)
  val name = (width - center - WideLogo * 2 - WideGap * 4) / 2
  // Names show only when both fit whole, so every state at one size looks alike.
  val nameSize = fittingNameSize(copy, name, WideNameSizes, typography)
  Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
    FavoriteWidgetLogo(copy.team1.logo, logos, WideLogo)
    Spacer(GlanceModifier.width(WideGap))
    Box(GlanceModifier.defaultWeight(), contentAlignment = Alignment.CenterStart) {
      if (nameSize != null) TeamName(copy.team1, copy.status.tone, nameSize, name, TextAlign.Start)
    }
    Spacer(GlanceModifier.width(WideGap))
    Center(copy, WideScore, center, null, typography)
    Spacer(GlanceModifier.width(WideGap))
    Box(GlanceModifier.defaultWeight(), contentAlignment = Alignment.CenterEnd) {
      if (nameSize != null) TeamName(copy.team2, copy.status.tone, nameSize, name, TextAlign.End)
    }
    Spacer(GlanceModifier.width(WideGap))
    FavoriteWidgetLogo(copy.team2.logo, logos, WideLogo)
  }
  Spacer(GlanceModifier.defaultWeight())
}

@Composable
private fun ColumnScope.Compact(copy: FavoriteMatchCopy.Match, logos: WidgetBitmaps, width: Dp) {
  val typography = FavoriteWidgetTypography(LocalContext.current)
  StatusLabel(copy.status, width)
  Spacer(GlanceModifier.defaultWeight())
  StackedHero(copy, logos, width, CompactScore, CompactLogo, CompactNameSizes, 4.dp, typography)
  Spacer(GlanceModifier.defaultWeight())
}

@Composable
private fun Header(status: FavoriteMatchStatus, trailing: String, trailingMatch: String?, width: Dp, typography: FavoriteWidgetTypography) {
  val statusMax = width * StatusShare
  val statusWidth = minOf(statusMax, statusWidth(status, typography))
  Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
    StatusLabel(status, statusMax)
    Spacer(GlanceModifier.width(HeaderGap))
    val modifier = GlanceModifier.defaultWeight()
    FavoriteWidgetText(
      trailing,
      FavoriteWidgetFont.Label,
      GlanceTheme.colors.onSurfaceVariant,
      align = TextAlign.End,
      properName = true,
      width = width - statusWidth - HeaderGap,
      modifier = if (trailingMatch == null) modifier else modifier.opens(WidgetTap.Match(trailingMatch)),
    )
  }
}

/** Draws the status label, ellipsized only when it is wider than [maxWidth]. */
@Composable
private fun StatusLabel(status: FavoriteMatchStatus, maxWidth: Dp) {
  Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
    val live = status.tone == FavoriteMatchStatusTone.Live
    if (live) {
      Image(
        ImageProvider(R.drawable.favorite_widget_live),
        null,
        GlanceModifier.size(LiveDot),
        colorFilter = ColorFilter.tint(GlanceTheme.colors.error),
      )
      Spacer(GlanceModifier.width(LiveDotGap))
    }
    FavoriteWidgetText(
      status.text,
      FavoriteWidgetFont.Label,
      status.tone.color,
      FavoriteWidgetWeight.Bold,
      properName = true,
      width = if (live) maxWidth - LiveDot - LiveDotGap else maxWidth,
    )
  }
}

/** Logos with names under them and the score level with the logos, as on a scoreboard. */
@Composable
private fun StackedHero(
  copy: FavoriteMatchCopy.Match,
  logos: WidgetBitmaps,
  width: Dp,
  score: Int,
  maxLogo: Dp,
  nameSizes: List<Int>,
  gap: Dp,
  typography: FavoriteWidgetTypography,
) {
  // Columns reserve room for a score in every state, so logos keep one size as the match moves on.
  val center = maxOf(centerWidth(copy.center, score, typography), centerWidth(ScoreReserve, score, typography))
  val side = (width - center) / 2
  val logo = minOf(maxLogo, side)
  val nameSize = fittingNameSize(copy, side, nameSizes, typography) ?: nameSizes.last().takeIf { side >= MinimumName }
  val top = maxOf(logo, typography.height("0", score, center, maxLines = 1, weight = FavoriteWidgetWeight.CondensedBold))
  Row(GlanceModifier.fillMaxWidth()) {
    StackedTeam(copy.team1, copy.status.tone, logos, logo, top, nameSize, gap, side)
    Center(copy, score, center, top, typography)
    StackedTeam(copy.team2, copy.status.tone, logos, logo, top, nameSize, gap, side)
  }
}

private fun fittingNameSize(copy: FavoriteMatchCopy.Match, width: Dp, sizes: List<Int>, typography: FavoriteWidgetTypography): Int? =
  sizes.firstOrNull { size ->
    listOf(copy.team1, copy.team2).all { typography.width(it.name, size, FavoriteWidgetWeight.Medium) <= width - 1.dp }
  }

@Composable
private fun RowScope.StackedTeam(
  team: FavoriteMatchTeam,
  status: FavoriteMatchStatusTone,
  logos: WidgetBitmaps,
  logo: Dp,
  top: Dp,
  nameSize: Int?,
  gap: Dp,
  width: Dp,
) {
  Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
    Box(GlanceModifier.height(top), contentAlignment = Alignment.Center) {
      FavoriteWidgetLogo(team.logo, logos, logo)
    }
    if (nameSize != null) {
      Spacer(GlanceModifier.height(gap))
      TeamName(team, status, nameSize, width, TextAlign.Center)
    }
  }
}

@Composable
private fun TeamName(team: FavoriteMatchTeam, status: FavoriteMatchStatusTone, size: Int, width: Dp, align: TextAlign) {
  FavoriteWidgetText(
    team.name,
    size,
    if (status == FavoriteMatchStatusTone.Final && team.tone == FavoriteMatchTeamTone.Loser) {
      GlanceTheme.colors.onSurfaceVariant
    } else {
      GlanceTheme.colors.onSurface
    },
    FavoriteWidgetWeight.Medium,
    align = align,
    properName = true,
    width = width,
  )
}

@Composable
private fun Center(copy: FavoriteMatchCopy.Match, score: Int, width: Dp, height: Dp?, typography: FavoriteWidgetTypography) {
  val box = GlanceModifier.width(width)
  Box(if (height == null) box else box.height(height), contentAlignment = Alignment.Center) {
    when (val center = copy.center) {
      is FavoriteMatchCenter.Score -> Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        val live = copy.status.tone == FavoriteMatchStatusTone.Live
        val gap = scoreGap(score)
        ScoreText(center.team1, score, scoreColor(live, copy.team1.tone), typography)
        Spacer(GlanceModifier.width(gap))
        ScoreText(Dash, score, if (live) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant, typography)
        Spacer(GlanceModifier.width(gap))
        ScoreText(center.team2, score, scoreColor(live, copy.team2.tone), typography)
      }

      is FavoriteMatchCenter.Versus -> FavoriteWidgetText(
        center.text,
        versusSize(score),
        GlanceTheme.colors.onSurfaceVariant,
        FavoriteWidgetWeight.Bold,
        align = TextAlign.Center,
        properName = true,
        width = width,
      )

      is FavoriteMatchCenter.Unknown -> FavoriteWidgetText(
        Dash,
        score,
        GlanceTheme.colors.onSurfaceVariant,
        FavoriteWidgetWeight.CondensedBold,
        align = TextAlign.Center,
        maxLines = 1,
        description = center.description,
      )
    }
  }
}

@Composable
private fun ScoreText(text: String, size: Int, color: ColorProvider, typography: FavoriteWidgetTypography) {
  FavoriteWidgetText(
    text,
    size,
    color,
    FavoriteWidgetWeight.CondensedBold,
    maxLines = 1,
    align = TextAlign.Center,
    modifier = GlanceModifier.width(typography.width(text, size, FavoriteWidgetWeight.CondensedBold)),
  )
}

@Composable
private fun scoreColor(live: Boolean, tone: FavoriteMatchTeamTone): ColorProvider =
  if (!live && tone == FavoriteMatchTeamTone.Loser) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.primary

private val FavoriteMatchStatusTone.color: ColorProvider
  @Composable get() = when (this) {
    FavoriteMatchStatusTone.Live -> GlanceTheme.colors.error
    FavoriteMatchStatusTone.Final -> GlanceTheme.colors.onSurfaceVariant
    FavoriteMatchStatusTone.Kickoff -> GlanceTheme.colors.primary
  }

private fun statusWidth(status: FavoriteMatchStatus, typography: FavoriteWidgetTypography): Dp {
  val text = typography.width(status.text, FavoriteWidgetFont.Label, FavoriteWidgetWeight.Bold)
  return if (status.tone == FavoriteMatchStatusTone.Live) text + LiveDot + LiveDotGap else text
}

private fun centerWidth(center: FavoriteMatchCenter, score: Int, typography: FavoriteWidgetTypography): Dp = CenterInset + when (center) {
  is FavoriteMatchCenter.Score -> listOf(center.team1, Dash, center.team2)
    .fold(scoreGap(score) * 2) { total, text -> total + typography.width(text, score, FavoriteWidgetWeight.CondensedBold) }
  is FavoriteMatchCenter.Versus -> typography.width(center.text, versusSize(score), FavoriteWidgetWeight.Bold)
  is FavoriteMatchCenter.Unknown -> typography.width(Dash, score, FavoriteWidgetWeight.CondensedBold)
}

private fun scoreGap(score: Int): Dp = if (score >= TallScore) 6.dp else 3.dp
private fun versusSize(score: Int): Int = if (score >= WideScore) 16 else FavoriteWidgetFont.Row

private const val Dash = "–"
private const val TallScore = 34
private const val WideScore = 28
private const val CompactScore = 20
private val TallLogo = 44.dp
private const val RoomyTallScore = 40
private val RoomyTallLogo = 52.dp
private val RoomyTallWidth = 280.dp
private val WideLogo = 32.dp
private val CompactLogo = 36.dp
private val WideGap = 8.dp
private val HeaderGap = 8.dp
private val CenterInset = 8.dp
private val LiveDot = 10.dp
private val LiveDotGap = 4.dp
private val MinimumName = 40.dp
private val MessageGap = 2.dp
private val TallNameSizes = listOf(FavoriteWidgetFont.Row, FavoriteWidgetFont.Body, FavoriteWidgetFont.Label)
private val CompactNameSizes = listOf(FavoriteWidgetFont.Label)
private val WideNameSizes = listOf(FavoriteWidgetFont.Row, FavoriteWidgetFont.Body)
private val ScoreReserve = FavoriteMatchCenter.Score("0", "0")
private const val StatusShare = 0.6f
