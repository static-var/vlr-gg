/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.match

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.LocaleList
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.R
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.test.platform.app.InstrumentationRegistry
import dev.staticvar.vlr.android.widget.favorite.FavoriteWidgetSurface
import dev.staticvar.vlr.android.widget.favorite.WidgetBitmaps
import dev.staticvar.vlr.android.widget.favorite.favoriteWidgetDescendants
import dev.staticvar.vlr.shared.widget.UpcomingMatchesSnapshot
import dev.staticvar.vlr.shared.widget.UpcomingWidgetMatch
import dev.staticvar.vlr.shared.widget.WidgetTheme
import dev.staticvar.vlr.shared.widget.favoriteMatchState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Instant

class FavoriteMatchWidgetRenderTest {
  @Test
  fun everyStateRendersWithoutClippingOverlapOrTruncation() {
    assertEquals(80, renders.count { it.texts.isNotEmpty() })
    val problems = renders.flatMap { render -> render.problems.map { "${render.name}: $it" } }
    assertTrue(problems.joinToString("\n"), problems.isEmpty())
  }

  @Test
  fun tallLayoutShowsPinnedCopy() {
    val expected = mapOf(
      "live" to listOf("LIVE", "Masters London", "Sentinels", "Paper Rex", "1", "–", "0", "Map 2 of 3 · Bo3"),
      "live-hidden" to listOf("LIVE", "Masters London", "Sentinels", "Paper Rex", "–", "Bo3"),
      "upcoming" to listOf("Tue 19:00", "Masters London", "Sentinels", "LOUD", "VS", "Playoffs · Bo3"),
      "completed" to listOf("FINAL", "Masters London", "Paper Rex", "Team Liquid", "2", "–", "1", "Grand final"),
      "completed-next" to listOf("FINAL", "Paper Rex", "Team Liquid", "Next: LOUD vs Sentinels · Tue 19:00"),
      "no-matches" to listOf("No matches scheduled", "Your favorites return here when they play"),
      "no-favorites" to listOf("Pick favorites", "Follow teams or events to see their matches here"),
      "needs-app" to listOf("To use this widget,", "Open Val Esports"),
    )
    expected.forEach { (state, strings) ->
      listOf(false, true).forEach { dark ->
        val render = renders.single { it.name == "$state-${Tall.label}-${if (dark) "dark" else "light"}" }
        assertEquals(render.name, strings, strings.filter { it in render.texts.map(RenderedText::value) })
      }
    }
    val hidden = renders.first { it.name.startsWith("live-hidden-${Tall.label}") }
    assertTrue(hidden.descriptions.toString(), "Scores hidden" in hidden.descriptions)
  }

