package dev.staticvar.vlr.ui.helper

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Feed
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.NavState
import dev.staticvar.vlr.ui.Action
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.navAnimation

@Composable
fun VlrBottomNavbar(navState: NavState, action: Action, viewModel: VlrViewModel) {
  val resetScroll = { viewModel.resetScroll() }
  AnimatedContent(navState, transitionSpec = { navAnimation(navState) }) { targetState ->
    if (
      targetState != NavState.MATCH_DETAILS &&
        targetState != NavState.TOURNAMENT_DETAILS &&
        targetState != NavState.TEAM_DETAILS &&
        targetState != NavState.NEWS
    ) {
      VLRTheme.colorScheme.surface
      NavigationBar(
        containerColor = VLRTheme.colorScheme.surfaceColorAtElevation(8.dp),
        contentColor = contentColorFor(VLRTheme.colorScheme.onSurface),
        tonalElevation = 8.dp,
        modifier = Modifier.navigationBarsPadding()
      ) {
        NavigationBarItem(
          selected = navState == NavState.NEWS_OVERVIEW,
          icon = {
            Icon(
              imageVector =
                if (navState == NavState.NEWS_OVERVIEW) Icons.Filled.Feed else Icons.Outlined.Feed,
              contentDescription = stringResource(R.string.news),
              tint = VLRTheme.colorScheme.onPrimaryContainer
            )
          },
          label = { Text(text = stringResource(R.string.news)) },
          onClick = if (navState == NavState.NEWS_OVERVIEW) resetScroll else action.goNews,
        )
        NavigationBarItem(
          selected = navState == NavState.MATCH_OVERVIEW,
          icon = {
            Icon(
              imageVector =
                if (navState == NavState.MATCH_OVERVIEW) Icons.Filled.SportsEsports
                else Icons.Outlined.SportsEsports,
              contentDescription = stringResource(R.string.games),
              tint = VLRTheme.colorScheme.onPrimaryContainer
            )
          },
          label = { Text(text = stringResource(R.string.matches)) },
          onClick = if (navState == NavState.MATCH_OVERVIEW) resetScroll else action.matchOverview
        )
        NavigationBarItem(
          selected = navState == NavState.TOURNAMENT,
          icon = {
            Icon(
              imageVector =
                if (navState == NavState.TOURNAMENT) Icons.Filled.EmojiEvents
                else Icons.Outlined.EmojiEvents,
              contentDescription = stringResource(R.string.tournament),
              tint = VLRTheme.colorScheme.onPrimaryContainer
            )
          },
          label = { Text(text = stringResource(R.string.events)) },
          onClick = if (navState == NavState.TOURNAMENT) resetScroll else action.goEvents
        )
        NavigationBarItem(
          selected = navState == NavState.ABOUT,
          icon = {
            Icon(
              imageVector =
                if (navState == NavState.ABOUT) Icons.Filled.Info else Icons.Outlined.Info,
              contentDescription = stringResource(R.string.about),
              tint = VLRTheme.colorScheme.onPrimaryContainer
            )
          },
          label = { Text(text = stringResource(R.string.about)) },
          onClick = if (navState == NavState.ABOUT) EMPTY else action.goAbout
        )
      }
    }
  }
}

val EMPTY = {}
