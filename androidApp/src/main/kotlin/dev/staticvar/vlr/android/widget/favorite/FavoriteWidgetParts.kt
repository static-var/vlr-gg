/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.text.LineBreaker
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.LruCache
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.staticvar.vlr.android.R
import dev.staticvar.vlr.shared.widget.favorite.LogoMark
import dev.staticvar.vlr.shared.widget.favorite.PillText
import dev.staticvar.vlr.shared.widget.favorite.PillTone
import kotlin.math.ceil
import kotlin.math.roundToInt

internal enum class FavoriteWidgetPart { Form, Context, Pills, RegionalRank, Progress, Subtitle, Beside, OtherLive }

internal data class FavoriteWidgetPresentation(
  val large: Boolean,
  val heroSize: Int,
  val parts: Set<FavoriteWidgetPart>,
  val rows: Int,
  val rowHeight: Dp,
  val compact: Boolean,
  val textStep: Int = 0,
) {
  val verticalPadding: Dp get() = if (compact) 8.dp else 16.dp
  val heroGap: Dp get() = if (compact) 6.dp else 12.dp
  val headerLogo: Dp get() = when {
    compact -> 28.dp
    large -> 40.dp
    else -> 32.dp
  }

  fun textSize(size: Int): Int = size - textStep

  operator fun contains(part: FavoriteWidgetPart): Boolean = part in parts
}

internal fun presentationFor(
  size: DpSize,
  rowCount: Int,
  rowHeight: (Int) -> Dp,
  height: (FavoriteWidgetPresentation) -> Dp,
  largeParts: Set<FavoriteWidgetPart> =
    setOf(FavoriteWidgetPart.Form, FavoriteWidgetPart.Context, FavoriteWidgetPart.RegionalRank),
  mediumParts: Set<FavoriteWidgetPart> =
    setOf(FavoriteWidgetPart.Form, FavoriteWidgetPart.Context, FavoriteWidgetPart.Pills),
): FavoriteWidgetPresentation {
  val compact = size.height < 158.dp
  val floor = favoriteWidgetHeroFloor(size)
  val requiredLarge = FavoriteWidgetPresentation(true, floor, emptySet(), 0, rowHeight(0), compact = true)
  val large = height(requiredLarge.fittingWidth(height)) <= size.height
  val parts = if (large) largeParts else mediumParts
  val start = favoriteWidgetHeroStart(size, large)
  val candidates = buildList {
    var candidate = FavoriteWidgetPresentation(
      large,
      start,
      parts,
      if (large) rowCount else 0,
      rowHeight(if (large) rowCount else 0),
      compact = false,
    )
    add(candidate)
    while (candidate.heroSize > floor) {
      candidate = candidate.copy(heroSize = maxOf(floor, candidate.heroSize - 2))
      add(candidate)
    }
    candidate = candidate.copy(compact = true)
    add(candidate)
    while (candidate.rows > 0) {
      candidate = candidate.copy(rows = candidate.rows - 1, rowHeight = rowHeight(candidate.rows - 1))
      add(candidate)
    }
    for (part in FavoriteWidgetPartDropOrder) {
      if (part !in candidate) continue
      candidate = candidate.copy(parts = candidate.parts - part)
      add(candidate)
    }
    while (candidate.heroSize > FavoriteWidgetHeroMin) {
      candidate = candidate.copy(heroSize = candidate.heroSize - 1)
      add(candidate)
    }
    while (candidate.textStep < FavoriteWidgetMaxTextStep) {
      candidate = candidate.copy(textStep = candidate.textStep + 1, heroSize = candidate.heroSize - 1)
      add(candidate)
    }
  }
  val fitted = candidates.asSequence().map { it.fittingWidth(height) }
  return fitted.firstOrNull { height(it) <= size.height } ?: fitted.last()
}

private val FavoriteWidgetPartDropOrder = listOf(
  FavoriteWidgetPart.RegionalRank,
  FavoriteWidgetPart.OtherLive,
  FavoriteWidgetPart.Progress,
  FavoriteWidgetPart.Pills,
  FavoriteWidgetPart.Beside,
  FavoriteWidgetPart.Context,
  FavoriteWidgetPart.Subtitle,
  FavoriteWidgetPart.Form,
)

