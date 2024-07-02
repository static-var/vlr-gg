package dev.staticvar.vlr.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOr
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.staticvar.vlr.MainActivity
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.VlrRepository
import dev.staticvar.vlr.data.api.response.MatchPreviewInfo
import dev.staticvar.vlr.ui.Destination
import dev.staticvar.vlr.ui.theme.WidgetTheme
import dev.staticvar.vlr.utils.Constants
import dev.staticvar.vlr.utils.Waiting
import dev.staticvar.vlr.utils.onFail
import dev.staticvar.vlr.utils.onPass
import dev.staticvar.vlr.utils.onWaiting
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
    val coroutine = rememberCoroutineScope()
    val context = LocalContext.current
    val state by vlrRepository.updateLatestMatches().collectAsState(initial = Ok(false))
    val resultList by
      vlrRepository.getMatchesFromDb().collectAsState(initial = Waiting<List<MatchPreviewInfo>>())

    WidgetTheme(context = context, darkTheme = context.isDarkThemeOn()) {
      Scaffold(
        modifier = GlanceModifier.appWidgetBackground().fillMaxWidth(),
        backgroundColor = GlanceTheme.colors.widgetBackground,
        titleBar = {
          TitleBar(
            title = "Matches",
            modifier = GlanceModifier.fillMaxWidth(),
            startIcon = ImageProvider(resId = R.drawable.ic_launcher_foreground),
          ) {
            Image(
              modifier =
                GlanceModifier.padding(8.dp).cornerRadius(100.dp).clickable {
                  if (state.get() != true) coroutine.launch(Dispatchers.IO) {}
                },
              provider = ImageProvider(resId = R.drawable.rounded_refresh),
              colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
              contentDescription = "Refresh",
            )
          }
        },
      ) {
        resultList
          .onFail { WidgetUnableToUpdateUi() }
          .onWaiting { WaitingUi() }
          .onPass {
            data?.let {
              val matchList = it.filterNot { matches ->
                matches.status == "completed"
              }
              MatchList(list = matchList, isUpdating = state.getOr(false))
            } ?: run { WidgetUnableToUpdateUi() }
          }
      }
    }
  }

  @Composable
  fun MatchList(
    modifier: GlanceModifier = GlanceModifier,
    list: List<MatchPreviewInfo>,
    isUpdating: Boolean = false,
  ) {
    val context = LocalContext.current
    LazyColumn(modifier = modifier.cornerRadius(16.dp).fillMaxWidth()) {
      headerText(isUpdating = isUpdating)
      items(list) {
        Column(
          modifier =
            GlanceModifier.fillMaxWidth()
              .cornerRadius(16.dp)
              .padding(vertical = 4.dp)
              .clickable(
                actionStartActivity(
                  Intent(
                    Intent.ACTION_VIEW,
                    "${Constants.DEEP_LINK_BASEURL}${Destination.Match.Args.ID}=${it.id}".toUri(),
                    context,
                    MainActivity::class.java,
                  )
                )
              )
        ) {
          Column(
            modifier =
              GlanceModifier.fillMaxWidth()
                .cornerRadius(16.dp)
                .padding(4.dp)
                .background(GlanceTheme.colors.primaryContainer)
          ) {
            WidgetTimeRow(status = it.status, time = it.time)
            WidgetTeamUiRow(teamNameA = it.team1.name, teamNameB = it.team2.name)
            WidgetScoreUiRow(teamScoreA = it.team1.score, teamScoreB = it.team2.score)
          }
        }
      }
    }
  }

  @Composable
  fun WaitingUi(modifier: GlanceModifier = GlanceModifier) {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(
        text = "Updating...",
        style =
          TextStyle(
            textAlign = TextAlign.Center,
            color = ColorProvider(MaterialTheme.colorScheme.onPrimaryContainer),
            fontSize = 12.sp,
          ),
        modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp),
      )
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
