package dev.unusedvariable.vlr.widget

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.github.ajalt.timberkt.e
import dev.unusedvariable.vlr.data.VlrRepository

class ScoreWidget(private val repository: VlrRepository) : GlanceAppWidget() {

    @Composable
    override fun Content() {
        e {"Re-draw"}

        var list by remember {
            mutableStateOf(repository.getFiveUpcomingMatches())
        }

        if (list.isEmpty())
            Column(modifier = GlanceModifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(0.2f)).cornerRadius(16.dp)) {
                Text(text = "Unable to find matches, open the app to fetch data.")
            }
        else {
            Column(
                modifier = GlanceModifier.padding(8.dp).fillMaxSize().background(MaterialTheme.colorScheme.primary).cornerRadius(16.dp)
            ) {
                LazyColumn(GlanceModifier.fillMaxWidth()) {
                    items(list) {

                        Column(
                            modifier = GlanceModifier.fillMaxWidth().absolutePadding(top = 8.dp, bottom = 4.dp).cornerRadius(16.dp),
                        ) {

                            Row(
                                modifier = GlanceModifier.padding(4.dp).fillMaxWidth()
                            ) {
                                Text(
                                    text = if (it.isLive) "LIVE" else it.date,
                                    modifier = GlanceModifier.fillMaxWidth(),
                                    style = TextStyle(
                                        textAlign = TextAlign.End,
                                        color = ColorProvider(MaterialTheme.colorScheme.onPrimary),
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            Row(
                                GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Vertical.CenterVertically,
                                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                            ) {
                                Text(
                                    text = it.team1,
                                    style = TextStyle(
                                        ColorProvider(MaterialTheme.colorScheme.onPrimary),
                                        textAlign = TextAlign.Start,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = GlanceModifier.defaultWeight().padding(2.dp),
                                    maxLines = 1
                                )
                                Text(
                                    text = it.team1Score,
                                    style = TextStyle(
                                        ColorProvider(MaterialTheme.colorScheme.onPrimary),
                                        textAlign = TextAlign.End
                                    ),
                                    modifier = GlanceModifier.padding(2.dp)
                                )
                                Text(
                                    text = it.team2Score,
                                    style = TextStyle(
                                        ColorProvider(MaterialTheme.colorScheme.onPrimary),
                                        textAlign = TextAlign.Start
                                    ),
                                    modifier = GlanceModifier.padding(2.dp)
                                )
                                Text(
                                    text = it.team2,
                                    style = TextStyle(
                                        ColorProvider(MaterialTheme.colorScheme.onPrimary),
                                        textAlign = TextAlign.End,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = GlanceModifier.defaultWeight().padding(2.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}