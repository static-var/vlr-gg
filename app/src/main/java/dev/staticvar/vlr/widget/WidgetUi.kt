package dev.staticvar.vlr.widget

import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyListScope
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.staticvar.vlr.MainActivity
import dev.staticvar.vlr.ui.Destination
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.Constants
import dev.staticvar.vlr.utils.readableDateAndTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun WidgetTimeRow(modifier: GlanceModifier = GlanceModifier, status: String, time: String?) {
  Row(modifier = modifier.fillMaxWidth()) {
    Text(
      text =
      if (status.equals("LIVE", true)) "LIVE"
      else time?.readableDateAndTime ?: "",
      modifier = GlanceModifier.fillMaxWidth(),
      style =
      TextStyle(
        textAlign = TextAlign.Center,
        color = ColorProvider(MaterialTheme.colorScheme.onSecondaryContainer),
        fontSize = 12.sp
      )
    )
  }
}

@Composable
fun WidgetTeamUiRow(
  modifier: GlanceModifier = GlanceModifier,
  teamNameA: String,
  teamNameB: String,
) {
  Row(
    modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Vertical.CenterVertically,
    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
  ) {
    Text(
      text = teamNameA,
      style =
      TextStyle(
        ColorProvider(MaterialTheme.colorScheme.onSecondaryContainer),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold
      ),
      modifier = GlanceModifier.defaultWeight().padding(2.dp),
      maxLines = 1
    )
    Text(
      text = teamNameB,
      style =
      TextStyle(
        ColorProvider(MaterialTheme.colorScheme.onSecondaryContainer),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold
      ),
      modifier = GlanceModifier.defaultWeight().padding(2.dp)
    )
  }
}

@Composable
fun WidgetScoreUiRow(
  modifier: GlanceModifier = GlanceModifier,
  teamScoreA: Int?,
  teamScoreB: Int?,
) {
  Row(
    modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Vertical.CenterVertically,
    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
  ) {
    Text(
      text = teamScoreA?.toString() ?: "-",
      style =
      TextStyle(
        ColorProvider(MaterialTheme.colorScheme.onSecondaryContainer),
        textAlign = TextAlign.Center
      ),
      modifier = GlanceModifier.defaultWeight().padding(2.dp)
    )
    Text(
      text = teamScoreB?.toString() ?: "-",
      style =
      TextStyle(
        ColorProvider(MaterialTheme.colorScheme.onSecondaryContainer),
        textAlign = TextAlign.Center
      ),
      modifier = GlanceModifier.defaultWeight().padding(2.dp),
      maxLines = 1
    )
  }
}

@Composable
fun WidgetUnableToUpdateUi(modifier: GlanceModifier = GlanceModifier) {
  val context = LocalContext.current
  Column(
    modifier = modifier.fillMaxSize()
      .padding(8.dp)
      .background(MaterialTheme.colorScheme.primaryContainer)
      .cornerRadius(16.dp)
      .clickable(
        actionStartActivity(
          Intent(
            Intent.ACTION_VIEW,
            "${Constants.DEEP_LINK_BASEURL}${Destination.MatchOverview}".toUri(),
            context,
            MainActivity::class.java
          )
        )
      ),
    verticalAlignment = Alignment.Vertical.CenterVertically,
    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
  ) {
    Text(
      text = "Unable to find matches, open the app to fetch data.",
      style =
      TextStyle(
        color = ColorProvider(VLRTheme.colorScheme.onPrimaryContainer),
        textAlign = TextAlign.Center
      ),
    )
  }
}

fun LazyListScope.headerText() {
  item {
    Text(
      text = "Matches",
      style =
      TextStyle(
        textAlign = TextAlign.Center,
        color = ColorProvider(MaterialTheme.colorScheme.onPrimaryContainer),
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
      ),
      modifier = GlanceModifier.fillMaxWidth().padding(8.dp)
    )
  }
  item {
    Text(
      text =
      "Last updated at ${
        LocalTime
          .now()
          .atOffset(ZoneOffset.UTC)
          .format(DateTimeFormatter.ofPattern("HH:mm a"))
      }",
      style =
      TextStyle(
        textAlign = TextAlign.Center,
        color = ColorProvider(MaterialTheme.colorScheme.onPrimaryContainer),
        fontSize = 12.sp
      ),
      modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp)
    )
  }
}