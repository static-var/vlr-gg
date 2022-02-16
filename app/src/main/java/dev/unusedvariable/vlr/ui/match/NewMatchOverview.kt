package dev.unusedvariable.vlr.ui.match

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.statusBarsPadding
import dev.unusedvariable.vlr.data.api.response.MatchPreviewInfo
import dev.unusedvariable.vlr.ui.CARD_ALPHA
import dev.unusedvariable.vlr.ui.VlrViewModel
import dev.unusedvariable.vlr.ui.theme.VLRTheme
import dev.unusedvariable.vlr.utils.Waiting
import dev.unusedvariable.vlr.utils.onFail
import dev.unusedvariable.vlr.utils.onPass
import dev.unusedvariable.vlr.utils.onWaiting

@Composable
fun MatchOverview(viewModel: VlrViewModel) {

    val allMatches by remember(viewModel) {
        viewModel.getAllMatches()
    }.collectAsState(initial = Waiting())

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
        )

        allMatches.onPass {
            data?.let { list ->
                MatchOverviewContainer(viewModel = viewModel, list = list)
            }
        }.onWaiting {
            LinearProgressIndicator()
        }.onFail {
            Text(text = message())
        }
    }
}

@Composable
fun MatchOverviewContainer(viewModel: VlrViewModel, list: List<MatchPreviewInfo>) {
    var tabPosition by remember(viewModel) { mutableStateOf(0) }
    val (ongoing, upcoming, completed) = list.groupBy { it.status.startsWith("LIVE", ignoreCase = true) }.let {
        Triple(
            it[true].orEmpty(),
            it[false]?.groupBy { it.status.startsWith("Upcoming", ignoreCase = true) }?.get(true).orEmpty(),
            it[false]?.groupBy { it.status.startsWith("upcoming", ignoreCase = true) }?.get(false).orEmpty()
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabPosition, backgroundColor = VLRTheme.colorScheme.primaryContainer) {
            Tab(selected = tabPosition == 0, onClick = { tabPosition = 0 }) {
                Text(text = "Ongoing", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = tabPosition == 1, onClick = { tabPosition = 1 }) {
                Text(text = "Upcoming", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = tabPosition == 2, onClick = { tabPosition = 2 }) {
                Text(text = "Completed", modifier = Modifier.padding(16.dp))
            }
        }
        when (tabPosition) {
            0 -> {
                if (ongoing.isEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "No ongoing events")
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn() {
                        items(ongoing) {
                            MatchOverviewPreview(matchPreviewInfo = it)
                        }
                    }
                }
            }

            1 -> {
                if (upcoming.isEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "No ongoing events")
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn() {
                        items(upcoming) {
                            MatchOverviewPreview(matchPreviewInfo = it)
                        }
                    }
                }
            }
            else -> {
                if (completed.isEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "No ongoing events")
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn() {
                        items(completed) {
                            MatchOverviewPreview(matchPreviewInfo = it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchOverviewPreview(matchPreviewInfo: MatchPreviewInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(CARD_ALPHA)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = matchPreviewInfo.status,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = VLRTheme.typography.displaySmall
            )
            Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = matchPreviewInfo.team1.name,
                    style = VLRTheme.typography.titleSmall,
                    modifier = Modifier
                        .weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = matchPreviewInfo.team1.score,
                    style = VLRTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = matchPreviewInfo.team2.name,
                    style = VLRTheme.typography.titleSmall,
                    modifier = Modifier
                        .weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = matchPreviewInfo.team2.score,
                    style = VLRTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = matchPreviewInfo.event.name + " - " + matchPreviewInfo.event.stage,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = VLRTheme.typography.labelSmall
            )
        }
    }
}