private fun FavoriteWidgetPresentation.fittingWidth(
  height: (FavoriteWidgetPresentation) -> Dp,
): FavoriteWidgetPresentation {
  fun FavoriteWidgetPresentation.fits() = height(this) != Dp.Infinity
  fun FavoriteWidgetPresentation.smallerHero() =
    (heroSize - 1 downTo FavoriteWidgetHeroMin - textStep).asSequence().map { copy(heroSize = it) }.firstOrNull { it.fits() }
  if (fits()) return this
  smallerHero()?.let { return it }
  var fitted = this
  val removed = mutableListOf<FavoriteWidgetPart>()
  for (part in FavoriteWidgetPartDropOrder) {
    if (fitted.fits()) break
    if (part !in fitted) continue
    fitted = fitted.copy(parts = fitted.parts - part)
    removed += part
  }
  if (!fitted.fits()) fitted = fitted.smallerHero() ?: return this
  for (part in removed.asReversed()) {
    fitted.copy(parts = fitted.parts + part).takeIf { it.fits() }?.let { fitted = it }
  }
  return fitted
}

private const val FavoriteWidgetHeroMin = 18
private const val FavoriteWidgetMaxTextStep = 9

internal fun favoriteWidgetHeroFloor(size: DpSize): Int = if (size.height < 158.dp) 28 else 34

internal fun favoriteWidgetHeroStart(size: DpSize, large: Boolean): Int = when {
  size.height < 158.dp -> favoriteWidgetHeroFloor(size)
  large -> 44
  else -> 40
}

private const val Ellipsis = "…"

// Text sizes in sp before the fit ladder subtracts its text step.
internal object FavoriteWidgetFont {
  const val Title = 14
  const val Row = 13
  const val Body = 12
  const val Label = 11
}

internal val LocalFavoriteWidgetTextStep = staticCompositionLocalOf { 0 }
internal val FavoriteWidgetCaptionGap = 6.dp
internal val FavoriteWidgetCardPadding = 12.dp
internal val FavoriteWidgetDivider = 1.dp

internal enum class FavoriteWidgetWeight(family: String, style: Int) {
  Normal("sans-serif", Typeface.NORMAL),
  Medium("sans-serif-medium", Typeface.NORMAL),
  Bold("sans-serif", Typeface.BOLD),
  ;

  val typeface: Typeface = Typeface.create(family, style)
  val fontFamily = FontFamily(family)
  val fontWeight = if (style == Typeface.BOLD) FontWeight.Bold else FontWeight.Normal
}

internal data class FavoriteWidgetHeroMeasure(
  val lead: String,
  val value: String,
  val caption: String?,
  val width: Dp,
  val captionWidth: Dp,
  val valueSize: Int,
  val minimumHeight: Dp,
  val properName: Boolean,
)

internal class FavoriteWidgetTypography(context: Context) {
  private val metrics = context.resources.displayMetrics

  fun roundedToPixel(size: Dp): Dp = ((size.value * metrics.density).roundToInt() / metrics.density).dp
  fun heroHeight(hero: FavoriteWidgetHeroMeasure, textStep: Int): Dp {
    val gap = if (hero.caption == null) 0.dp else FavoriteWidgetCaptionGap
    val lead = maxOf(
      height(
        hero.lead,
        FavoriteWidgetFont.Body - textStep,
        hero.width - hero.captionWidth - gap,
        properName = hero.properName,
        weight = FavoriteWidgetWeight.Medium,
      ),
      hero.caption?.let { height(it, FavoriteWidgetFont.Body - textStep, hero.captionWidth, weight = FavoriteWidgetWeight.Medium) } ?: 0.dp,
    )
    return maxOf(
      roundedToPixel(hero.minimumHeight),
      lead + height(hero.value, hero.valueSize, hero.width, maxLines = 1),
    )
  }

  private fun textPixels(size: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size.toFloat(), metrics)

