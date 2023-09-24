package dev.staticvar.vlr.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.staticvar.vlr.MainActivity
import dev.staticvar.vlr.data.VlrRepository
import dev.staticvar.vlr.data.api.response.MatchPreviewInfo
import dev.staticvar.vlr.ui.Destination
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.ui.theme.WidgetTheme
import dev.staticvar.vlr.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScoreWidget : GlanceAppWidget() {

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface ExampleContentProviderEntryPoint {
    fun vlrRepository(): VlrRepository
  }

  @Composable
  fun Content(vlrRepository: VlrRepository) {
    var list by remember { mutableStateOf(listOf<MatchPreviewInfo>()) }
    val coroutine = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
      coroutine.launch(Dispatchers.IO) { list = vlrRepository.upcomingMatches().subList(0, 10) }
    }

    WidgetTheme(context = context, darkTheme = context.isDarkThemeOn()) {
      if (list.isEmpty())
        WidgetUnableToUpdateUi()
      else {
        LazyColumn(
          GlanceModifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .cornerRadius(16.dp)
        ) {
          headerText()
          items(list) {
            Column(
              modifier =
                GlanceModifier.fillMaxWidth()
                  .cornerRadius(16.dp)
                  .padding(8.dp)
                  .clickable(
                    actionStartActivity(
                      Intent(
                        Intent.ACTION_VIEW,
                        "${Constants.DEEP_LINK_BASEURL}${Destination.Match.Args.ID}=${it.id}".toUri(),
                        context,
                        MainActivity::class.java
                      )
                    )
                  ),
            ) {
              Column(
                modifier =
                  GlanceModifier.fillMaxWidth()
                    .cornerRadius(16.dp)
                    .padding(4.dp)
                    .background(VLRTheme.colorScheme.secondaryContainer),
              ) {
                WidgetTimeRow(status = it.status, time = it.time)
                WidgetTeamUiRow(teamNameA = it.team1.name, teamNameB = it.team2.name)
                WidgetScoreUiRow(teamScoreA = it.team1.score, teamScoreB = it.team2.score)
              }
            }
          }
        }
      }
    }
  }

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val appContext = context.applicationContext
    checkNotNull(appContext)
    val hiltEntryPoint =
      EntryPointAccessors.fromApplication(appContext, ExampleContentProviderEntryPoint::class.java)

    provideContent { Content(hiltEntryPoint.vlrRepository()) }
  }
}

fun Context.isDarkThemeOn(): Boolean {
  return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
}