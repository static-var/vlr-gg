package dev.unusedvariable.vlr.ui.match

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding
import dev.unusedvariable.vlr.data.model.MatchDetails
import dev.unusedvariable.vlr.ui.VlrViewModel
import dev.unusedvariable.vlr.ui.common.FailScreen
import dev.unusedvariable.vlr.ui.common.LoadingScreen
import dev.unusedvariable.vlr.ui.theme.VLRTheme
import dev.unusedvariable.vlr.utils.*

@Composable
fun MatchDetails(viewModel: VlrViewModel, matchUrl: String) {
    val url = Constants.BASE_URL + matchUrl
    val caller by viewModel.forceRefreshCounter.collectAsState()

    val matchInfo by remember(caller) {
        viewModel.getMatchDetails(url)
    }.collectAsState(Waiting())

    var matchDetails: MatchDetails? by remember(viewModel) {
        mutableStateOf(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())

        matchInfo
            .onWaiting {
                matchDetails?.let {
                    MatchDetailsUi(matchDetails = it, loading) {
                        viewModel.updateCounter()
                    }
                } ?: run {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { viewModel.updateCounter() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "")
                        }
                    }
                    LoadingScreen()
                }
            }
            .onFail {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { viewModel.updateCounter() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "")
                    }
                }
                FailScreen(info = error, e = exception, viewModel = viewModel)
            }
            .onPass {
                matchDetails = dataNotNull
                MatchDetailsUi(matchDetails = dataNotNull, false) {
                    viewModel.updateCounter()
                }
            }
    }
}