  private fun paint(pixels: Float, weight: FavoriteWidgetWeight) = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
    typeface = weight.typeface
    textSize = pixels
  }

  fun width(text: String, size: Int, weight: FavoriteWidgetWeight = FavoriteWidgetWeight.Normal): Dp {
    val key = WidthKey(text, textPixels(size), weight)
    val pixels = widths[key] ?: (paint(key.textPixels, weight).measureText(text) + 2).also { widths.put(key, it) }
    return (ceil(pixels) / metrics.density).dp
  }

  // TextView sizes its ellipsis with the regular base paint, so weighted text overflows; shorten it here, with
  // 1dp spare for the layout rounding of sibling widths. TextUtils.ellipsize can also overshoot its width.
  fun ellipsize(text: String, size: Int, width: Dp, weight: FavoriteWidgetWeight): String {
    val paint = paint(textPixels(size), weight)
    val available = (width - 1.dp).value * metrics.density
    if (paint.measureText(text) <= available) return text
    var end = text.length
    while (end > 0 && paint.measureText(text.take(end).trimEnd() + Ellipsis) > available) {
      end = text.offsetByCodePoints(end, -1)
    }
    return text.take(end).trimEnd() + Ellipsis
  }

  fun column(
    text: String?,
    size: Int,
    available: Dp,
    share: Float,
    weight: FavoriteWidgetWeight = FavoriteWidgetWeight.Normal,
  ): Dp = if (text == null) 0.dp else minOf(width(text, size, weight), available * share)

  fun height(
    text: String,
    size: Int,
    width: Dp,
    maxLines: Int = 2,
    properName: Boolean = false,
    weight: FavoriteWidgetWeight = FavoriteWidgetWeight.Normal,
  ): Dp {
    if (!properName && maxLines == 1 && width(text, size, weight) > width) return Dp.Infinity
    val key = HeightKey(
      text,
      textPixels(size),
      (width.value * metrics.density).roundToInt().coerceAtLeast(1),
      if (properName) 1 else maxLines,
      properName,
      weight,
    )
    val pixels = heights[key] ?: measure(key).also { heights.put(key, it) }
    return if (pixels == Int.MAX_VALUE) Dp.Infinity else (pixels / metrics.density).dp
  }

  @SuppressLint("InlinedApi") // StaticLayout supports this inlined strategy value from API 23.
  private fun measure(key: HeightKey): Int {
    val paint = paint(key.textPixels, key.weight)
    val layout = StaticLayout.Builder.obtain(key.text, 0, key.text.length, paint, key.widthPixels)
      .setIncludePad(true)
      .setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
      .setEllipsize(TextUtils.TruncateAt.END)
      .setMaxLines(key.maxLines).build()
    val fits = key.properName || (
      layout.getLineEnd(layout.lineCount - 1) >= key.text.length &&
        (0 until layout.lineCount).none {
          layout.getLineWidth(it) > key.widthPixels || layout.getEllipsisCount(it) > 0
        }
      )
    return if (fits) layout.height else Int.MAX_VALUE
  }

  private data class WidthKey(val text: String, val textPixels: Float, val weight: FavoriteWidgetWeight)
  private data class HeightKey(
    val text: String,
    val textPixels: Float,
    val widthPixels: Int,
    val maxLines: Int,
    val properName: Boolean,
    val weight: FavoriteWidgetWeight,
  )

  private companion object {
    val widths = LruCache<WidthKey, Float>(1024)
    val heights = LruCache<HeightKey, Int>(4096)
  }
}

@Composable
internal fun FavoriteWidgetText(
  text: String,
  size: Int,
  color: ColorProvider = GlanceTheme.colors.onSurface,
  weight: FavoriteWidgetWeight = FavoriteWidgetWeight.Normal,
  maxLines: Int = 2,
  modifier: GlanceModifier = GlanceModifier,
  align: TextAlign = TextAlign.Start,
  description: String? = null,
  properName: Boolean = false,
  textStep: Int = LocalFavoriteWidgetTextStep.current,
  width: Dp? = null,
) {
  Text(
    text = if (properName && width != null) {
      FavoriteWidgetTypography(LocalContext.current).ellipsize(text, size - textStep, width, weight)
    } else {
      text
    },
    modifier = if (description == null) modifier else modifier.semantics { contentDescription = description },
    maxLines = if (properName) 1 else maxLines,
    style = TextStyle(
      color = color,
      fontSize = (size - textStep).sp,
      fontWeight = weight.fontWeight,
      fontFamily = weight.fontFamily,
      textAlign = align,
    ),
  )
}

internal val FavoriteWidgetSecondary: ColorProvider @Composable get() = GlanceTheme.colors.onSurfaceVariant

