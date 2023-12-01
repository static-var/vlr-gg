package dev.staticvar.vlr.ui.match.details_ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.MatchInfo
import dev.staticvar.vlr.ui.Local16DPPadding
import dev.staticvar.vlr.ui.Local2DPPadding
import dev.staticvar.vlr.ui.Local8DP_4DPPadding
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.helper.EmphasisCardView
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun PreviousEncounter(
  modifier: Modifier = Modifier,
  previousEncounter: MatchInfo.Head2head,
  onClick: (String) -> Unit
) {
  CardView(modifier = modifier.clickable { onClick(previousEncounter.id) }) {
    Row(
      modifier = modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.End
    ) {
      Icon(
        Icons.AutoMirrored.Outlined.OpenInNew,
        contentDescription = stringResource(R.string.open_match_content_description),
        modifier = modifier.size(24.dp).padding(Local2DPPadding.current)
      )
    }
    Row(
      modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Text(
        text = previousEncounter.teams[0].name,
        modifier = modifier.weight(1f).padding(Local2DPPadding.current),
        textAlign = TextAlign.Center
      )
      Text(
        text = previousEncounter.teams[1].name,
        modifier = modifier.weight(1f).padding(Local2DPPadding.current),
        textAlign = TextAlign.Center
      )
    }
    Row(
      modifier = modifier.fillMaxWidth().padding(Local8DP_4DPPadding.current),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Text(
        text = previousEncounter.teams[0].score?.toString() ?: "",
        modifier = modifier.weight(1f).padding(Local2DPPadding.current),
        textAlign = TextAlign.Center
      )
      Text(
        text = previousEncounter.teams[1].score?.toString() ?: "",
        modifier = modifier.weight(1f).padding(Local2DPPadding.current),
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun PreviousMatches(
  modifier: Modifier = Modifier,
  head2head: List<MatchInfo.Head2head>,
  onClick: (String) -> Unit
) {
  var upcomingMatchToggle by rememberSaveable { mutableStateOf(false) }
  EmphasisCardView(
    modifier = modifier.clickable { upcomingMatchToggle = upcomingMatchToggle.not() }
  ) {
    Box(
      modifier = modifier.fillMaxWidth().padding(Local16DPPadding.current),
      contentAlignment = Alignment.CenterEnd
    ) {
      Text(
        text = stringResource(R.string.previous_encounter),
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.titleSmall,
        color = VLRTheme.colorScheme.primary,
      )
      Icon(
        if (upcomingMatchToggle) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
        contentDescription = stringResource(R.string.expand),
        modifier = modifier.size(16.dp),
        tint = VLRTheme.colorScheme.primary,
      )
    }
  }
  if (upcomingMatchToggle) {
    head2head.forEach {
      PreviousEncounter(
        modifier = modifier.animateContentSize(),
        previousEncounter = it,
        onClick = onClick
      )
    }
  }
}
