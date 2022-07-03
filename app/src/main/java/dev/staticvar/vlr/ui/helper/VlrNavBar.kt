package dev.staticvar.vlr.ui.helper

import androidx.compose.animation.*
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.NavState
import dev.staticvar.vlr.ui.Action
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun VlrBottomNavbar(navState: NavState, action: Action) {
  AnimatedContent(
    navState,
    transitionSpec = {
      fadeIn(animationSpec = tween(200, 200)) with
        fadeOut(animationSpec = tween(200)) using
        SizeTransform { initialSize, targetSize ->
          if (
            navState != NavState.MATCH_DETAILS &&
              navState != NavState.TOURNAMENT_DETAILS &&
              navState != NavState.TEAM_DETAILS &&
              navState != NavState.NEWS
          ) {
            keyframes {
              // Expand horizontally first.
              IntSize(targetSize.width, initialSize.height) at 150
              durationMillis = 300
            }
          } else {
            keyframes {
              // Shrink vertically first.
              IntSize(initialSize.width, targetSize.height) at 150
              durationMillis = 300
            }
          }
        }
    }
  ) { targetState ->
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
          onClick = if (navState == NavState.NEWS_OVERVIEW) EMPTY else action.goNews,
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
          onClick = if (navState == NavState.MATCH_OVERVIEW) EMPTY else action.matchOverview
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
          onClick = if (navState == NavState.TOURNAMENT) EMPTY else action.goEvents
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