@Composable
internal fun FavoriteWidgetLogo(mark: LogoMark, logos: WidgetBitmaps, size: Dp) {
  val bitmap = (mark as? LogoMark.Named)?.source?.let(logos.bySource::get)
  val description = when (mark) {
    is LogoMark.Named -> mark.name
    LogoMark.Undecided -> LogoMark.Undecided.monogram
  }
  if (bitmap != null) {
    Image(ImageProvider(bitmap), description, GlanceModifier.size(size), contentScale = ContentScale.Fit)
  } else {
    val monogram = when (mark) {
      is LogoMark.Named -> mark.monogram
      LogoMark.Undecided -> LogoMark.Undecided.monogram
    }
    Box(
      GlanceModifier.size(size).cornerRadius(size / if (mark == LogoMark.Undecided) 2 else 4)
        .background(GlanceTheme.colors.primaryContainer)
        .semantics { contentDescription = description },
      contentAlignment = Alignment.Center,
    ) {
      val context = LocalContext.current
      val typography = FavoriteWidgetTypography(context)
      val largest = (size.value * 0.34f / context.resources.configuration.fontScale).toInt()
      val textSize = (largest downTo 4).firstOrNull {
        typography.width(monogram, it, FavoriteWidgetWeight.Medium) <= size - 6.dp
      } ?: 4
      FavoriteWidgetText(
        monogram,
        textSize,
        GlanceTheme.colors.onPrimaryContainer,
        FavoriteWidgetWeight.Medium,
        align = TextAlign.Center,
        modifier = GlanceModifier.width(size - 4.dp),
        properName = true,
        textStep = 0,
      )
    }
  }
}

internal enum class FavoriteWidgetDotStyle { Win, Loss, Live, Empty, Played }

@Composable
internal fun FavoriteWidgetDot(label: String, style: FavoriteWidgetDotStyle) {
  Image(
    ImageProvider(if (style == FavoriteWidgetDotStyle.Loss) R.drawable.favorite_widget_ring else R.drawable.favorite_widget_dot),
    label,
    GlanceModifier.width(FavoriteWidgetDotSize + FavoriteWidgetDotGap).height(FavoriteWidgetDotSize)
      .padding(start = FavoriteWidgetDotGap),
    colorFilter = ColorFilter.tint(
      when (style) {
        FavoriteWidgetDotStyle.Win -> GlanceTheme.colors.primary
        FavoriteWidgetDotStyle.Loss -> GlanceTheme.colors.outline
        FavoriteWidgetDotStyle.Live -> GlanceTheme.colors.error
        FavoriteWidgetDotStyle.Empty -> GlanceTheme.colors.surfaceVariant
        FavoriteWidgetDotStyle.Played -> GlanceTheme.colors.onSurfaceVariant
      },
    ),
  )
}

internal val FavoriteWidgetDotSize = 10.dp
internal val FavoriteWidgetDotGap = 5.dp

internal val FavoriteWidgetHeaderGap = 12.dp

@Composable
internal fun FavoriteWidgetEmptyHeader(title: String, subtitle: String?, logoSize: Dp) {
  Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
    Box(GlanceModifier.size(logoSize).cornerRadius(logoSize / 4).background(GlanceTheme.colors.secondaryContainer)) {}
    Spacer(GlanceModifier.width(FavoriteWidgetHeaderGap))
    Column(GlanceModifier.defaultWeight()) {
      FavoriteWidgetText(title, FavoriteWidgetFont.Title, weight = FavoriteWidgetWeight.Medium)
      subtitle?.let { FavoriteWidgetText(it, FavoriteWidgetFont.Body, FavoriteWidgetSecondary) }
    }
  }
}

@Composable
internal fun ColumnScope.FavoriteWidgetHeroSpacer(presentation: FavoriteWidgetPresentation) {
  Spacer(GlanceModifier.height(presentation.heroGap))
  if (presentation.rows == 0) Spacer(GlanceModifier.defaultWeight())
}

internal val FavoriteWidgetPillGap = 4.dp
internal val FavoriteWidgetPillPadding = 6.dp

internal fun FavoriteWidgetTypography.pillsHeight(items: List<PillText>, width: Dp, textStep: Int): Dp {
  val pillWidth = (width - FavoriteWidgetPillGap * (items.size - 1)) / items.size - FavoriteWidgetPillPadding * 2
  return 8.dp + FavoriteWidgetPillPadding + items.maxOf {
    height(it.text, FavoriteWidgetFont.Label - textStep, pillWidth, maxLines = 1, weight = FavoriteWidgetWeight.Medium)
  }
}

