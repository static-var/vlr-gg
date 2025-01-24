package dev.staticvar.vlr.ui.match.details_ui

import android.Manifest
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.MatchInfo
import dev.staticvar.vlr.ui.Local8DPPadding
import dev.staticvar.vlr.ui.helper.EmphasisCardView
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.Constants
import dev.staticvar.vlr.utils.hasElapsed
import dev.staticvar.vlr.utils.openAsCustomTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MatchInfoMoreOptions(
  detailData: MatchInfo,
  isTracked: Boolean,
  eventId: String,
  onSubButton: suspend () -> Unit,
  onEventClick: (String) -> Unit,
) {

  val scope = rememberCoroutineScope()
  val notificationPermission =
    rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)

  EmphasisCardView(
    Modifier.animateContentSize(tween(500)),
  ) {

    Text(
      text = stringResource(R.string.more_info),
      style = VLRTheme.typography.titleSmall,
      color = VLRTheme.colorScheme.primary,
      textAlign = TextAlign.Center,
      modifier = Modifier
        .padding(Local8DPPadding.current)
        .fillMaxWidth()
    )

    var dialogOpen by remember { mutableStateOf(false) }
    MatchMoreDetailsDialog(
      detailData = detailData,
      open = dialogOpen,
      onDismiss = { dialogOpen = it }
    )

    Row(
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = { dialogOpen = true },
        modifier = Modifier
          .weight(1f)
          .testTag("details:more_info"),
        shape = VLRTheme.shapes.small
      ) {
        Text(text = stringResource(R.string.details))
      }
      detailData.event.date?.let {
        if (!(detailData.fromEventsFav || detailData.fromTeamsFav))
        if (!it.hasElapsed) {
          var processingTopicSubscription by remember { mutableStateOf(false) }
          Button(
            onClick = {
              if (notificationPermission.status.isGranted) {
                if (!processingTopicSubscription) {
                  processingTopicSubscription = true
                  scope.launch(Dispatchers.IO) {
                    onSubButton()
                    processingTopicSubscription = false
                  }
                }
              } else notificationPermission.launchPermissionRequest()
            },
            modifier = Modifier.weight(1f),
            shape = VLRTheme.shapes.small
          ) {
            if (processingTopicSubscription) {
              LinearProgressIndicator()
            } else if (isTracked) Text(text = stringResource(R.string.unsubscribe))
            else Text(text = stringResource(R.string.get_notified))
          }
        }
      }
    }

    Row(
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      val context = LocalContext.current
      Button(
        onClick = { (Constants.VLR_BASE + detailData.id).openAsCustomTab(context) },
        modifier = Modifier.weight(1f),
        shape = VLRTheme.shapes.small
      ) {
        Text(text = stringResource(id = R.string.view_at_vlr), maxLines = 1)
      }

      Button(
        onClick = { onEventClick(eventId) },
        modifier = Modifier.weight(1f),
        shape = VLRTheme.shapes.small
      ) {
        Text(text = stringResource(id = R.string.check_event))
      }
    }
    Spacer(modifier = Modifier.padding(4.dp))
  }
}