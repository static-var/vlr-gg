/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AtomicFile
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.test.platform.app.InstrumentationRegistry
import dev.staticvar.vlr.android.R
import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import dev.staticvar.vlr.widget.ScoreWidget
import java.io.File
import java.io.FileNotFoundException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.context.GlobalContext

class WidgetPayloadRenderTest {
  @Test
  fun productionWidgetsRenderPayloadVariants() = runBlocking {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val base = instrumentation.targetContext
    val files = listOf("upcoming_matches_widget.json", "all_matches_widget.json")
      .map { AtomicFile(base.getFileStreamPath(it)) }
    val originalFiles = files.map { file ->
      try {
        file.readFully()
      } catch (_: FileNotFoundException) {
        null
      }
    }
    val preferences = GlobalContext.get().get<SpoilerPreferencesRepository>()
    val spoilersWereHidden = preferences.enabled.value
    try {
      if (preferences.enabled.value) preferences.toggle()
      for (dark in listOf(false, true)) {
        val configuration = Configuration(base.resources.configuration).apply {
          uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        val context = base.createConfigurationContext(configuration)
        val hostContext = instrumentation.context.createConfigurationContext(configuration)
        for (fixture in fixtures()) {
          files.forEach { file -> file.replace(fixture.json?.toByteArray(Charsets.UTF_8)) }
          for (kind in WidgetKind.entries) {
            val remote = kind.widget().compose(context, size = kind.size)
            lateinit var view: View
            instrumentation.runOnMainSync {
              view = remote.apply(hostContext, AppWidgetHostView(hostContext))
              view.measureAndLayout(context, kind.size)
            }
            val emptyState = fixture.emptyState(context, kind)
            val marker = emptyState ?: fixture.team1
            val deadline = SystemClock.uptimeMillis() + 3_000
            var texts = emptyList<TextView>()
            do {
              instrumentation.runOnMainSync {
                view.measureAndLayout(context, kind.size)
                texts = textViews(view)
              }
              if (texts.any { it.text.toString() == marker }) break
              instrumentation.waitForIdleSync()
              SystemClock.sleep(25)
            } while (SystemClock.uptimeMillis() < deadline)
            instrumentation.runOnMainSync {
              val label = "${fixture.name}/${kind.name}/${if (dark) "dark" else "light"}"
              assertTrue("$label did not render $marker", texts.any { it.text.toString() == marker })
              if (emptyState != null) {
                assertFalse("$label rendered a filtered match", texts.any { it.text.toString() == fixture.team1 })
              } else {
                assertMatch(context, fixture, texts, label)
              }
              val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
              view.draw(Canvas(bitmap))
              val output = File(context.getExternalFilesDir(null), "widget-payload-renders").apply { mkdirs() }
              File(output, "${fixture.name}-${kind.name.lowercase()}-${if (dark) "dark" else "light"}.png")
                .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
              bitmap.recycle()
            }
          }
        }
      }
    } finally {
      try {
        files.zip(originalFiles).forEach { (file, bytes) -> file.replace(bytes) }
      } finally {
        if (preferences.enabled.value != spoilersWereHidden) preferences.toggle()
      }
    }
  }

  private fun assertMatch(context: Context, fixture: Fixture, texts: List<TextView>, label: String) {
    val names = texts.filter { it.text.toString() in listOf(fixture.team1, fixture.team2) }
    assertEquals("$label team names", 2, names.size)
    names.forEach { name ->
      assertEquals("$label team wrapping", 2, name.maxLines)
      assertTrue("$label team must have visible width", name.width > 0)
    }
    val status = context.getString(if (fixture.live) R.string.widget_live else R.string.widget_upcoming)
    assertTrue("$label status", texts.any { it.text.toString() == status })
    val scores = texts.filter { it.text.toString() in listOf("1", "2", "—") }
    assertEquals("$label scores", fixture.scores, scores.map { it.text.toString() })
    if (fixture.hidden) {
      scores.forEach {
        assertEquals("$label hidden score description", context.getString(R.string.widget_scores_hidden), it.contentDescription)
      }
    }
    if (fixture.timeTbd) {
      assertTrue("$label missing time", texts.any { it.text.toString() == context.getString(R.string.widget_time_tbd) })
    }
  }

  private fun fixtures(): List<Fixture> {
    val live = """{"id":"live","event":"VCT Pacific","team1":"Team Liquid","team2":"Paper Rex","status":"LIVE","startTimeEpochMillis":null,"score1":1,"score2":2,"format":"BO3","stage":"Playoffs"}"""
    val upcoming = """{"id":"upcoming","event":"VCT Pacific","team1":"Team Liquid","team2":"Paper Rex","status":"UPCOMING","startTimeEpochMillis":4102444800000,"score1":null,"score2":null}"""
    val missingOptional = """{"id":"missing","event":"VCT Pacific","team1":"Team Liquid","team2":"Paper Rex"}"""
    val nullable = """{"id":"nullable","event":"VCT Pacific","team1":"Team Liquid","team2":"Paper Rex","status":"LIVE","startTimeEpochMillis":null,"score1":null,"score2":null}"""
    val unknown = live.dropLast(1) + """, "futureField":{"unexpected":true}}"""
    val longNames = live.replace("Team Liquid", "Shopify Rebellion Gold").replace("Paper Rex", "Twisted Minds Esports")
    return listOf(
      Fixture("live", snapshot(live), live = true, scores = listOf("1", "2")),
      Fixture("upcoming", snapshot(upcoming)),
      Fixture("missing-optional", snapshot(missingOptional), timeTbd = true),
      Fixture("null-scores", snapshot(nullable), live = true),
      Fixture("unknown-fields", snapshot(unknown, extra = """, "futureRoot": [1, 2, 3]"""), live = true, scores = listOf("1", "2")),
      Fixture("hidden", snapshot(live, hidden = true), live = true, hidden = true),
      Fixture("long-names", snapshot(longNames), live = true, scores = listOf("1", "2"), team1 = "Shopify Rebellion Gold", team2 = "Twisted Minds Esports"),
      Fixture("unknown-status", snapshot(live.replace("LIVE", "PAUSED")), noMatches = true),
      Fixture("empty", snapshot(""), noMatches = true),
      Fixture("no-favorites", snapshot(live, favorites = false), live = true, scores = listOf("1", "2"), noFavorites = true),
      Fixture("invalid", """{"matches": [""", invalid = true),
      Fixture("missing-snapshot", null, invalid = true),
    )
  }

  private fun snapshot(matches: String, favorites: Boolean = true, hidden: Boolean = false, extra: String = ""): String =
    """{"savedAtEpochMillis":1900000000000,"hasFavorites":$favorites,"spoilersHidden":$hidden,"matches":[$matches]$extra}"""

  private data class Fixture(
    val name: String,
    val json: String?,
    val live: Boolean = false,
    val hidden: Boolean = false,
    val scores: List<String> = listOf("—", "—"),
    val timeTbd: Boolean = false,
    val team1: String = "Team Liquid",
    val team2: String = "Paper Rex",
    val noMatches: Boolean = false,
    val noFavorites: Boolean = false,
    val invalid: Boolean = false,
  ) {
    fun emptyState(context: Context, kind: WidgetKind): String? = when {
      invalid -> context.getString(if (kind == WidgetKind.LEGACY) R.string.legacy_matches_widget_loading else R.string.widget_not_initialized_title)
      noFavorites && kind != WidgetKind.LEGACY -> context.getString(R.string.widget_no_favorites_title)
      noMatches -> context.getString(R.string.widget_no_upcoming_title)
      else -> null
    }
  }

  private enum class WidgetKind(val size: DpSize) {
    SMALL(DpSize(158.dp, 158.dp)),
    MEDIUM(DpSize(338.dp, 158.dp)),
    LEGACY(DpSize(338.dp, 220.dp));

    fun widget(): GlanceAppWidget = when (this) {
      SMALL -> SmallUpcomingFavoritesWidget()
      MEDIUM -> UpcomingFavoritesWidget()
      LEGACY -> ScoreWidget()
    }
  }

  private fun AtomicFile.replace(bytes: ByteArray?) {
    if (bytes == null) {
      delete()
      return
    }
    val output = startWrite()
    try {
      output.write(bytes)
      finishWrite(output)
    } catch (error: Exception) {
      failWrite(output)
      throw error
    }
  }

  private fun View.measureAndLayout(context: Context, size: DpSize) {
    val density = context.resources.displayMetrics.density
    val width = (size.width.value * density).toInt()
    val height = (size.height.value * density).toInt()
    measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
    layout(0, 0, width, height)
  }

  private fun textViews(view: View): List<TextView> = when (view) {
    is TextView -> listOf(view)
    is ViewGroup -> (0 until view.childCount).flatMap { textViews(view.getChildAt(it)) }
    else -> emptyList()
  }
}
