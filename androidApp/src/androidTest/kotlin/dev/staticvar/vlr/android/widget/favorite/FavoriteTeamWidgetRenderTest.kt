/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.LocaleList
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.R
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.test.platform.app.InstrumentationRegistry
import dev.staticvar.vlr.shared.widget.UpcomingMatchesSnapshot
import dev.staticvar.vlr.shared.widget.WidgetFavoriteTeam
import dev.staticvar.vlr.shared.widget.WidgetTheme
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetEngine
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetFrame
import dev.staticvar.vlr.shared.widget.favorite.NeutralTeamHeroText
import dev.staticvar.vlr.shared.widget.favorite.StoredWidgetRecord
import dev.staticvar.vlr.shared.widget.favorite.TeamDetail
import dev.staticvar.vlr.shared.widget.favorite.TeamDisplayBody
import dev.staticvar.vlr.shared.widget.favorite.TeamHeroText
import dev.staticvar.vlr.shared.widget.favorite.TeamLiveBelow
import dev.staticvar.vlr.shared.widget.favorite.TeamPresentation
import dev.staticvar.vlr.shared.widget.favorite.WidgetKind
import dev.staticvar.vlr.shared.widget.favorite.WidgetRequest
import dev.staticvar.vlr.shared.widget.favorite.WidgetSelection
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.koin.core.context.GlobalContext
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Instant

