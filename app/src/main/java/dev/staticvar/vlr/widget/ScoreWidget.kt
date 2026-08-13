package dev.staticvar.vlr.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.glance.currentState
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
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
import dev.staticvar.vlr.utils.widgetLastUpdateMillisKey
import dev.staticvar.vlr.utils.onFail
import dev.staticvar.vlr.utils.onPass
import dev.staticvar.vlr.utils.onWaiting

class ScoreWidget : GlanceAppWidget() {
  override val sizeMode: SizeMode = SizeMode.Exact

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface ExampleContentProviderEntryPoint {
    fun vlrRepository(): VlrRepository
  }

  @Composable
  private fun Content(vlrRepository: VlrRepository) {
    val lastUpdatedAt = currentState(widgetLastUpdateMillisKey)
    val resultList by
      vlrRepository.getMatchesFromDb().collectAsState(initial = Waiting<List<MatchPreviewInfo>>())

    WidgetTheme {
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
                GlanceModifier.padding(8.dp)
                  .cornerRadius(100.dp)
                  .clickable(actionRunCallback<RefreshWidgetAction>()),
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
              MatchList(list = matchList, lastUpdatedAt = lastUpdatedAt)
            } ?: run { WidgetUnableToUpdateUi() }
          }
      }
    }
  }

  @Composable
  private fun MatchList(
    modifier: GlanceModifier = GlanceModifier,
    list: List<MatchPreviewInfo>,
    lastUpdatedAt: Long?,
  ) {
    val context = LocalContext.current
    LazyColumn(modifier = modifier.cornerRadius(16.dp).fillMaxWidth()) {
      headerText(lastUpdatedAt)
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
  private fun WaitingUi(modifier: GlanceModifier = GlanceModifier) {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(
        text = "Updating...",
        style =
          TextStyle(
            textAlign = TextAlign.Center,
            color = GlanceTheme.colors.onPrimaryContainer,
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