@Composable
fun MatchDetailsUi(matchDetails: MatchDetails, isLoading: Boolean, refreshAction: () -> Unit) {
    val context = LocalContext.current
    val team1Painter =
        rememberImagePainter(
            data = matchDetails.team1Url,
        )
    val team2Painter =
        rememberImagePainter(
            data = matchDetails.team2Url,
        )

    // Map info
    var expandMapInfo by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(true)
    ) {

        val infiniteRepeatable = rememberInfiniteTransition()
        val animatingCircleColor by infiniteRepeatable.animateColor(
            initialValue = Color.Red,
            targetValue = VLRTheme.colors.background,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        // Loading animation
        AnimatedVisibility(visible = isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                )
            }
        }

        AnimatedVisibility(visible = !expandMapInfo) {
            Column(Modifier.fillMaxWidth()) {
                // Date +  time + isLive
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LIVE
                    if (matchDetails.live) {
                        Canvas(modifier = Modifier
                            .size(24.dp)
                            .padding(8.dp), onDraw = {
                            drawCircle(animatingCircleColor)
                        })
                    }
                    Text(
                        text = matchDetails.date + " | " + matchDetails.time,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = VLRTheme.typography.body1
                    )
                    IconButton(onClick = refreshAction) {
                        Icon(Icons.Default.Refresh, contentDescription = "")
                    }
                }

                // ETA
                matchDetails.eta?.let { eta ->
                    Text(
                        text = eta, textAlign = TextAlign.Center, modifier = Modifier
                            .fillMaxWidth(), style = VLRTheme.typography.body2
                    )
                }
            }
        }

        val animateVisibility by animateFloatAsState(
            if (expandMapInfo) 0f else 0.3f, animationSpec = spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        val animateSize by animateDpAsState(
            if (expandMapInfo) 120.dp else 220.dp, animationSpec = spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        // Team name, Image and Score
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animateSize)
                .padding(16.dp)
                .animateContentSize()
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .alpha(animateVisibility),
                verticalAlignment = Alignment.CenterVertically
            ) { // Team logos
                Image(
                    painter = team1Painter,
                    modifier = Modifier.width(LocalContext.current.resources.configuration.screenWidthDp.dp / 2),
                    contentDescription = "",
                    contentScale = ContentScale.Crop
                )
                Image(
                    painter = team2Painter,
                    modifier = Modifier.width(LocalContext.current.resources.configuration.screenWidthDp.dp / 2),
                    contentDescription = "",
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) { // Team Names and score
                Text(
                    text = matchDetails.team1,
                    modifier = Modifier.width(LocalContext.current.resources.configuration.screenWidthDp.dp / 3),
                    style = VLRTheme.typography.h5,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = matchDetails.scoreLine,
                    modifier = Modifier.width(LocalContext.current.resources.configuration.screenWidthDp.dp / 3),
                    style = VLRTheme.typography.h5,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = matchDetails.team2,
                    modifier = Modifier.width(LocalContext.current.resources.configuration.screenWidthDp.dp / 3),
                    style = VLRTheme.typography.h5,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Picks and bans
        AnimatedVisibility(
            visible = !expandMapInfo,
        ) {
            Column(Modifier.fillMaxWidth()) {
                // Patch info
                matchDetails.patchInfo?.let { patchInfo ->
                    Text(
                        text = "$patchInfo${if (matchDetails.matchNotes.isNotBlank()) " | ${matchDetails.matchNotes}" else ""}",
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth(),
                        style = VLRTheme.typography.body2
                    )
                }

                // Pick and bans
                matchDetails.pickAndBans?.let { pickAndBans ->
                    Text(
                        text = pickAndBans.split(";").joinToString(separator = " |") { it },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 30.dp),
                        style = VLRTheme.typography.body2
                    )
                }
                var expandCard by remember {
                    mutableStateOf(false)
                }

                // Streams and VODs
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .animateContentSize()
                        .clickable { expandCard = expandCard.not() },
                    shape = VLRTheme.shapes.large,
                    elevation = 16.dp
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (expandCard) "Show less" else "VODs and Streams",
                                modifier = Modifier
                                    .padding(8.dp),
                                style = VLRTheme.typography.body1
                            )
                            Icon(
                                if (expandCard) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = ""
                            )
                        }
                        AnimatedVisibility(visible = expandCard) {
                            Column(modifier = Modifier.fillMaxWidth()) {

                                matchDetails.vod?.let { vodInfo ->
                                    Text(
                                        text = "VODs",
                                        Modifier.padding(horizontal = 16.dp),
                                        style = VLRTheme.typography.body2
                                    )
                                    Button(
                                        onClick = {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(vodInfo.second)
                                                )
                                            )
                                        }, modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(text = vodInfo.first)
                                    }
                                }
                                matchDetails.streams?.takeUnless { it.isNullOrEmpty() }
                                    ?.let { streams ->
                                        Text(
                                            text = "Streams",
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp),
                                            style = VLRTheme.typography.body2
                                        )
                                        LazyColumn(Modifier.fillMaxWidth()) {
                                            items(streams) { streamInfo ->
                                                Button(
                                                    onClick = {
                                                        context.startActivity(
                                                            Intent(
                                                                Intent.ACTION_VIEW,
                                                                Uri.parse(streamInfo.second)
                                                            )
                                                        )
                                                    }, modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp)
                                                ) {
                                                    Text(text = streamInfo.first)
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }
            }
        }



        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(),
            shape = VLRTheme.shapes.large,
            elevation = 16.dp
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { expandMapInfo = expandMapInfo.not() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expandMapInfo) "Show less" else "Maps",
                        modifier = Modifier
                            .padding(8.dp),
                        style = VLRTheme.typography.body1
                    )
                    Icon(
                        if (expandMapInfo) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = ""
                    )
                }
                AnimatedVisibility(visible = expandMapInfo) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        var tabPosition by remember { mutableStateOf(0) }
                        matchDetails.mapInfo?.takeIf { it.isNotEmpty() }?.let { maps ->
                            TabRow(selectedTabIndex = tabPosition) {
                                maps.forEachIndexed { index, pair ->
                                    Tab(
                                        selected = tabPosition == index,
                                        onClick = { tabPosition = index },
                                    ) {
                                        Text(
                                            text = pair.first,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                            matchDetails.mapData?.takeIf { tabPosition < it.size }?.get(tabPosition)
                                ?.let { mapData ->
                                    Column(Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.5f)
                                                    .padding(end = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = mapData.team1)
                                                Text(text = mapData.team1Score)
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth(1f)
                                                    .padding(start = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = mapData.team2Score)
                                                Text(
                                                    text = mapData.team2,
                                                    textAlign = TextAlign.End
                                                )
                                            }
                                        }

                                        if (mapData.isMapComplete) { // show stats
                                            val allPlayers = mapData.team1Players?.plus(
                                                mapData.team2Players ?: emptyList()
                                            )
                                            allPlayers?.takeIf { it.isNotEmpty() }?.let { players ->
                                                val playerGroup = players.groupBy { it.org }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceEvenly
                                                ) {
                                                    Text(
                                                        text = "Agent",
                                                        modifier = Modifier
                                                            .padding(2.dp)
                                                            .fillMaxWidth(0.17f)
                                                    )
                                                    Text(
                                                        text = "Name",
                                                        modifier = Modifier
                                                            .padding(2.dp)
                                                            .fillMaxWidth(0.20f)
                                                    )
                                                    Text(
                                                        text = "ACS",
                                                        modifier = Modifier
                                                            .padding(2.dp)
                                                            .fillMaxWidth(0.17f)
                                                    )
                                                    Text(
                                                        text = "K",
                                                        modifier = Modifier
                                                            .padding(2.dp)
                                                            .fillMaxWidth(0.15f)
                                                    )
                                                    Text(
                                                        text = "D",
                                                        modifier = Modifier
                                                            .padding(2.dp)
                                                            .fillMaxWidth(0.18f)
                                                    )
                                                    Text(
                                                        text = "A",
                                                        modifier = Modifier
                                                            .padding(2.dp)
                                                            .fillMaxWidth(0.25f)
                                                    )
                                                    Text(
                                                        text = "ADR",
                                                        modifier = Modifier
                                                            .padding(2.dp)
                                                            .fillMaxWidth(0.50f)
                                                    )
                                                    Text(
                                                        text = "HS%",
                                                        modifier = Modifier
                                                            .padding(2.dp)
                                                            .fillMaxWidth()
                                                    )
                                                }
                                                LazyColumn(Modifier.fillMaxWidth()) {
                                                    playerGroup.forEach { (org, agents) ->
                                                        stickyHeader {
                                                            Text(
                                                                text = org,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .background(
                                                                        color = VLRTheme.colors.background.copy(
                                                                            0.7f
                                                                        )
                                                                    ),
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }

                                                        items(agents) { agent ->
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceEvenly
                                                            ) {
                                                                Image(
                                                                    painter = rememberImagePainter(
                                                                        data = agent.agent
                                                                    ),
                                                                    contentDescription = "",
                                                                    modifier = Modifier
                                                                        .padding(2.dp)
                                                                        .fillMaxWidth(0.17f),
                                                                    contentScale = ContentScale.FillHeight,
                                                                    alignment = Alignment.Center
                                                                )
                                                                Text(
                                                                    text = agent.name,
                                                                    modifier = Modifier
                                                                        .padding(2.dp)
                                                                        .fillMaxWidth(0.20f)
                                                                )
                                                                Text(
                                                                    text = agent.combatStats.acs,
                                                                    modifier = Modifier
                                                                        .padding(2.dp)
                                                                        .fillMaxWidth(0.17f)
                                                                )
                                                                Text(
                                                                    text = agent.combatStats.kills,
                                                                    modifier = Modifier
                                                                        .padding(2.dp)
                                                                        .fillMaxWidth(0.15f)
                                                                )
                                                                Text(
                                                                    text = agent.combatStats.deaths,
                                                                    modifier = Modifier
                                                                        .padding(2.dp)
                                                                        .fillMaxWidth(0.18f)
                                                                )
                                                                Text(
                                                                    text = agent.combatStats.assists,
                                                                    modifier = Modifier
                                                                        .padding(2.dp)
                                                                        .fillMaxWidth(0.25f)
                                                                )
                                                                Text(
                                                                    text = agent.combatStats.adr,
                                                                    modifier = Modifier
                                                                        .padding(2.dp)
                                                                        .fillMaxWidth(0.50f)
                                                                )
                                                                Text(
                                                                    text = agent.combatStats.headShot,
                                                                    modifier = Modifier
                                                                        .padding(2.dp)
                                                                        .fillMaxWidth()
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = "Match is going on, wait to see the stats",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                textAlign = TextAlign.Center,
                                                style = VLRTheme.typography.body1
                                            )
                                        }
                                    }
                                } ?: Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "No info about the match")
                            }
                        }
                    }
                }
            }
        }
    }
}