@Composable
internal fun FavoriteWidgetMapPills(items: List<PillText>) {
  Row(GlanceModifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Vertical.CenterVertically) {
    items.forEachIndexed { index, pill ->
      if (index > 0) Spacer(GlanceModifier.width(FavoriteWidgetPillGap))
      val (container, content) = when (pill.tone) {
        PillTone.Live -> GlanceTheme.colors.errorContainer to GlanceTheme.colors.onErrorContainer
        PillTone.Won -> GlanceTheme.colors.primaryContainer to GlanceTheme.colors.onPrimaryContainer
        PillTone.Lost, PillTone.Next -> GlanceTheme.colors.surfaceVariant to GlanceTheme.colors.onSurfaceVariant
      }
      Box(
        GlanceModifier.defaultWeight().cornerRadius(8.dp).background(container)
          .padding(horizontal = FavoriteWidgetPillPadding, vertical = FavoriteWidgetPillPadding / 2),
        contentAlignment = Alignment.Center,
      ) {
        FavoriteWidgetText(pill.text, FavoriteWidgetFont.Label, content, FavoriteWidgetWeight.Medium, maxLines = 1, align = TextAlign.Center)
      }
    }
  }
}

internal val PillTone.color: ColorProvider @Composable get() = when (this) {
  PillTone.Live -> GlanceTheme.colors.error
  PillTone.Won -> GlanceTheme.colors.primary
  PillTone.Lost, PillTone.Next -> GlanceTheme.colors.onSurfaceVariant
}

internal fun FavoriteWidgetTypography.cardChrome(heading: String, width: Dp, rows: Int, textStep: Int): Dp =
  FavoriteWidgetCardTop + height(heading, FavoriteWidgetFont.Label - textStep, width, weight = FavoriteWidgetWeight.Medium) +
    FavoriteWidgetCardBottom + FavoriteWidgetDivider * (rows - 1)

internal val FavoriteWidgetCardColor: ColorProvider @Composable get() = GlanceTheme.colors.secondaryContainer
internal val FavoriteWidgetCardOutline: ColorProvider @Composable get() = GlanceTheme.colors.widgetBackground
internal val FavoriteWidgetCardTop = 10.dp
internal val FavoriteWidgetCardBottom = 4.dp

@Composable
internal fun FavoriteWidgetCard(
  heading: String,
  modifier: GlanceModifier = GlanceModifier,
  rows: List<@Composable () -> Unit>,
) {
  Column(
    modifier.fillMaxWidth().cornerRadius(16.dp).background(FavoriteWidgetCardColor)
      .padding(start = FavoriteWidgetCardPadding, end = FavoriteWidgetCardPadding, bottom = FavoriteWidgetCardBottom),
  ) {
    FavoriteWidgetText(
      heading,
      FavoriteWidgetFont.Label,
      GlanceTheme.colors.primary,
      FavoriteWidgetWeight.Medium,
      modifier = GlanceModifier.padding(top = FavoriteWidgetCardTop),
    )
    rows.forEachIndexed { index, row ->
      if (index > 0) {
        Box(GlanceModifier.fillMaxWidth().height(FavoriteWidgetDivider).background(FavoriteWidgetCardOutline)) {}
      }
      Box(GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.CenterStart) { row() }
    }
  }
}

@Composable
internal fun FavoriteWidgetChip(text: String, win: Boolean, size: DpSize) {
  Box(
    GlanceModifier.width(size.width).height(size.height).cornerRadius(size.height / 2)
      .background(if (win) GlanceTheme.colors.primary else FavoriteWidgetCardOutline),
    contentAlignment = Alignment.Center,
  ) {
    FavoriteWidgetText(
      text,
      FavoriteWidgetFont.Label,
      if (win) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSurfaceVariant,
      FavoriteWidgetWeight.Bold,
      maxLines = 1,
      align = TextAlign.Center,
    )
  }
}

internal fun FavoriteWidgetTypography.chipSize(text: String, textStep: Int): DpSize {
  val width = maxOf(22.dp, width(text, FavoriteWidgetFont.Label - textStep, FavoriteWidgetWeight.Bold) + 10.dp)
  return DpSize(
    width,
    maxOf(22.dp, height(text, FavoriteWidgetFont.Label - textStep, width, maxLines = 1, weight = FavoriteWidgetWeight.Bold)),
  )
}
