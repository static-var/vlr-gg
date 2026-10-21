/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetFrame
import dev.staticvar.vlr.shared.widget.favorite.LogoMark
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

internal data class TextMeasurement(val value: String, val color: Int, val fontSize: Float, val problems: List<String>)
internal data class FavoriteWidgetViewInspection(val texts: List<TextMeasurement>, val clippedViews: List<String>)
internal enum class WidgetTextRole(val sp: Int?, val lines: Int = 1, val properName: Boolean = false) {
  Hero(null),
  HeroName(null, properName = true),
  Title(15, 2),
  NameTitle(15, properName = true),
  Lead(13, 2),
  NameLead(13, properName = true),
  Secondary(12, 2),
  Ranking(12),
  Stage(12, 2),
  Round(11, 2),
  Pill(11),
  Score(17),
  RowName(15, properName = true),
  GroupName(13, properName = true),
  MatchName(12, properName = true),
  Clock(13),
  Rank(13),
  Record(13),
  Amount(13),
  Position(12),
  MatchScore(12),
  Mark(12),
  Logo16(5, properName = true),
  Logo20(6, properName = true),
  Logo22(7, properName = true),
  Logo28(9, properName = true),
  Logo30(10, properName = true),
  Logo40(13, properName = true),
  Logo44(14, properName = true),
}

internal class WidgetTextExpectations {
  val roles = mutableMapOf<String, MutableSet<WidgetTextRole>>()
  fun add(value: String?, role: WidgetTextRole) {
    if (value != null) roles.getOrPut(value) { mutableSetOf() }.add(role)
  }
  fun logo(mark: LogoMark?, size: Int) {
    if (mark == null) return
    val role = when (size) {
      16 -> WidgetTextRole.Logo16
      20 -> WidgetTextRole.Logo20
      22 -> WidgetTextRole.Logo22
      28 -> WidgetTextRole.Logo28
      30 -> WidgetTextRole.Logo30
      40 -> WidgetTextRole.Logo40
      44 -> WidgetTextRole.Logo44
      else -> error("Unknown logo size $size")
    }
    add(
      when (mark) {
        is LogoMark.Named -> mark.monogram
        LogoMark.Undecided -> LogoMark.Undecided.monogram
      },
      role,
    )
  }
  fun empty(frame: FavoriteWidgetFrame) {
    when (frame) {
      is FavoriteWidgetFrame.Setup -> {
        add(frame.text.title, WidgetTextRole.Title)
        add(frame.text.subtitle, WidgetTextRole.Secondary)
        add(frame.text.lead, WidgetTextRole.Lead)
        add(frame.text.action, WidgetTextRole.Hero)
        add(frame.text.instruction, WidgetTextRole.Secondary)
        add(frame.text.ghost, WidgetTextRole.Secondary)
      }

      is FavoriteWidgetFrame.Placeholder -> {
        add(frame.text.title, WidgetTextRole.Title)
        add(frame.text.value, WidgetTextRole.Hero)
        add(frame.text.ghost, WidgetTextRole.Secondary)
      }

      else -> Unit
    }
  }
}

