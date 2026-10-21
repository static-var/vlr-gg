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
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
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
import androidx.glance.text.TextAlign
import dev.staticvar.vlr.android.R
import dev.staticvar.vlr.android.widget.PrismTextTone
import dev.staticvar.vlr.android.widget.PrismWidgetText
import dev.staticvar.vlr.shared.widget.favorite.LogoMark
import dev.staticvar.vlr.shared.widget.favorite.PillText
import dev.staticvar.vlr.shared.widget.favorite.PillTone
import kotlin.math.ceil
import kotlin.math.roundToInt

internal enum class FavoriteWidgetPart { Form, Context, Pills, RegionalRank }

internal data class FavoriteWidgetPresentation(
  val large: Boolean,
  val heroSize: Int,
  val parts: Set<FavoriteWidgetPart>,
  val rows: Int,
  val rowHeight: Dp,
  val compact: Boolean,
  val textStep: Int = 0,
) {
  val verticalPadding: Dp get() = if (compact) 6.dp else 12.dp

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
  val heroSize = if (compact) 32 else 40
  val requiredLarge = FavoriteWidgetPresentation(true, heroSize, emptySet(), 0, rowHeight(0), compact)
  val large = height(requiredLarge.copy(compact = true)) <= size.height
  val parts = if (large) largeParts else mediumParts
  val candidates = buildList {
    var candidate = FavoriteWidgetPresentation(
      large,
      heroSize,
      parts,
      if (large) rowCount else 0,
      rowHeight(if (large) rowCount else 0),
      compact = false,
    )
    add(candidate)
    candidate = candidate.copy(compact = true)
    add(candidate)
    while (candidate.rows > 0) {
      candidate = candidate.copy(rows = candidate.rows - 1, rowHeight = rowHeight(candidate.rows - 1))
      add(candidate)
    }
    for (part in listOf(
      FavoriteWidgetPart.RegionalRank,
      FavoriteWidgetPart.Pills,
      FavoriteWidgetPart.Context,
      FavoriteWidgetPart.Form,
    )) {
      if (part !in candidate) continue
      candidate = candidate.copy(parts = candidate.parts - part)
      add(candidate)
    }
    while (candidate.heroSize > 15 + 1) {
      candidate = candidate.copy(heroSize = candidate.heroSize - 1)
      add(candidate)
    }
    while (candidate.textStep < 7) {
      candidate = candidate.copy(textStep = candidate.textStep + 1, heroSize = candidate.heroSize - 1)
      add(candidate)
    }
  }
  return candidates.firstOrNull { height(it) <= size.height } ?: candidates.last()
}

internal val LocalFavoriteWidgetTextStep = staticCompositionLocalOf { 0 }
internal val FavoriteWidgetCaptionGap = 4.dp

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
    fun text(text: String, sp: Int, width: Dp, name: Boolean = false) =
      height(text, sp - textStep, width, properName = name)
    val gap = if (hero.caption == null) 0.dp else FavoriteWidgetCaptionGap
    val lead = maxOf(
      text(hero.lead, 13, hero.width - hero.captionWidth - gap, hero.properName),
      hero.caption?.let { text(it, 12, hero.captionWidth) } ?: 0.dp,
    )
    return maxOf(
      roundedToPixel(hero.minimumHeight),
      lead + height(hero.value, hero.valueSize, hero.width, maxLines = 1),
    )
  }

  private fun textPixels(size: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size.toFloat(), metrics)

  private fun paint(pixels: Float) = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
    typeface = Condensed
    textSize = pixels
  }

  fun width(text: String, size: Int): Dp {
    val key = WidthKey(text, textPixels(size))
    val pixels = widths[key] ?: (paint(key.textPixels).measureText(text) + 2).also { widths.put(key, it) }
    return (ceil(pixels) / metrics.density).dp
  }

  fun column(text: String?, size: Int, available: Dp, share: Float): Dp =
    if (text == null) 0.dp else minOf(width(text, size), available * share)

  fun height(text: String, size: Int, width: Dp, maxLines: Int = 2, properName: Boolean = false): Dp {
    if (!properName && maxLines == 1 && width(text, size) > width) return Dp.Infinity
    val key = HeightKey(
      text,
      textPixels(size),
      (width.value * metrics.density).roundToInt().coerceAtLeast(1),
      if (properName) 1 else maxLines,
      properName,
    )
    val pixels = heights[key] ?: measure(key).also { heights.put(key, it) }
    return if (pixels == Int.MAX_VALUE) Dp.Infinity else (pixels / metrics.density).dp
  }

  @SuppressLint("InlinedApi") // StaticLayout supports this inlined strategy value from API 23.
  private fun measure(key: HeightKey): Int {
    val paint = paint(key.textPixels)
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

  private data class WidthKey(val text: String, val textPixels: Float)
  private data class HeightKey(
    val text: String,
    val textPixels: Float,
    val widthPixels: Int,
    val maxLines: Int,
    val properName: Boolean,
  )

  private companion object {
    val Condensed: Typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
    val widths = LruCache<WidthKey, Float>(1024)
    val heights = LruCache<HeightKey, Int>(4096)
  }
}

