/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.LocaleList
import android.view.View
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
import dev.staticvar.vlr.shared.widget.WidgetFavoriteEvent
import dev.staticvar.vlr.shared.widget.WidgetFavorites
import dev.staticvar.vlr.shared.widget.WidgetTheme
import dev.staticvar.vlr.shared.widget.favorite.EndedText
import dev.staticvar.vlr.shared.widget.favorite.EventDisplayBody
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetEngine
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetFrame
import dev.staticvar.vlr.shared.widget.favorite.FinishedEventText
import dev.staticvar.vlr.shared.widget.favorite.FinishedListText
import dev.staticvar.vlr.shared.widget.favorite.GapText
import dev.staticvar.vlr.shared.widget.favorite.HiddenEventText
import dev.staticvar.vlr.shared.widget.favorite.MatchListText
import dev.staticvar.vlr.shared.widget.favorite.NeutralListText
import dev.staticvar.vlr.shared.widget.favorite.ScheduledEventText
import dev.staticvar.vlr.shared.widget.favorite.StoredWidgetRecord
import dev.staticvar.vlr.shared.widget.favorite.UpcomingListText
import dev.staticvar.vlr.shared.widget.favorite.VisibleEventText
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
class FavoriteEventWidgetRenderTest(
  width: Float,
  height: Float,
  private val dark: Boolean,
  private val fontScale: Float,
  private val densityDpi: Int,
) {
  private val size = DpSize(width.dp, height.dp)

  @Test
  fun textFitsWithoutClippingOrOverlap() {
    val problems = renders().flatMap { render ->
      render.inspection.clippedViews.map { "${render.name}: $it" } +
        render.inspection.texts.filter {
          it.problems.isNotEmpty()
        }.map { "${render.name}: ${it.value}: ${it.problems}" }
    }
    assertTrue(problems.joinToString("\n"), problems.isEmpty())
  }

  @Test
  fun frameStringsLogosAndThemeReachRemoteViews() {
    renders().forEach { render ->
      val strings = render.inspection.texts.map { it.value }
      frameStrings(render).forEach { assertTrue("${render.name}: missing frame text $it", it in strings) }
      val hidden = render.name.endsWith("-hidden")
      val name = render.name.removeSuffix("-hidden")
      val expected = when (name) {
        "setup-choose" -> listOf("Choose an event", "To use this widget,", "Tap to choose")

        "setup-no-favorites" -> listOf("No favorite events", "To use this widget,", "Open Val Esports")

        "placeholder" -> listOf("Event", "TBD")

        "scheduled-first" -> listOf("Champions Paris", "First match", "6:00 PM")

        "scheduled-today", "scheduled-group", "scheduled-tomorrow" -> listOf(
          "Champions Paris",
          "LOUD vs Team Vitality",
          "6:00 PM",
        )

        "scheduled-delayed" -> listOf("LOUD vs Team Vitality", "Any minute")

        "scheduled-tbd" -> listOf("LOUD vs TBD", "TBD", "?")

        "live-bo5" -> listOf("LOUD vs Team Vitality", "Live", if (hidden) "Live now" else "2–2")

        "live-bo3-next", "live-earlier" -> listOf("LOUD vs Team Vitality", "Live", if (hidden) "Live now" else "2–1")

        "live-one-map" -> listOf("LOUD vs Team Vitality", "Live", if (hidden) "Live now" else "0–0")

        "live-no-score" -> listOf("LOUD vs Team Vitality", "Live", if (hidden) "Live now" else "–")

        "between" -> if (hidden) {
          listOf(
            "LOUD vs Team Vitality",
            "6:00 PM",
          )
        } else {
          listOf("LOUD beat Team Vitality", "2–1")
        }

        "gap-start" -> listOf("Starts on", "Oct 20")

        "gap-tbd" -> listOf("Starts on", "TBD")

        "gap-next" -> listOf("Next match", "TBD")

        "finished-prizes", "finished-matches" -> if (hidden) {
          listOf(
            "Event ended",
            "Oct 15",
          )
        } else {
          listOf("Champions", "LOUD")
        }

        "finished-unconfirmed" -> listOf("Event ended", "Oct 15")

        "finished-long-champion" -> listOf("Champions", LongChampion)

        "live-bo5-bitmaps" -> listOf("LOUD vs Team Vitality", "Live", "2–2")

        else -> throw AssertionError(name)
      }
      expected.forEach { assertTrue("${render.name}: missing $it in $strings", it in strings) }
      val fit = render.fit
      if (name == "scheduled-today" || name == "scheduled-group") assertTrue("Today" in strings)
      if (name == "scheduled-tomorrow") assertTrue("Tomorrow" in strings)
      if (name.startsWith("live")) assertTrue("live accessibility", "Live" in render.descriptions)
      if (!hidden && name.startsWith("live-bo5") && FavoriteWidgetPart.Pills in fit) {
        listOf("13–9", "9–13", "Split 6–4").forEach { assertTrue("${render.name}: missing pill $it", it in strings) }
      }
      if (!hidden && name == "live-bo5" && FavoriteWidgetPart.OtherLive in fit) assertTrue("+1" in strings)
      if (!hidden && name.startsWith("live") && FavoriteWidgetPart.Beside in fit) {
        assertTrue("Grand final" in strings)
        assertTrue((if (name.startsWith("live-bo5")) "Bo5" else "Bo3") in strings)
      }
      if (name == "live-bo5-bitmaps") assertTrue("decoded bitmaps", render.images >= 3)
      if (render.frame is FavoriteWidgetFrame.Event) {
        assertTrue(
          "event logo description",
          "Champions Paris" in render.descriptions,
        )
      }
      assertEquals("Material You background", render.background, render.corner)
      assertTrue("Material You text", render.inspection.texts.any { it.color == render.onSurface })
    }
  }

  @Test
  fun hiddenFramesHaveNoScoresMapsStandingsOrVictory() {
    renders().filter { it.name.endsWith("-hidden") }.forEach { render ->
      val strings = render.inspection.texts.map { it.value }
      assertFalse(
        "${render.name}: score or standing leaked",
        strings.any {
          Regex(".*[0-9]+[–−-][0-9]+.*").matches(it)
        },
      )
      assertFalse(
        "${render.name}: result leaked",
        strings.any {
          it in listOf(
            "Haven", "Lotus", "Ascent", "Split", "Icebox", "Champions", "Final standings",
            "Group Omega · W–L · maps", "$400,000", "$200,000", "$100,000",
          ) || it.startsWith("Beat ") || it.contains(" beat ")
        },
      )
    }
  }

  @Test
  fun contentUsesRealHostBoundsWithoutWastedSpace() {
    assertEquals(SizeMode.Exact, FavoriteEventWidget().sizeMode)
    renders().forEach { render ->
      val p = render.fit
      val measure = EventWidgetMeasure(render.frame, size, FavoriteWidgetTypography(context()))
      val richer = when {
        p.textStep > 0 -> p.copy(textStep = p.textStep - 1, heroSize = p.heroSize + 1)
        p.heroSize < favoriteWidgetHeroFloor(size) -> p.copy(heroSize = p.heroSize + 1)
        FavoriteWidgetPart.Subtitle !in p -> p.copy(parts = p.parts + FavoriteWidgetPart.Subtitle)
        FavoriteWidgetPart.Context !in p -> p.copy(parts = p.parts + FavoriteWidgetPart.Context)
        FavoriteWidgetPart.Beside !in p -> p.copy(parts = p.parts + FavoriteWidgetPart.Beside)
        FavoriteWidgetPart.Pills !in p -> p.copy(parts = p.parts + FavoriteWidgetPart.Pills)
        FavoriteWidgetPart.Progress !in p -> p.copy(parts = p.parts + FavoriteWidgetPart.Progress)
        FavoriteWidgetPart.OtherLive !in p -> p.copy(parts = p.parts + FavoriteWidgetPart.OtherLive)
        p.large && p.rows < (measure.list?.count ?: 0) ->
          p.copy(rows = p.rows + 1, parts = if (p.rows == 0) p.parts - FavoriteWidgetPart.Pills else p.parts)
        p.compact -> p.copy(compact = false)
        p.heroSize < favoriteWidgetHeroStart(size, p.large) ->
          p.copy(heroSize = minOf(favoriteWidgetHeroStart(size, p.large), p.heroSize + 2))
        else -> null
      }
      assertTrue("${render.name}: chosen content does not fit", measure.height(p) <= size.height)
      if (richer != null) {
        assertTrue(
          "${render.name}: richer candidate fits at $size, " +
            "font=$fontScale, dpi=$densityDpi",
          measure.height(richer) > size.height,
        )
      }
    }
  }

  @Test
  fun listHeadingsRequireVisibleWholeRows() {
    renders().forEach { render ->
      val strings = render.inspection.texts.map { it.value }
      val list = EventWidgetMeasure(render.frame, size, FavoriteWidgetTypography(context())).list
      if (list != null) {
        assertEquals("${render.name}: heading and rows", render.rows > 0, list.heading in strings)
        assertEquals("${render.name}: whole rows", render.fit.rows, render.rows)
      }
    }
    val rows = renders().associate { it.name to it.fit.rows }
    assertEquals("long champion name keeps the list", rows["finished-prizes"], rows["finished-long-champion"])
  }

  private fun frameStrings(render: Render): List<String> = buildList {
    val fit = render.fit
    fun context(value: String) {
      if (FavoriteWidgetPart.Context in fit) add(value)
    }
    fun scheduled(hero: ScheduledEventText) {
      add(hero.lead)
      add(hero.time)
      hero.day?.let(::add)
      context(hero.context)
    }
    fun ended(hero: EndedText) {
      add(hero.lead)
      add(hero.date)
      context(hero.context)
    }
    fun gap(hero: GapText) {
      add(hero.lead)
      add(hero.value)
      context(hero.context)
    }
    fun matches(list: MatchListText?) {
      if (fit.rows == 0 || list == null) return
      add(list.heading)
      list.rows.take(fit.rows).forEach {
        addAll(listOf(it.day, it.time, it.leftName, it.rightName, it.round))
        it.leftScore?.let(::add)
        it.rightScore?.let(::add)
      }
    }
    fun neutral(list: NeutralListText?) {
      if (fit.rows == 0 || list == null) return
      add(list.heading)
      list.rows.take(fit.rows).forEach { addAll(listOf(it.day, it.time, it.leftName, it.rightName, it.round)) }
    }
    when (val frame = render.frame) {
      is FavoriteWidgetFrame.Setup -> {
        addAll(listOf(frame.text.title, frame.text.lead, frame.text.action))
        context(frame.text.subtitle)
        context(frame.text.instruction)
        if (fit.rows > 0) add(frame.text.ghost)
      }

      is FavoriteWidgetFrame.Placeholder -> {
        addAll(listOf(frame.text.title, frame.text.value))
        if (fit.rows > 0) add(frame.text.ghost)
      }

      is FavoriteWidgetFrame.Event -> {
        val value = if (fit.large) frame.large else frame.medium
        add(value.header.title)
        if (FavoriteWidgetPart.Subtitle in fit) add(value.header.subtitle)
        when (val body = value.body) {
          is EventDisplayBody.Visible -> when (val state = body.state) {
            is VisibleEventText.Scheduled -> {
              scheduled(state.hero)
              when (val list = state.list) {
                is UpcomingListText.Matches -> matches(list.value)

                is UpcomingListText.Group -> if (fit.rows > 0) {
                  add(list.heading)
                  list.rows.take(fit.rows).forEach { addAll(listOf(it.rank, it.team.name, it.record, it.maps)) }
                }

                null -> Unit
              }
            }

            is VisibleEventText.Live -> {
              val hero = state.hero
              addAll(listOf(hero.lead, hero.score, hero.liveLabel))
              if (FavoriteWidgetPart.OtherLive in fit) hero.otherLive?.let(::add)
              if (FavoriteWidgetPart.Beside in fit) {
                add(hero.beside.stage)
                hero.beside.format?.let(::add)
              }
              if (FavoriteWidgetPart.Pills in fit) addAll(hero.pills.map { it.text })
              matches(state.list)
            }

            is VisibleEventText.Between -> {
              addAll(listOf(state.hero.lead, state.hero.score))
              context(state.hero.context)
              matches(state.list)
            }

            is VisibleEventText.Gap -> {
              gap(state.hero)
              matches(state.list)
            }

            is VisibleEventText.Finished -> {
              when (val hero = state.hero) {
                is FinishedEventText.Champion -> {
                  add(hero.label)
                  add(hero.name)
                  context(hero.context)
                }

                is FinishedEventText.Unconfirmed -> ended(hero.ended)
              }
              when (val list = state.list) {
                is FinishedListText.Matches -> matches(list.value)

                is FinishedListText.Prizes -> if (fit.rows > 0) {
                  add(list.heading)
                  list.rows.take(fit.rows).forEach { addAll(listOf(it.position, it.team.name, it.amount)) }
                }

                null -> Unit
              }
            }
          }

          is EventDisplayBody.Hidden -> when (val state = body.state) {
            is HiddenEventText.Scheduled -> {
              scheduled(state.hero)
              neutral(state.list)
            }

            is HiddenEventText.Live -> {
              addAll(listOf(state.hero.lead, state.hero.label, state.hero.liveLabel))
              context(state.hero.context)
              neutral(state.list)
            }

            is HiddenEventText.Gap -> {
              gap(state.hero)
              neutral(state.list)
            }

            is HiddenEventText.Finished -> {
              ended(state.hero)
              neutral(state.list)
            }
          }
        }
      }

      is FavoriteWidgetFrame.Team -> throw AssertionError("Unexpected Team frame")
    }
  }

  private fun context(): Context {
    val base = InstrumentationRegistry.getInstrumentation().targetContext
    return base.createConfigurationContext(
      Configuration(base.resources.configuration).apply {
        setLocales(LocaleList(Locale.US))
        fontScale = this@FavoriteEventWidgetRenderTest.fontScale
        if (this@FavoriteEventWidgetRenderTest.densityDpi !=
          0
        ) {
          densityDpi = this@FavoriteEventWidgetRenderTest.densityDpi
        }
        uiMode =
          (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
          if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
      },
    )
  }
  private fun renders() = cache.getOrPut(listOf(size, dark, fontScale, densityDpi)) {
    runBlocking { renderCases() }
  }
  private fun expectedText(frame: FavoriteWidgetFrame, fit: FavoriteWidgetPresentation) = WidgetTextExpectations().apply {
    val large = fit.large
    empty(frame)
    if (frame is FavoriteWidgetFrame.Event) {
      val value = if (large) frame.large else frame.medium
      add(value.header.title, WidgetTextRole.NameTitle)
      add(value.header.subtitle, WidgetTextRole.Secondary)
      logo(value.header.event, fit.headerLogo.value.toInt())
      val content = value.content()
      val hero = content.hero
      add(hero.lead, if (hero.pair != null) WidgetTextRole.NameLead else WidgetTextRole.Lead)
      add(hero.value, if (hero.champion != null) WidgetTextRole.HeroName else WidgetTextRole.Hero)
      add(hero.context, WidgetTextRole.Secondary)
      add(hero.day, WidgetTextRole.Secondary)
      add(hero.liveLabel, WidgetTextRole.Secondary)
      add(hero.otherLive, WidgetTextRole.Secondary)
      hero.beside?.let {
        add(it.stage, WidgetTextRole.Stage)
        add(it.format, WidgetTextRole.Stage)
      }
      hero.pills.forEach { add(it.text, WidgetTextRole.Pill) }
      hero.pair?.let {
        logo(it.left, fit.pairLogo.value.toInt())
        logo(it.right, fit.pairLogo.value.toInt())
      }
      logo(hero.champion, fit.championLogo.value.toInt())
      when (val list = content.list) {
        is EventWidgetList.Matches -> {
          add(list.heading, WidgetTextRole.Heading)
          list.rows.forEach { row ->
            add(row.day, WidgetTextRole.Caption)
            add(row.time, WidgetTextRole.Clock)
            add(row.round, WidgetTextRole.Round)
            logo(row.left, 18)
            logo(row.right, 18)
            add(row.leftName, WidgetTextRole.MatchName)
            add(row.rightName, WidgetTextRole.MatchName)
            add(row.leftScore, WidgetTextRole.MatchScore)
            add(row.rightScore, WidgetTextRole.MatchScore)
          }
        }

        is EventWidgetList.Group -> {
          add(list.heading, WidgetTextRole.Heading)
          list.rows.forEach { row ->
            add(row.rank, WidgetTextRole.Rank)
            logo(row.team, 24)
            add(row.team.name, WidgetTextRole.GroupName)
            add(row.record, WidgetTextRole.Record)
            add(row.maps, WidgetTextRole.Maps)
          }
        }

        is EventWidgetList.Prizes -> {
          add(list.heading, WidgetTextRole.Heading)
          list.rows.forEach { row ->
            add(row.position, WidgetTextRole.Position)
            logo(row.team, 24)
            add(row.team.name, WidgetTextRole.GroupName)
            add(row.amount, WidgetTextRole.Amount)
          }
        }

        is EventWidgetList.Ghost, null -> Unit
      }
    }
  }

  private suspend fun renderCases(): List<Render> {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = context()
    val locale = AndroidWidgetLocale(context, use24Hour = false)
    val engine = GlobalContext.get().get<FavoriteWidgetEngine>()
    val source =
      UpcomingMatchesSnapshot(
        savedAtEpochMillis = 0,
        hasFavorites = true,
        matches = emptyList(),
        theme = WidgetTheme(0, 0, 0, 0, 0, 0, false),
        clientId = "render-client",
        favoriteEvents = listOf(WidgetFavoriteEvent("3122", "Champions Paris")),
        favorites = WidgetFavorites(teamIds = listOf("1"), eventIds = listOf("3122")),
      )
    val request =
      WidgetRequest(
        WidgetSelection.Chosen(WidgetKind.Event, "3122"),
        source,
        Instant.parse("2026-10-16T12:00:00Z"),
        TimeZone.UTC,
      )
    val cases = mutableListOf(
      "setup-choose" to
        engine.project(request.copy(selection = WidgetSelection.None(WidgetKind.Event)), null, locale).frame,
      "setup-no-favorites" to
        engine.project(request.copy(source = source.copy(favoriteEvents = emptyList())), null, locale).frame,
      "placeholder" to engine.project(request, null, locale).frame,
    )
    val records = Json.parseToJsonElement(
      instrumentation.context.assets.open("favorite-event-widget/records.json").bufferedReader().use {
        it.readText()
      },
    ).jsonObject
    records.forEach { (name, record) ->
      for (hidden in listOf(false, true)) {
        cases +=
          (name + if (hidden) "-hidden" else "") to
          engine.project(
            request.copy(source = source.copy(spoilersHidden = hidden)),
            StoredWidgetRecord(record.toString()),
            locale,
          ).frame
      }
    }
    cases += "live-bo5-bitmaps" to cases.first { it.first == "live-bo5" }.second
    val longChampion = records.getValue("finished-prizes").toString().replace("\"LOUD\"", "\"$LongChampion\"")
    cases += "finished-long-champion" to engine.project(request, StoredWidgetRecord(longChampion), locale).frame
    return cases.map { (name, frame) ->
      val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply {
        eraseColor(context.getColor(R.color.glance_colorPrimary))
      }
      val logos =
        WidgetBitmaps(if (name.endsWith("-bitmaps")) frame.logoSources.associateWith { bitmap } else emptyMap())
      val fit = eventPresentationFor(frame, size, FavoriteWidgetTypography(context))
      val remote = object : GlanceAppWidget() {
        override suspend fun provideGlance(context: Context, id: GlanceId) {
          provideContent {
            GlanceTheme {
              Box(GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground)) {
                EventWidgetLayout(frame, logos)
              }
            }
          }
        }
      }.compose(context, size = size)
      var result: Render? = null
      instrumentation.runOnMainSync {
        val hostContext = instrumentation.context.createConfigurationContext(context.resources.configuration)
        val view = remote.apply(hostContext, FrameLayout(hostContext))
        val density = context.resources.displayMetrics.density
        val w = (size.width.value * density).roundToInt()
        val h = (size.height.value * density).roundToInt()
        view.measure(
          View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, w, h)
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(output))
        val dir = File(context.filesDir, "favorite-event-renders").apply { mkdirs() }
        val stem = "$name-${size.width.value}x${size.height.value}-" +
          "${if (dark) "dark" else "light"}-font-$fontScale-dpi-$densityDpi"
        File(dir, "$stem.png").outputStream().use { output.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val all = favoriteWidgetDescendants(view)
        val cardWidth = ((size.width.value - 32 - FavoriteWidgetCardPadding.value * 2) * density).roundToInt()
        val heading = EventWidgetMeasure(frame, size, FavoriteWidgetTypography(context)).list?.heading
        val dividers = all.count {
          kotlin.math.abs(it.width - cardWidth) <= 3 && it.height == FavoriteWidgetDivider.value.times(density).roundToInt()
        }
        val rowCount = if (all.any { it is TextView && it.text.toString() == heading }) dividers + 1 else 0
        val inspection = inspectFavoriteWidgetView(view, expectedText(frame, fit), fit.textStep, fit.heroSize)
        result =
          Render(
            name, frame, fit, inspection,
            all.mapNotNull {
              it.contentDescription?.toString()
            },
            all.filterIsInstance<android.widget.ImageView>().size, rowCount,
            context.getColor(
              R.color.glance_colorWidgetBackground,
            ),
            output.getPixel(0, 0), context.getColor(R.color.glance_colorOnSurface),
          )
        output.recycle()
      }
      bitmap.recycle()
      requireNotNull(result)
    }
  }
  private data class Render(
    val name: String,
    val frame: FavoriteWidgetFrame,
    val fit: FavoriteWidgetPresentation,
    val inspection: FavoriteWidgetViewInspection,
    val descriptions: List<String>,
    val images: Int,
    val rows: Int,
    val background: Int,
    val corner: Int,
    val onSurface: Int,
  )
  companion object {
    private const val LongChampion = "Leviatán Esports Academy Gaming Club"
    private val cache = mutableMapOf<List<Any>, List<Render>>()

    @JvmStatic
    @Parameterized.Parameters(name = "width={0}, height={1}, dark={2}, font={3}, dpi={4}")
    fun hosts(): List<Array<Any>> = favoriteWidgetRenderHosts.map { it.parameters() }
  }
}