@RunWith(Parameterized::class)
class FavoriteTeamWidgetRenderTest(
  width: Float,
  height: Float,
  private val dark: Boolean,
  private val fontScale: Float,
  private val densityDpi: Int,
  private val scaled: Boolean,
  private val keptProfile: Int,
) {
  private val size = DpSize(width.dp, height.dp)
  private val large = size.height >= 213.dp
  private val fullLarge = size.height >= 330.dp

  @Test
  fun textFitsWithoutClippingOrOverlap() {
    val problems = renders().flatMap { render ->
      render.texts.filter { it.problems.isNotEmpty() }.map { "${render.name}: ${it.value}: ${it.problems}" } +
        render.clippedViews.map { "${render.name}: $it" }
    }
    assertTrue(problems.joinToString("\n"), problems.isEmpty())
  }

  @Test
  fun frameStringsLogosAndThemeReachRemoteViews() {
    if (scaled) {
      assertScaledContent()
      return
    }
    renders().forEach { render ->
      val strings = render.texts.map { it.value }
      val profile = fullKeptProfile(render.name)
      val large = profile.startsWith("large")
      val expectedRows = profile.substringAfter("rows=").toInt()
      val contextKept = "context" in profile
      assertEquals("${render.name}: presentation", large, render.presentation.large)
      val baseName = render.name.removeSuffix("-hidden")
      val expected = when (baseName) {
        "setup-choose" -> listOf(
          "Choose a team",
          "Val Esports",
          "To use this widget,",
          "Tap to choose",
          "Pick a team from your favorites",
        )

        "setup-no-favorites" -> listOf(
          "No favorite teams",
          "Val Esports",
          "Open Val Esports",
          "Favorite a team, then pick it here",
        )

        "placeholder" -> listOf("Team", "Loading team…")

        "upcoming", "upcoming-bitmaps" -> listOf(
          "Sentinels",
          "Plays FNATIC",
          "Today",
          "2:14 PM",
          "Masters London · Upper final · Bo3",
        )

        "upcoming-tbd" -> listOf("Plays TBD", "TBD", "?")

        "upcoming-tomorrow" -> listOf("Plays FNATIC", "Tomorrow", "2:14 PM")

        "upcoming-delayed" -> listOf("Plays FNATIC", "Any minute", "Was due 11:59 AM · Masters London")

        "idle-empty" -> listOf("Season starts soon", "No matches", "VLR has not listed a match yet")

        "idle-win" -> if (render.hidden) {
          listOf(
            "Last played Paper Rex",
            "Oct 15",
            "Scores hidden · No match scheduled",
          )
        } else {
          listOf("Beat Paper Rex", "2–1", "Oct 15 · No match scheduled")
        }

        "idle-loss" -> if (render.hidden) {
          listOf(
            "Last played Paper Rex",
            "Oct 15",
            "Scores hidden · No match scheduled",
          )
        } else {
          listOf("Lost to Paper Rex", "2–1", "Oct 15 · No match scheduled")
        }

        else -> if (render.hidden) {
          listOf(
            "Playing FNATIC",
            "Live",
            "Live now",
            if (baseName ==
              "live-bo5"
            ) {
              "Scores hidden · Bo5"
            } else {
              "Scores hidden · Bo3"
            },
          )
        } else {
          listOf(
            "Playing FNATIC",
            "Live",
            when (baseName) {
              "live-bo5" -> "2–2"
              "live-no-score" -> "–"
              else -> "1–1"
            },
          )
        }
      }
      val contexts = setOf(
        "Val Esports", "Pick a team from your favorites", "Favorite a team, then pick it here",
        "Masters London · Upper final · Bo3", "Was due 11:59 AM · Masters London",
        "VLR has not listed a match yet", "Scores hidden · No match scheduled", "Oct 15 · No match scheduled",
        "Scores hidden · Bo3", "Scores hidden · Bo5",
      )
      expected.filterNot { !contextKept && it in contexts }.forEach {
        assertTrue("${render.name}: missing $it in $strings", it in strings)
      }
      if (!contextKept) assertTrue("${render.name}: removed context remains", strings.none { it in contexts })
      if (render.name.startsWith("live") && !render.hidden) {
        val below = if (large) {
          listOf("Masters London · Upper final · ${if (baseName == "live-bo5") "Bo5" else "Bo3"}")
        } else {
          when (baseName) {
            "live-bo3" -> listOf("Haven 13–9", "Lotus 9–13", "Ascent 6–4")
            "live-next" -> listOf("Haven 13–9", "Lotus 9–13", "Ascent next")
            "live-bo5" -> listOf("13–9", "9–13", "Split 6–4")
            else -> listOf("Masters London · Upper final · Bo3")
          }
        }
        if (!large && baseName == "live-bo5" && "pills" in profile) {
          assertEquals(2, strings.count { it == "13–9" })
          assertEquals(2, strings.count { it == "9–13" })
        }
        val belowKept = if (!large && baseName in listOf("live-bo3", "live-next", "live-bo5")) {
          "pills" in profile
        } else {
          contextKept
        }
        below.forEach { assertEquals("${render.name}: below $it", belowKept, it in strings) }
        assertTrue("Live accent lacks description", "Live" in render.descriptions)
        assertEquals(render.primary, render.texts.first { it.value == "Live" }.color)
        if (large && baseName in listOf("live-bo3", "live-next", "live-bo5")) {
          val allMaps = if (baseName == "live-bo5") {
            listOf("Haven", "Lotus", "Ascent", "Icebox", "Split")
          } else {
            listOf("Haven", "Lotus", "Ascent")
          }
          val count = expectedRows
          assertEquals("${render.name}: map prefix", allMaps.take(count), strings.filter { it in allMaps })
          assertEquals("${render.name}: map heading", count > 0, "Maps" in strings)
          if (baseName != "live-bo5" && count > 0) {
            assertTrue("${render.name}: missing SEN pick · Won in $strings", "SEN pick · Won" in strings)
            if (count == allMaps.size) {
              assertTrue("FNC pick · Lost" in strings)
              assertTrue((if (baseName == "live-next") "Decider · Next" else "Decider · Playing now") in strings)
            }
          }
        }
      }
      if (render.frame is FavoriteWidgetFrame.Team) {
        assertTrue("Sentinels" in render.descriptions)
        assertTrue(
          "${render.name}: size-specific ranking missing",
          (if ("regional" in profile) "#4 worldwide · #1 Americas" else "#4 worldwide") in strings,
        )
        if (!render.name.startsWith("idle")) {
          assertTrue(
            if (baseName ==
              "upcoming-tbd"
            ) {
              "?" in render.descriptions
            } else {
              "FNATIC" in render.descriptions
            },
          )
        }
        if (large &&
          !(baseName.startsWith("live") && !render.hidden && baseName in listOf("live-bo3", "live-bo5", "live-next")) &&
          baseName != "idle-empty"
        ) {
          assertEquals(
            "${render.name}: history heading",
            expectedRows > 0,
            (if (baseName.startsWith("idle")) "Before that" else "Recent") in strings,
          )
          val rows = if (baseName.startsWith("idle")) {
            listOf("DRX", "G2 Esports", "FNATIC", "LOUD")
          } else {
            listOf("Paper Rex", "DRX", "G2 Esports", "FNATIC")
          }
          val count = expectedRows
          assertEquals("${render.name}: history prefix", rows.take(count), strings.filter { it in rows }.distinct())
        }
        assertEquals(render.onSurface, render.texts.first { it.value == "Sentinels" }.color)
        if (baseName == "upcoming-bitmaps") {
          assertFalse("SEN" in strings)
          assertEquals(if (expectedRows == 4) 1 else 0, strings.count { it == "FNC" })
        }
      }
      if (render.frame !is FavoriteWidgetFrame.Team) {
        assertEquals("${render.name}: ghost row count", expectedRows, render.ghostRows)
        assertEquals(
          "Ghost heading",
          expectedRows > 0,
          "Recent results show here" in strings,
        )
      }
      assertEquals(render.background, render.cornerColor)
    }
  }

  @Test
  fun listHeadingsRequireVisibleRows() {
    renders().forEach { render ->
      val strings = render.texts.map { it.value }
      val rowNames = when (val frame = render.frame) {
        is FavoriteWidgetFrame.Team -> when (val body = frame.large.body) {
          is TeamDisplayBody.Visible -> when (val detail = body.detail) {
            is TeamDetail.Maps -> detail.rows.map { it.name }
            is TeamDetail.History -> detail.rows.map { it.name }
            null -> emptyList()
          }

          is TeamDisplayBody.Hidden -> body.history?.rows.orEmpty().map { it.name }
        }

        else -> emptyList()
      }
      assertEquals(
        "${render.name}: list heading requires visible rows",
        render.ghostRows > 0 || strings.any { it in rowNames },
        strings.any { it in setOf("Maps", "Recent", "Before that", "Recent results show here") },
      )
    }
  }

  @Test
  fun hiddenFramesHaveNoScoresMapsOrResultMarks() {
    renders().filter { it.hidden }.forEach { render ->
      val strings = render.texts.map { it.value }
      assertFalse("${render.name}: score leaked", strings.any { Regex(".*[0-9]+[–−-][0-9]+.*").matches(it) })
      assertFalse(
        "${render.name}: result or map leaked",
        strings.any {
          it in
            listOf("W", "L", "Maps", "Haven", "Lotus", "Ascent", "Split", "Icebox")
        },
      )
      assertFalse("${render.name}: result description leaked", render.descriptions.any { it == "Win" || it == "Loss" })
    }
  }

  @Test
  fun contentUsesRealHostBounds() {
    assertEquals(SizeMode.Exact, FavoriteTeamWidget().sizeMode)
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val configuration = Configuration(instrumentation.targetContext.resources.configuration).apply {
      fontScale = this@FavoriteTeamWidgetRenderTest.fontScale
      if (this@FavoriteTeamWidgetRenderTest.densityDpi != 0) densityDpi = this@FavoriteTeamWidgetRenderTest.densityDpi
    }
    val context = instrumentation.targetContext.createConfigurationContext(configuration)
    val typography = FavoriteWidgetTypography(context)
    renders().forEach { render ->
      val name = render.name
      val frame = render.frame
      val presentation = render.presentation
      val measure = TeamWidgetMeasure(frame, size, typography)
      assertTrue(
        "$name: chosen $presentation needs ${measure.height(presentation)} at $size",
        measure.height(presentation) <= size.height,
      )
      val richer = when {
        presentation.textStep > 0 -> presentation.copy(
          textStep = presentation.textStep - 1,
          heroSize =
          presentation.heroSize + 1,
        )

        presentation.heroSize < (if (size.height < 158.dp) 32 else 40) ->
          presentation.copy(heroSize = presentation.heroSize + 1)

        FavoriteWidgetPart.Form !in presentation ->
          presentation.copy(parts = presentation.parts + FavoriteWidgetPart.Form)

        FavoriteWidgetPart.Context !in presentation ->
          presentation.copy(parts = presentation.parts + FavoriteWidgetPart.Context)

        !presentation.large && FavoriteWidgetPart.Pills !in presentation ->
          presentation.copy(parts = presentation.parts + FavoriteWidgetPart.Pills)

        presentation.large && FavoriteWidgetPart.RegionalRank !in presentation ->
          presentation.copy(parts = presentation.parts + FavoriteWidgetPart.RegionalRank)

        presentation.large && presentation.rows < measure.rowCount -> presentation.copy(rows = presentation.rows + 1)

        presentation.compact -> presentation.copy(compact = false)

        else -> null
      }
      if (richer != null) {
        assertTrue(
          "$name: wasted space, the next richer candidate fits at $size, font=$fontScale, dpi=$densityDpi",
          measure.height(richer) > size.height,
        )
      }
      if (name == "idle-win" && densityDpi == 540 && fontScale == 2f && size == DpSize(235.55556.dp, 174.81482.dp)) {
        assertEquals("Idle hero", 35, presentation.heroSize)
        assertEquals("Idle kept parts", emptySet<FavoriteWidgetPart>(), presentation.parts)
        assertEquals("Idle kept content", "rows=0", keptContent(render))
        assertEquals("Idle form marks removed", 0, render.formCount)
        assertFalse("Idle context removed", "Oct 15 · No match scheduled" in render.texts.map { it.value })
        val hero = render.texts.single { it.value == "2–1" }
        assertEquals(
          "Idle hero in RemoteViews",
          TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 35f, context.resources.displayMetrics),
          hero.fontSize,
          0.01f,
        )
        assertTrue(
          "Idle hero is the largest text",
          render.texts.filter { it !== hero }.all {
            it.fontSize <
              hero.fontSize
          },
        )
      }
    }
  }

  private fun fullKeptProfile(name: String): String {
    val profiles = when {
      name == "setup-no-favorites" -> listOf(
        "large regional context form rows=0",
        "medium rows=0",
        "medium rows=0",
        "medium rows=0",
      )

      name == "placeholder" -> listOf(
        "large regional context form rows=0",
        "large regional context form rows=2",
        "large regional context form rows=4",
        "large regional context form rows=2",
      )

      name.startsWith("idle-empty") -> listOf(
        "large regional context form rows=0",
        "large regional context form rows=0",
        "large regional context form rows=0",
        "large regional context form rows=0",
      )

      name == "live-bo5" -> listOf(
        "large context form rows=0",
        "large context form rows=0",
        "large context form rows=0",
        "large regional context form rows=2",
      )

      name.startsWith("live") -> listOf(
        "large context form rows=0",
        "large context form rows=0",
        "large context form rows=0",
        "large regional context form rows=1",
      )

      else -> listOf(
        "large regional context form rows=0",
        "large regional context form rows=1",
        "large regional context form rows=4",
        "large regional context form rows=1",
      )
    }
    return profiles[listOf(136f, 213f, 330f, 229.33333f).indexOf(size.height.value)]
  }

  private fun assertScaledContent() {
    renders().forEach { render ->
      val strings = render.texts.map { it.value }
      assertEquals(
        "${render.name}: kept content",
        keptProfiles[keptProfile][scaledCaseNames.indexOf(render.name)],
        keptContent(render),
      )
      val required = when (render.name) {
        "setup-choose" -> listOf("Choose a team", "To use this widget,", "Tap to choose")

        "upcoming-long" -> listOf(
          "Sentinels International Academy",
          "Plays FNATIC International Academy",
          "2:14 PM",
          "Today",
          "#4 worldwide",
        )

        "idle-win" -> listOf("Sentinels", "Beat Paper Rex", "2–1", "#4 worldwide")

        "live-bo5-hidden" -> listOf("Sentinels", "Playing FNATIC", "Live", "Live now", "#4 worldwide")

        else -> listOf("Sentinels", "Playing FNATIC", "Live", "2–2", "#4 worldwide")
      }
      required.forEach { expected ->
        assertTrue(
          "${render.name}: missing $expected in $strings",
          strings.any {
            it == expected ||
              it.startsWith("$expected ·")
          },
        )
      }
      if (render.formCount > 0) {
        assertEquals("${render.name}: complete form", if (render.name.startsWith("live")) 6 else 5, render.formCount)
      }
      if (render.name == "setup-choose") {
        assertEquals("Val Esports" in strings, "Pick a team from your favorites" in strings)
      }
      if (render.name == "live-bo5" && "Maps" !in strings && "13–9" in strings) {
        assertEquals(
          listOf("13–9", "9–13", "13–9", "9–13", "Split 6–4"),
          strings.filter { it in setOf("13–9", "9–13", "Split 6–4") },
        )
      }
      if (render.name.startsWith("live")) {
        assertTrue("Live accent missing", "Live" in render.descriptions)
        assertEquals(render.primary, render.texts.first { it.value == "Live" }.color)
      }
    }
  }

  private fun keptContent(render: Render): String {
    val strings = render.texts.map { it.value }
    val rowNames = when (render.name) {
      "upcoming-long" -> listOf("Paper Rex", "DRX", "G2 Esports", "FNATIC")
      "idle-win" -> listOf("DRX", "G2 Esports", "FNATIC", "LOUD")
      "live-bo5" -> listOf("Haven", "Lotus", "Ascent", "Icebox", "Split")
      "live-bo5-hidden" -> listOf("Paper Rex", "DRX", "G2 Esports", "FNATIC")
      else -> emptyList()
    }
    val visibleRows = strings.filter { it in rowNames }.distinct()
    assertEquals("${render.name}: whole row prefix", rowNames.take(visibleRows.size), visibleRows)
    return buildList {
      if (strings.any { "#1 Americas" in it }) add("regional")
      if (strings.any {
          it in
            setOf(
              "Val Esports",
              "Masters London · Upper final · Bo3",
              "Masters London · Upper final · Bo5",
              "Scores hidden · Bo5",
              "Oct 15 · No match scheduled",
            )
        }
      ) {
        add("context")
      }
      if (render.name == "live-bo5" && "Maps" !in strings && "13–9" in strings) add("pills")
      if (render.formCount > 0) add("form")
      add("rows=${visibleRows.size}")
    }.joinToString(" ")
  }

  private fun renders(): List<Render> = cache.getOrPut(listOf(size, dark, fontScale, densityDpi, scaled)) {
    runBlocking { renderCases() }
  }

  private fun expectedText(frame: FavoriteWidgetFrame, large: Boolean) = WidgetTextExpectations().apply {
    empty(frame)
    if (frame is FavoriteWidgetFrame.Team) {
      val content = if (large) frame.large else frame.medium
      add(content.header.title, WidgetTextRole.NameTitle)
      add(content.header.ranking, WidgetTextRole.Ranking)
      add(frame.medium.header.ranking, WidgetTextRole.Ranking)
      logo(content.header.team, if (large) 40 else 28)
      when (val body = content.body) {
        is TeamDisplayBody.Visible -> {
          val hero = body.hero
          add(hero.lead, if (hero.opponent != null) WidgetTextRole.NameLead else WidgetTextRole.Lead)
          add(hero.value, WidgetTextRole.Hero)
          logo(hero.opponent, 44)
          when (hero) {
            is TeamHeroText.Upcoming -> {
              add(hero.day, WidgetTextRole.Secondary)
              add(hero.context, WidgetTextRole.Secondary)
            }

            is TeamHeroText.Idle -> add(hero.context, WidgetTextRole.Secondary)

            is TeamHeroText.Live -> {
              add(hero.liveLabel, WidgetTextRole.Secondary)
              when (val below = hero.below) {
                is TeamLiveBelow.Context -> add(below.text, WidgetTextRole.Secondary)
                is TeamLiveBelow.Pills -> below.items.forEach { add(it.text, WidgetTextRole.Pill) }
              }
            }
          }
          when (val detail = body.detail) {
            is TeamDetail.Maps -> {
              add(detail.heading, WidgetTextRole.Secondary)
              detail.rows.forEach { row ->
                add(row.name, WidgetTextRole.RowName)
                add(row.subtitle, WidgetTextRole.Secondary)
                add(row.score, WidgetTextRole.Score)
              }
            }

            is TeamDetail.History -> {
              add(detail.heading, WidgetTextRole.Secondary)
              detail.rows.forEach { row ->
                add(row.mark, WidgetTextRole.Mark)
                logo(row.opponent, 22)
                add(row.name, WidgetTextRole.RowName)
                add(row.date, WidgetTextRole.Secondary)
                add(row.score, WidgetTextRole.Score)
              }
            }

            null -> Unit
          }
        }

        is TeamDisplayBody.Hidden -> {
          val hero = body.hero
          add(hero.lead, if (hero.opponent != null) WidgetTextRole.NameLead else WidgetTextRole.Lead)
          add(hero.value, WidgetTextRole.Hero)
          add(hero.context, WidgetTextRole.Secondary)
          logo(hero.opponent, 44)
          when (hero) {
            is NeutralTeamHeroText.Upcoming -> add(hero.day, WidgetTextRole.Secondary)
            is NeutralTeamHeroText.Live -> add(hero.liveLabel, WidgetTextRole.Secondary)
            is NeutralTeamHeroText.Idle -> Unit
          }
          body.history?.let { list ->
            add(list.heading, WidgetTextRole.Secondary)
            list.rows.forEach { row ->
              logo(row.opponent, 22)
              add(row.name, WidgetTextRole.RowName)
              add(row.date, WidgetTextRole.Secondary)
            }
          }
        }
      }
    }
  }

  private suspend fun renderCases(): List<Render> {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val base = instrumentation.targetContext
    val configuration = Configuration(base.resources.configuration).apply {
      setLocales(LocaleList(Locale.US))
      this.fontScale = this@FavoriteTeamWidgetRenderTest.fontScale
      if (this@FavoriteTeamWidgetRenderTest.densityDpi !=
        0
      ) {
        this.densityDpi = this@FavoriteTeamWidgetRenderTest.densityDpi
      }
      uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    }
    val context = base.createConfigurationContext(configuration)
    val engine = GlobalContext.get().get<FavoriteWidgetEngine>()
    val locale = AndroidWidgetLocale(context, use24Hour = false)
    val source = UpcomingMatchesSnapshot(
      savedAtEpochMillis = 0,
      clientId = "render-client",
      hasFavorites = true,
      matches = emptyList(),
      theme = WidgetTheme(0, 0, 0, 0, 0, 0, false),
      favoriteTeams = listOf(WidgetFavoriteTeam("2", "Sentinels")),
    )
    val request = WidgetRequest(
      WidgetSelection.Chosen(WidgetKind.Team, "2"),
      source,
      Instant.parse("2026-10-16T12:00:00Z"),
      TimeZone.UTC,
    )
    val records = Json.parseToJsonElement(
      instrumentation.context.assets.open("favorite-team-widget/records.json")
        .bufferedReader().use { it.readText() },
    ).jsonObject
    val cases = mutableListOf(
      "setup-choose" to
        engine.project(request.copy(selection = WidgetSelection.None(WidgetKind.Team)), null, locale).frame,
      "setup-no-favorites" to
        engine.project(request.copy(source = source.copy(favoriteTeams = emptyList())), null, locale).frame,
      "placeholder" to engine.project(request, null, locale).frame,
    )
    records.forEach { (name, record) ->
      for (hidden in listOf(false, true)) {
        cases += (name + if (hidden) "-hidden" else "") to engine.project(
          request.copy(source = source.copy(spoilersHidden = hidden)),
          StoredWidgetRecord(record.toString()),
          locale,
        ).frame
      }
    }
    cases += "upcoming-bitmaps" to cases.first { it.first == "upcoming" }.second
    if (scaled) {
      cases.retainAll { it.first in setOf("live-bo5", "upcoming", "idle-win", "live-bo5-hidden", "setup-choose") }
      val upcoming = cases.indexOfFirst { it.first == "upcoming" }
      val frame = cases[upcoming].second as FavoriteWidgetFrame.Team
      fun longNames(value: TeamPresentation) = value.copy(
        header = value.header.copy(title = "Sentinels International Academy"),
        body = (value.body as TeamDisplayBody.Visible).let { body ->
          body.copy(
            hero = (body.hero as TeamHeroText.Upcoming)
              .copy(lead = "Plays FNATIC International Academy"),
          )
        },
      )
      cases[upcoming] = "upcoming-long" to frame.copy(medium = longNames(frame.medium), large = longNames(frame.large))
    }
    return cases.map { (name, frame) ->
      val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply {
        eraseColor(context.getColor(R.color.glance_colorPrimary))
      }
      val logos = WidgetBitmaps(
        if (name == "upcoming-bitmaps") {
          mapOf("https://example.test/sentinels.png" to bitmap, "https://example.test/fnatic.png" to bitmap)
        } else {
          emptyMap()
        },
      )
      val typography = FavoriteWidgetTypography(context)
      val presentation = teamPresentationFor(frame, size, typography)
      val remote = object : GlanceAppWidget() {
        override suspend fun provideGlance(context: Context, id: GlanceId) {
          provideContent {
            GlanceTheme {
              Box(GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground)) {
                TeamWidgetLayout(frame, logos)
              }
            }
          }
        }
      }.compose(context, size = size)
      var result: Render? = null
      instrumentation.runOnMainSync {
        val hostContext = instrumentation.context.createConfigurationContext(configuration)
        val view = remote.apply(hostContext, FrameLayout(hostContext))
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
        val directory = File(context.filesDir, "favorite-team-renders" + if (scaled) "/scale" else "").apply {
          mkdirs()
        }
        val sizeName = when {
          fullLarge -> "large"
          size.height >= 229.dp -> "portrait-4x2"
          this@FavoriteTeamWidgetRenderTest.large -> "compact-large"
          else -> "medium"
        }
        val suffix = if (scaled) "-font-$fontScale-dpi-$densityDpi-${size.width.value}x${size.height.value}" else ""
        File(directory, "$name-$sizeName-${if (dark) "dark" else "light"}$suffix.png")
          .outputStream().use { output.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val allViews = favoriteWidgetDescendants(view)
        if (scaled) {
          File(directory, "$name$suffix.txt").writeText(
            "@forms=${allViews.filterIsInstance<android.widget.ImageView>().size}\n" +
              allViews.filterIsInstance<TextView>().filter {
                it.text.isNotEmpty()
              }.joinToString("\n") { it.text.toString() },
          )
        }
        val inspection =
          inspectFavoriteWidgetView(
            view,
            expectedText(frame, presentation.large),
            presentation.textStep,
            presentation.heroSize,
          )
        result =
          Render(
            name, frame, presentation, name.endsWith("-hidden"),
            inspection.clippedViews,
            inspection.texts,
            allViews.mapNotNull { it.contentDescription?.toString() },
            allViews.filterIsInstance<android.widget.ImageView>().size,
            if (frame is FavoriteWidgetFrame.Setup || frame is FavoriteWidgetFrame.Placeholder) {
              allViews.count {
                it.visibility == View.VISIBLE && it.width == (22 * density).roundToInt() &&
                  it.height == (22 * density).roundToInt()
              }
            } else {
              0
            },
            context.getColor(R.color.glance_colorOnSurface),
            context.getColor(
              R.color.glance_colorPrimary,
            ),
            context.getColor(R.color.glance_colorWidgetBackground), output.getPixel(0, 0),
          )
        if (scaled) File(directory, "$name$suffix.kept").writeText(keptContent(requireNotNull(result)))
        output.recycle()
      }
      bitmap.recycle()
      requireNotNull(result)
    }
  }

  private data class Render(
    val name: String,
    val frame: FavoriteWidgetFrame,
    val presentation: FavoriteWidgetPresentation,
    val hidden: Boolean,
    val clippedViews: List<String>,
    val texts: List<TextMeasurement>,
    val descriptions: List<String>,
    val formCount: Int,
    val ghostRows: Int,
    val onSurface: Int,
    val primary: Int,
    val background: Int,
    val cornerColor: Int,
  )

  companion object {
    private val scaledCaseNames = listOf("setup-choose", "upcoming-long", "live-bo5", "live-bo5-hidden", "idle-win")
    private val keptProfiles = listOf(
      listOf(
        "context rows=0",
        "regional context form rows=3",
        "regional context form rows=3",
        "regional context form rows=3",
        "regional context form rows=3",
      ),
      listOf(
        "context rows=0",
        "regional context form rows=2",
        "regional context form rows=3",
        "regional context form rows=2",
        "regional context form rows=2",
      ),
      listOf(
        "context rows=0",
        "context form rows=0",
        "context form rows=0",
        "context form rows=0",
        "context form rows=0",
      ),
      listOf(
        "context rows=0",
        "regional context form rows=1",
        "regional context form rows=1",
        "regional context form rows=1",
        "regional context form rows=1",
      ),
      listOf(
        "context rows=0",
        "regional context form rows=0",
        "regional context form rows=1",
        "regional context form rows=0",
        "regional context form rows=0",
      ),
      listOf(
        "context rows=0",
        "form rows=0",
        "form rows=0",
        "context form rows=0",
        "context form rows=0",
      ),
      listOf(
        "context rows=0",
        "regional context form rows=2",
        "regional context form rows=2",
        "regional context form rows=2",
        "regional context form rows=2",
      ),
      listOf(
        "context rows=0",
        "regional context form rows=0",
        "regional context form rows=0",
        "regional context form rows=0",
        "regional context form rows=0",
      ),
      listOf(
        "rows=0",
        "rows=0",
        "rows=0",
        "rows=0",
        "rows=0",
      ),
      listOf(
        "context rows=0",
        "regional context form rows=4",
        "regional context form rows=5",
        "regional context form rows=4",
        "regional context form rows=4",
      ),
      listOf(
        "rows=0",
        "context form rows=0",
        "context form rows=0",
        "context form rows=0",
        "context form rows=0",
      ),
      listOf(
        "rows=0",
        "context form rows=0",
        "rows=0",
        "rows=0",
        "context form rows=0",
      ),
      listOf(
        "context rows=0",
        "regional context form rows=1",
        "regional context form rows=2",
        "regional context form rows=1",
        "regional context form rows=1",
      ),
      listOf(
        "rows=0",
        "form rows=0",
        "form rows=0",
        "context form rows=0",
        "form rows=0",
      ),
      listOf(
        "rows=0",
        "form rows=0",
        "form rows=0",
        "form rows=0",
        "form rows=0",
      ),
      listOf(
        "rows=0",
        "form rows=0",
        "form rows=0",
        "context form rows=0",
        "context form rows=0",
      ),
    )

    private val cache = mutableMapOf<List<Any>, List<Render>>()

    @JvmStatic
    @Parameterized.Parameters(name = "width={0}, height={1}, dark={2}, font={3}, dpi={4}, scaled={5}")
    fun sizesAndThemes(): List<Array<Any>> = listOf(
      DpSize(280.dp, 136.dp),
      DpSize(280.dp, 213.dp),
      DpSize(280.dp, 330.dp),
      DpSize(292.57144.dp, 229.33333.dp),
    ).flatMap { size ->
      listOf(false, true).map { dark -> arrayOf<Any>(size.width.value, size.height.value, dark, 1f, 0, false, -1) }
    } + listOf(
      Triple(356, DpSize(338.42694.dp, 280.44943.dp), listOf(0, 1, 2)),
      Triple(356, DpSize(338.42694.dp, 222.02246.dp), listOf(3, 4, 5)),
      Triple(356, DpSize(338.42694.dp, 268.76404.dp), listOf(0, 6, 2)),
      Triple(356, DpSize(519.99994.dp, 167.191.dp), listOf(7, 7, 8)),
      Triple(356, DpSize(671.01117.dp, 167.191.dp), listOf(7, 7, 8)),
      Triple(356, DpSize(338.42694.dp, 577.0786.dp), listOf(9, 9, 2)),
      Triple(356, DpSize(338.42694.dp, 460.2247.dp), listOf(9, 9, 2)),
      Triple(356, DpSize(338.42694.dp, 553.7078.dp), listOf(9, 9, 2)),
      Triple(356, DpSize(519.99994.dp, 344.26965.dp), listOf(9, 9, 6)),
      Triple(356, DpSize(671.01117.dp, 344.26965.dp), listOf(9, 9, 6)),
      Triple(356, DpSize(235.dp, 103.dp), listOf(8, 8, 8)),
      Triple(356, DpSize(235.dp, 221.dp), listOf(10, 11, 8)),
      Triple(356, DpSize(235.dp, 344.dp), listOf(10, 11, 8)),
      Triple(420, DpSize(292.57144.dp, 229.33333.dp), listOf(12, 2, 13)),
      Triple(420, DpSize(292.57144.dp, 196.57143.dp), listOf(4, 2, 14)),
      Triple(420, DpSize(292.57144.dp, 180.57143.dp), listOf(7, 2, 14)),
      Triple(420, DpSize(292.57144.dp, 221.71428.dp), listOf(3, 2, 13)),
      Triple(420, DpSize(440.38095.dp, 137.14285.dp), listOf(7, 14, 8)),
      Triple(420, DpSize(537.9048.dp, 137.14285.dp), listOf(7, 14, 8)),
      Triple(420, DpSize(292.57144.dp, 474.66666.dp), listOf(9, 2, 10)),
      Triple(420, DpSize(292.57144.dp, 409.14285.dp), listOf(9, 2, 10)),
      Triple(420, DpSize(292.57144.dp, 377.14285.dp), listOf(9, 2, 10)),
      Triple(420, DpSize(292.57144.dp, 459.42856.dp), listOf(9, 2, 10)),
      Triple(420, DpSize(440.38095.dp, 284.1905.dp), listOf(0, 1, 7)),
      Triple(420, DpSize(537.9048.dp, 284.1905.dp), listOf(0, 1, 7)),
      Triple(420, DpSize(235.dp, 103.dp), listOf(8, 8, 8)),
      Triple(420, DpSize(235.dp, 221.dp), listOf(10, 10, 8)),
      Triple(420, DpSize(235.dp, 344.dp), listOf(10, 10, 8)),
      Triple(500, DpSize(251.52.dp, 195.84.dp), listOf(10, 10, 8)),
      Triple(500, DpSize(251.52.dp, 153.6.dp), listOf(2, 2, 8)),
      Triple(500, DpSize(251.52.dp, 189.44.dp), listOf(10, 10, 8)),
      Triple(500, DpSize(397.44.dp, 111.04.dp), listOf(8, 8, 8)),
      Triple(500, DpSize(456.32.dp, 111.04.dp), listOf(8, 8, 8)),
      Triple(500, DpSize(251.52.dp, 407.68.dp), listOf(10, 10, 8)),
      Triple(500, DpSize(251.52.dp, 323.2.dp), listOf(10, 10, 8)),
      Triple(500, DpSize(251.52.dp, 394.88.dp), listOf(10, 10, 8)),
      Triple(500, DpSize(397.44.dp, 232.0.dp), listOf(12, 3, 2)),
      Triple(500, DpSize(456.32.dp, 232.0.dp), listOf(12, 3, 7)),
      Triple(500, DpSize(235.dp, 103.dp), listOf(8, 8, 8)),
      Triple(500, DpSize(235.dp, 221.dp), listOf(10, 11, 8)),
      Triple(500, DpSize(235.dp, 344.dp), listOf(10, 11, 8)),
      Triple(540, DpSize(235.55556.dp, 174.81482.dp), listOf(10, 15, 8)),
      Triple(540, DpSize(235.55556.dp, 137.48148.dp), listOf(2, 14, 8)),
      Triple(540, DpSize(235.55556.dp, 170.07408.dp), listOf(10, 15, 8)),
      Triple(540, DpSize(361.18518.dp, 103.111115.dp), listOf(8, 8, 8)),
      Triple(540, DpSize(425.18518.dp, 103.111115.dp), listOf(8, 8, 8)),
      Triple(540, DpSize(235.55556.dp, 365.62964.dp), listOf(10, 10, 8)),
      Triple(540, DpSize(235.55556.dp, 290.96295.dp), listOf(10, 10, 8)),
      Triple(540, DpSize(235.55556.dp, 356.14816.dp), listOf(10, 10, 8)),
      Triple(540, DpSize(361.18518.dp, 212.14815.dp), listOf(3, 7, 2)),
      Triple(540, DpSize(425.18518.dp, 212.14815.dp), listOf(3, 7, 7)),
      Triple(540, DpSize(235.dp, 103.dp), listOf(8, 8, 8)),
      Triple(540, DpSize(235.dp, 221.dp), listOf(10, 11, 8)),
      Triple(540, DpSize(235.dp, 344.dp), listOf(10, 11, 8)),
    ).flatMap { (density, size, profiles) ->
      listOf(1f, 1.3f, 2f).mapIndexed { index, scale ->
        arrayOf<Any>(size.width.value, size.height.value, false, scale, density, true, profiles[index])
      }
    }
  }
}
