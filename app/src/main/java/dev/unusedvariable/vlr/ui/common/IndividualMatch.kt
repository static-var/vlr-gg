package dev.unusedvariable.vlr.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.ajalt.timberkt.e
import dev.unusedvariable.vlr.data.NavState
import dev.unusedvariable.vlr.data.model.CompletedMatch
import dev.unusedvariable.vlr.data.model.MatchData
import dev.unusedvariable.vlr.data.model.UpcomingMatch
import dev.unusedvariable.vlr.ui.Action
import dev.unusedvariable.vlr.ui.VlrViewModel
import dev.unusedvariable.vlr.ui.theme.VLRTheme
import java.net.SocketTimeoutException

@Composable
fun SuccessScreen(data: List<MatchData>, upcomingMatches: Boolean, action: Action) {
//    currentComposer.collectParameterInformation()
//    currentComposer.compositionData.compositionGroups.forEach {
//        it.data.forEach { e { "$it" } }
//        e { "Key ${it.key}" }
//        e { "node ${it.node}" }
//        e { "info ${it.sourceInfo}" }
//    }

    LazyColumn(modifier = Modifier
        .fillMaxSize()
        ) {

        val map = data.groupBy { it.date }
        map.forEach { (date, matchData) ->
            stickyHeader {
                Text(
                    text = date,
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(color = VLRTheme.colors.background.copy(0.7f)),
                    style = VLRTheme.typography.body1,
                )
            }

            items(matchData) { matchItem ->
                MatchUi(match = matchItem, upcomingMatch = upcomingMatches, action = action)
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            text = "Getting data...",
            style = VLRTheme.typography.h4,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun FailScreen(
    info: String,
    e: Exception,
    viewModel: VlrViewModel,
    showReload: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Unable to parse webpage, try again!",
            style = VLRTheme.typography.h4,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )

        val navState by remember {
            viewModel.navState
        }.collectAsState()

        when (navState) {
            NavState.MATCH_DETAILS -> {
            }
            else -> {
                if (e is SocketTimeoutException || showReload) {
                    Button(
                        onClick = when (viewModel.navState.value) {
                            NavState.UPCOMING -> {
                                viewModel.clearCache()
                                viewModel.action.goUpcoming
                            }
                            else -> {
                                viewModel.clearCache()
                                viewModel.action.goResults
                            }
                        }
                    ) {
                        Text(text = "Reload")
                    }
                }
            }
        }
    }
}

@Composable
fun MatchUi(match: MatchData, upcomingMatch: Boolean = true, action: Action) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                if (match is UpcomingMatch)
                    action.match(match.upcomingId)
                if (match is CompletedMatch)
                    action.match(match.completedId)
            },
        shape = VLRTheme.shapes.large,
        elevation = 12.dp,
    ) {
        Column(modifier = Modifier.padding(8.dp)) { // Main UI

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) { // Live Row
                if (match.isLive)
                    Text(
                        text = "LIVE",
                        modifier = Modifier
                            .background(Color.Red)
                            .border(1.dp, Color.Red)
                            .padding(horizontal = 4.dp),
                        style = VLRTheme.typography.body2,
                        color = Color.White
                    )
                else {
                    Text(
                        text = if (upcomingMatch) "in ${match.eta}" else "was ${match.eta} ago",
                        style = VLRTheme.typography.caption,
                    )

                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) { // Team names
                Text(
                    text = match.team1,
                    style = VLRTheme.typography.h6,
                )
                Text(
                    text = match.team1Score,
                    style = VLRTheme.typography.h6,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) { // Team names
                Text(
                    text = match.team2,
                    style = VLRTheme.typography.h6,
                )
                Text(
                    text = match.team2Score,
                    style = VLRTheme.typography.h6,
                )
            }

            Text(
                text = match.gameExtraInfo,
                style = VLRTheme.typography.caption,
                color = VLRTheme.colors.onBackground.copy(0.6f),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}