  @Test
  fun completedLoserIsMutedAndWinnerKeepsPrimaryScore() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val render = renders.single { it.name == "completed-${Tall.label}-light" }
    fun color(text: String) = render.texts.first { it.value == text }.color
    assertEquals(context.getColor(R.color.glance_colorOnSurface), color("Paper Rex"))
    assertEquals(context.getColor(R.color.glance_colorOnSurfaceVariant), color("Team Liquid"))
    assertEquals(context.getColor(R.color.glance_colorPrimary), color("2"))
    assertEquals(context.getColor(R.color.glance_colorOnSurfaceVariant), color("1"))
  }

  private data class RenderSize(val label: String, val size: DpSize)
  private data class RenderedText(val value: String, val color: Int)
  private data class Render(val name: String, val problems: List<String>, val texts: List<RenderedText>, val descriptions: List<String>)

  private companion object {
    val Compact = RenderSize("110x110", DpSize(110.dp, 110.dp))
    val Wide = RenderSize("250x110", DpSize(250.dp, 110.dp))
    val Tall = RenderSize("250x180", DpSize(250.dp, 180.dp))

    // Exact sizing also lays out common launcher 4x2 and lock screen hub 4x3 slots.
    val Launcher = RenderSize("330x130", DpSize(330.dp, 130.dp))
    val Hub = RenderSize("330x220", DpSize(330.dp, 220.dp))
    val now = Instant.parse("2026-10-25T12:00:00Z").toEpochMilliseconds()
    val tuesdayEvening = Instant.parse("2026-10-27T19:00:00Z").toEpochMilliseconds()
    val renders: List<Render> by lazy { runBlocking { renderAll() } }

    fun fixtures(): Map<String, UpcomingMatchesSnapshot?> {
      val live = UpcomingWidgetMatch(
        id = "live",
        event = "Masters London",
        team1 = "Sentinels",
        team2 = "Paper Rex",
        startTimeEpochMillis = now - 3_600_000,
        status = "LIVE",
        score1 = 1,
        score2 = 0,
        format = "BO3",
        stage = "Upper final",
        team1Logo = "https://example.test/sentinels.png",
      )
      val upcoming = UpcomingWidgetMatch(
        id = "next",
        event = "Masters London",
        team1 = "Sentinels",
        team2 = "LOUD",
        startTimeEpochMillis = tuesdayEvening,
        format = "BO3",
        stage = "Playoffs",
      )
      val final = UpcomingWidgetMatch(
        id = "final",
        event = "Masters London",
        team1 = "Paper Rex",
        team2 = "Team Liquid",
        startTimeEpochMillis = now - 4 * 3_600_000,
        status = "COMPLETED",
        score1 = 2,
        score2 = 1,
        format = "BO3",
        stage = "Grand final",
        team1Logo = "https://example.test/prx.png",
      )
      val base = UpcomingMatchesSnapshot(
        savedAtEpochMillis = now,
        hasFavorites = true,
        matches = emptyList(),
        theme = WidgetTheme(0, 0, 0, 0, 0, 0, false),
      )
      return linkedMapOf(
        "live" to base.copy(matches = listOf(live)),
        "live-hidden" to base.copy(matches = listOf(live.copy(score1 = null, score2 = null)), spoilersHidden = true),
        "upcoming" to base.copy(matches = listOf(upcoming)),
        "completed" to base.copy(results = listOf(final)),
        "completed-next" to base.copy(matches = listOf(upcoming.copy(team1 = "LOUD", team2 = "Sentinels")), results = listOf(final)),
        "no-matches" to base,
        "no-favorites" to base.copy(hasFavorites = false),
        "needs-app" to null,
      )
    }

    suspend fun renderAll(): List<Render> = buildList {
      for (dark in listOf(false, true)) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val configuration = Configuration(instrumentation.targetContext.resources.configuration).apply {
          setLocales(LocaleList(Locale.US))
          fontScale = 1f
          uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        val context = instrumentation.targetContext.createConfigurationContext(configuration)
        val text = FavoriteMatchText(context, use24Hour = true, timeZone = TimeZone.getTimeZone("UTC"))
        val logo = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply {
          eraseColor(context.getColor(R.color.glance_colorPrimary))
        }
        val logos = WidgetBitmaps(mapOf("https://example.test/sentinels.png" to logo, "https://example.test/prx.png" to logo))
        for ((state, snapshot) in fixtures()) {
          val copy = text.copy(favoriteMatchState(snapshot, now), snapshot?.spoilersHidden == true, now)
          for (size in listOf(Compact, Wide, Tall, Launcher, Hub)) {
            add(render("$state-${size.label}-${if (dark) "dark" else "light"}", copy, logos, size.size, context, configuration))
          }
        }
      }
    }

    suspend fun render(
      name: String,
      copy: FavoriteMatchCopy,
      logos: WidgetBitmaps,
      size: DpSize,
      context: Context,
      configuration: Configuration,
    ): Render {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val remote = object : GlanceAppWidget() {
        override suspend fun provideGlance(context: Context, id: GlanceId) {
          provideContent { FavoriteWidgetSurface(GlanceModifier) { FavoriteMatchLayout(copy, logos) } }
        }
      }.compose(context, size = size)
      var result: Render? = null
      instrumentation.runOnMainSync {
        val host = instrumentation.context.createConfigurationContext(configuration)
        val view = remote.apply(host, FrameLayout(host))
        val density = context.resources.displayMetrics.density
        val width = (size.width.value * density).roundToInt()
        val height = (size.height.value * density).roundToInt()
        view.measure(
          View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, width, height)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(output))
        val directory = File(context.filesDir, "favorite-match-renders").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { output.compress(Bitmap.CompressFormat.PNG, 100, it) }
        output.recycle()
        val views = favoriteWidgetDescendants(view).filter { it.visibleUnder(view) }
        val texts = views.filterIsInstance<TextView>().filter { it.text.isNotEmpty() }
        result = Render(
          name,
          inspect(view, views, texts),
          texts.map { RenderedText(it.text.toString(), it.currentTextColor) },
          views.mapNotNull { it.contentDescription?.toString() },
        )
      }
      return requireNotNull(result)
    }

    fun View.visibleUnder(root: View): Boolean {
      var current: View? = this
      while (current != null) {
        if (current.visibility != View.VISIBLE) return false
        if (current === root) return true
        current = current.parent as? View
      }
      return false
    }

    // Glance draws backgrounds and rounded tiles as an ImageView that fills its container.
    fun View.fillsParent(): Boolean = (parent as? View)?.let { width == it.width && height == it.height } == true

    fun inspect(root: View, views: List<View>, texts: List<TextView>): List<String> = buildList {
      fun View.boundsIn(rect: Rect): Rect {
        var child: View = this
        while (child !== root) {
          rect.offset(child.left, child.top)
          child = child.parent as View
        }
        return rect
      }
      for (child in views) {
        val parent = child.parent as? View ?: continue
        if (child === root) continue
        if (child.left < 0 || child.top < 0 || child.right > parent.width || child.bottom > parent.height) {
          add("${child.javaClass.simpleName} ${(child as? TextView)?.text ?: ""} clipped by its parent")
        }
      }
      val drawn = texts.map { text ->
        val layout = text.layout
        if (layout == null || layout.lineCount == 0) {
          add("${text.text}: no layout")
          return@map text.text.toString() to Rect()
        }
        for (line in 0 until layout.lineCount) {
          if (layout.getEllipsisCount(line) > 0) add("${text.text}: truncated by TextView")
          if (layout.getLineWidth(line) > text.width - text.compoundPaddingLeft - text.compoundPaddingRight + 1) {
            add("${text.text}: line wider than its view")
          }
        }
        if (layout.height > text.height - text.compoundPaddingTop - text.compoundPaddingBottom) add("${text.text}: taller than its view")
        val bounds = Rect(
          (0 until layout.lineCount).minOf { layout.getLineLeft(it) }.toInt(),
          0,
          ceil((0 until layout.lineCount).maxOf { layout.getLineRight(it) }).toInt(),
          layout.height,
        )
        bounds.offset(text.totalPaddingLeft, text.totalPaddingTop)
        text.text.toString() to text.boundsIn(bounds)
      } + views.filterIsInstance<ImageView>().filterNot { it.fillsParent() }
        .map { "image" to it.boundsIn(Rect(0, 0, it.width, it.height)) }
      drawn.forEachIndexed { index, (label, bounds) ->
        drawn.drop(index + 1).filter { (_, other) -> Rect.intersects(bounds, other) }
          .forEach { (other, _) -> add("$label overlaps $other") }
      }
    }
  }
}
