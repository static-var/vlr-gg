package dev.staticvar.vlr.android.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.test.platform.app.InstrumentationRegistry
import dev.staticvar.vlr.android.R
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PrismWidgetRenderTest {
  @Test
  fun remoteViewsKeepTypefaceScoresAndTwoLineNames() = runBlocking {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val base = instrumentation.targetContext
    val match = WidgetMatch("test", "VCT Pacific", "Team Liquid", "Paper Rex", 1900000000000,
      WidgetMatchStatus.LIVE, 1, 2, "BO3", "Playoffs")
    for (dark in listOf(false, true)) {
      val configuration = Configuration(base.resources.configuration).apply {
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
          if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
      }
      val context = base.createConfigurationContext(configuration)
      for (small in listOf(true, false)) {
        for (state in listOf("live", "hidden", "upcoming", "long-names", "compact")) {
          val fixture = when (state) {
            "upcoming" -> match.copy(status = WidgetMatchStatus.UPCOMING)
            "long-names" -> match.copy(team1 = "Shopify Rebellion Gold", team2 = "Twisted Minds Esports")
            else -> match
          }
          val hidden = state == "hidden"
          val size = DpSize(if (small) 158.dp else 338.dp, if (state == "compact") 110.dp else 158.dp)
          val remote = object : GlanceAppWidget() {
            override suspend fun provideGlance(context: Context, id: GlanceId) {
              provideContent {
                GlanceTheme {
                  Column(GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground)
                    .padding(horizontal = 16.dp, vertical = 12.dp)) {
                    PrismMatchCard(fixture, WidgetStrings(context), hidden, small, GlanceModifier.fillMaxSize(), compact = state == "compact")
                  }
                }
              }
            }
          }.compose(context, size = size)
          instrumentation.runOnMainSync {
            val hostContext = instrumentation.context.createConfigurationContext(configuration)
            val view = remote.apply(hostContext, FrameLayout(hostContext))
            val density = context.resources.displayMetrics.density
            val width = (size.width.value * density).toInt()
            val height = (size.height.value * density).toInt()
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
              View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, width, height)
            val texts = textViews(view)
            val status = texts.single { it.text.toString() == context.getString(
              if (state == "upcoming") R.string.widget_upcoming else R.string.widget_live) }
            assertTrue("Status must have visible width", status.width > 0)
            val names = texts.filter { it.text.toString() in listOf(fixture.team1, fixture.team2) }
            assertEquals(2, names.size)
            names.forEach {
              assertEquals(2, it.maxLines)
              val styled = it.text as android.text.Spanned
              assertTrue(styled.getSpans(0, styled.length, android.text.style.TypefaceSpan::class.java)
                .any { span -> span.family == "sans-serif-condensed" })
              assertEquals(context.getColor(R.color.widget_preview_content), it.currentTextColor)
            }
            val scores = texts.filter { it.text.toString() in listOf("1", "2", "—") }
            assertEquals(2, scores.size)
            assertEquals(if (hidden || state == "upcoming") listOf("—", "—") else listOf("1", "2"),
              scores.map { it.text.toString() })
            if (hidden) scores.forEach { assertEquals(context.getString(R.string.widget_scores_hidden), it.contentDescription) }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val output = File(context.getExternalFilesDir(null), "widget-renders").apply { mkdirs() }
            File(output, "$state-${if (dark) "dark" else "light"}-${if (small) "small" else "medium"}.png")
              .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
          }
        }
      }
    }
  }

  private fun textViews(view: View): List<TextView> = when (view) {
    is TextView -> listOf(view)
    is ViewGroup -> (0 until view.childCount).flatMap { textViews(view.getChildAt(it)) }
    else -> emptyList()
  }
}
