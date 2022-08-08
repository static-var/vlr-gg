package dev.staticvar.vlr.ui.match.details_ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.MatchInfo
import dev.staticvar.vlr.ui.Local4DPPadding
import dev.staticvar.vlr.ui.Local8DPPadding
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.readableDateAndTimeWithZone

@Composable
fun MatchMoreDetailsDialog(
  modifier: Modifier = Modifier,
  detailData: MatchInfo,
  open: Boolean,
  onDismiss: (Boolean) -> Unit
) {
  if (open)
    AlertDialog(
      onDismissRequest = { onDismiss(false) },
      title = {
        Text(
          text = stringResource(R.string.event_info),
          modifier = modifier.padding(Local8DPPadding.current),
          textAlign = TextAlign.Center,
          style = VLRTheme.typography.titleMedium
        )
      },
      text = {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
          detailData.bans
            .takeIf { it.isNotEmpty() }
            ?.let {
              Text(
                text = detailData.bans.joinToString { it },
                modifier = modifier.padding(Local8DPPadding.current),
                textAlign = TextAlign.Center
              )
            }
          detailData.event.patch?.let {
            Text(
              text = it,
              modifier = modifier.padding(Local4DPPadding.current).fillMaxWidth(),
              textAlign = TextAlign.Center
            )
          }
          Text(
            text = detailData.event.date?.readableDateAndTimeWithZone ?: "",
            modifier = modifier.padding(Local4DPPadding.current).fillMaxWidth(),
            textAlign = TextAlign.Center
          )
          Text(
            text = detailData.event.stage,
            modifier = modifier.padding(Local4DPPadding.current).fillMaxWidth(),
            textAlign = TextAlign.Center
          )
          Text(
            text = detailData.event.series,
            modifier = modifier.padding(Local4DPPadding.current).fillMaxWidth(),
            textAlign = TextAlign.Center
          )
        }
      },
      confirmButton = {}
    )
}