internal fun inspectFavoriteWidgetView(
  view: View,
  expected: WidgetTextExpectations,
  textStep: Int,
  heroSize: Int,
): FavoriteWidgetViewInspection {
  val allViews = favoriteWidgetDescendants(view)
  val textBounds = allViews.filterIsInstance<TextView>().filter { it.text.isNotEmpty() && it.layout != null }.map { text ->
    val layout = text.layout
    val bounds = Rect(
      (0 until layout.lineCount).minOf { layout.getLineLeft(it) }.toInt(),
      0,
      ceil((0 until layout.lineCount).maxOf { layout.getLineRight(it) }).toInt(),
      layout.height,
    )
    bounds.offset(text.totalPaddingLeft, text.totalPaddingTop)
    var child: View = text
    while (child !== view) {
      bounds.offset(child.left, child.top)
      child = child.parent as View
    }
    text.text.toString() to bounds
  }
  val overlaps = textBounds.flatMapIndexed { index, (text, bounds) ->
    val gap = view.resources.displayMetrics.density.roundToInt()
    val padded = Rect(bounds.left - gap, bounds.top, bounds.right + gap, bounds.bottom)
    textBounds.drop(index + 1).filter { (_, other) -> Rect.intersects(padded, other) }
      .map { (other, _) -> "$text overlaps or touches $other" }
  }

  val hierarchy = mutableMapOf<WidgetTextRole, MutableList<Float>>()
  val inspection = FavoriteWidgetViewInspection(
    texts = allViews.filterIsInstance<TextView>().filter {
      it.text.isNotEmpty()
    }.map { text ->
      val problems = mutableListOf<String>()
      var parent = text.parent as? View
      var logo = false
      while (parent != null && parent !== view) {
        if (!parent.contentDescription.isNullOrEmpty() && parent.width == parent.height) logo = true
        parent = parent.parent as? View
      }
      val roles = expected.roles[text.text.toString()].orEmpty().filter { it.name.startsWith("Logo") == logo }
      val matches = roles.filter { role ->
        val sp = role.sp?.minus(textStep) ?: heroSize
        val pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), text.resources.displayMetrics)
        abs(text.textSize - pixels) < .01f
      }
      if (matches.isEmpty()) {
        problems +=
          "role size ${text.textSize}px does not match $roles at step $textStep, hero $heroSize"
      }
      matches.forEach { hierarchy.getOrPut(it) { mutableListOf() }.add(text.textSize) }
      val layout = text.layout
      if (layout == null || layout.lineCount == 0) {
        problems += "no text layout"
      } else {
        if (matches.none { layout.lineCount <= it.lines }) {
          problems +=
            "unexpected ${layout.lineCount}-line wrap for $roles"
        }
        for (line in 0 until layout.lineCount) {
          if (layout.getEllipsisCount(line) != 0 && matches.none { it.properName }) problems += "ellipsis"
          if (layout.getLineWidth(line) >
            text.width - text.compoundPaddingLeft - text.compoundPaddingRight + 1
          ) {
            problems += "line width ${layout.getLineWidth(line)}/${text.width}"
          }
        }
        if (layout.getLineEnd(layout.lineCount - 1) < text.text.length) problems += "missing characters"
        if (layout.height >
          text.height - text.compoundPaddingTop - text.compoundPaddingBottom
        ) {
          problems += "text height ${layout.height}/${text.height}"
        }
      }
      val bounds = Rect(0, 0, text.width, text.height)
      var child: View = text
      while (child !== view) {
        bounds.offset(child.left, child.top)
        val parent = child.parent as View
        if (bounds.left < 0 || bounds.top < 0 || bounds.right > parent.width ||
          bounds.bottom > parent.height
        ) {
          problems += "ancestor clips text"
        }
        child = parent
      }
      TextMeasurement(text.text.toString(), text.currentTextColor, text.textSize, problems)
    },
    clippedViews = allViews.filter { child ->
      val parent = child.parent as? View
      child !== view && parent != null &&
        (child.left < 0 || child.top < 0 || child.right > parent.width || child.bottom > parent.height)
    }.map { "${it.javaClass.simpleName} clips at ${it.left},${it.top},${it.right},${it.bottom}" } + overlaps,
  )
  val hero = hierarchy[WidgetTextRole.Hero].orEmpty() + hierarchy[WidgetTextRole.HeroName].orEmpty()
  val title = hierarchy[WidgetTextRole.Title].orEmpty() + hierarchy[WidgetTextRole.NameTitle].orEmpty()
  val lead = hierarchy[WidgetTextRole.Lead].orEmpty() + hierarchy[WidgetTextRole.NameLead].orEmpty()
  val secondary =
    hierarchy[WidgetTextRole.Secondary].orEmpty() + hierarchy[WidgetTextRole.Stage].orEmpty() +
      hierarchy[WidgetTextRole.Ranking].orEmpty()
  val hierarchyProblems = buildList {
    if (hero.isEmpty() || title.isEmpty()) add("missing hero/title role")
    if (hero.any { h -> title.any { it >= h } }) add("hero must be larger than title")
    if (title.any { t -> lead.any { it > t } }) add("title must be at least lead size")
    if (lead.any { l -> secondary.any { it > l } }) add("lead must be at least secondary size")
  }
  return inspection.copy(clippedViews = inspection.clippedViews + hierarchyProblems)
}

internal fun favoriteWidgetDescendants(view: View): List<View> = listOf(view) + if (view is ViewGroup) {
  (0 until view.childCount).flatMap { favoriteWidgetDescendants(view.getChildAt(it)) }
} else {
  emptyList()
}