@Composable
internal fun FavoriteWidgetText(
  text: String,
  size: Int,
  tone: PrismTextTone = PrismTextTone.Content,
  maxLines: Int = 2,
  modifier: GlanceModifier = GlanceModifier,
  align: TextAlign = TextAlign.Start,
  description: String? = null,
  properName: Boolean = false,
  textStep: Int = LocalFavoriteWidgetTextStep.current,
) {
  PrismWidgetText(text, size - textStep, tone, if (properName) 1 else maxLines, align, description, modifier)
}

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
        .background(GlanceTheme.colors.secondaryContainer)
        .semantics { contentDescription = description },
      contentAlignment = Alignment.Center,
    ) {
      FavoriteWidgetText(
        monogram,
        (size.value * 0.34f).toInt(),
        tone = PrismTextTone.Secondary,
        align = TextAlign.Center,
        modifier = GlanceModifier.width(size - 4.dp),
        properName = true,
      )
    }
  }
}

@Composable
internal fun FavoriteWidgetDot(label: String, filled: Boolean, live: Boolean, empty: Boolean = false, size: Dp = 8.dp) {
  Image(
    ImageProvider(if (filled) R.drawable.favorite_widget_dot else R.drawable.favorite_widget_ring),
    label,
    GlanceModifier.width(size + 4.dp).height(size).padding(start = 4.dp),
    colorFilter = ColorFilter.tint(
      when {
        live -> GlanceTheme.colors.primary
        empty -> GlanceTheme.colors.secondaryContainer
        else -> GlanceTheme.colors.onSurfaceVariant
      },
    ),
  )
}

@Composable
internal fun FavoriteWidgetEmptyHeader(title: String, subtitle: String?, large: Boolean) {
  val logoSize = if (large) 40.dp else 28.dp
  Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
    Box(GlanceModifier.size(logoSize).cornerRadius(7.dp).background(GlanceTheme.colors.secondaryContainer)) {}
    Spacer(GlanceModifier.width(8.dp))
    Column(GlanceModifier.defaultWeight()) {
      FavoriteWidgetText(title, 15)
      subtitle?.let { FavoriteWidgetText(it, 12, PrismTextTone.Secondary) }
    }
  }
}

@Composable
internal fun FavoriteWidgetMapPills(items: List<PillText>) {
  Row(GlanceModifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Vertical.CenterVertically) {
    items.forEachIndexed { index, pill ->
      if (index > 0) Spacer(GlanceModifier.width(3.dp))
      Box(
        GlanceModifier.defaultWeight().cornerRadius(5.dp).background(GlanceTheme.colors.secondaryContainer)
          .padding(horizontal = 3.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
      ) {
        FavoriteWidgetText(pill.text, 11, pill.tone.textTone(), maxLines = 1)
      }
    }
  }
}

internal fun PillTone.textTone(): PrismTextTone = when (this) {
  PillTone.Live -> PrismTextTone.Accent
  PillTone.Won -> PrismTextTone.Content
  PillTone.Lost, PillTone.Next -> PrismTextTone.Secondary
}
