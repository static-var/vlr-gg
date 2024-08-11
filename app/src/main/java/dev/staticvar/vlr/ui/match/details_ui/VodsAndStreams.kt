package dev.staticvar.vlr.ui.match.details_ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.MatchInfo
import dev.staticvar.vlr.ui.Local16DPPadding
import dev.staticvar.vlr.ui.helper.EmphasisCardView
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun VideoReferenceUi(
  modifier: Modifier = Modifier,
  videos: MatchInfo.Videos,
) {
  var streamAndVodsCard by rememberSaveable { mutableStateOf(false) }
  val intent by remember { mutableStateOf(Intent(Intent.ACTION_VIEW)) }
  val context = LocalContext.current
  if (videos.streams.isEmpty() && videos.vods.isEmpty()) return

  EmphasisCardView(
    modifier.animateContentSize(tween(500)),
  ) {
    if (streamAndVodsCard) {
      Row(
        modifier
          .fillMaxWidth()
          .padding(Local16DPPadding.current)
          .clickable {
            streamAndVodsCard = streamAndVodsCard.not()
          },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = stringResource(R.string.streams_and_vods),
          style = VLRTheme.typography.titleSmall,
          color = VLRTheme.colorScheme.primary,
          textAlign = TextAlign.Center,
          modifier = modifier.weight(1f)
        )
        Icon(
          Icons.Outlined.ArrowUpward,
          contentDescription = stringResource(R.string.expand),
          modifier = modifier.size(16.dp),
          tint = VLRTheme.colorScheme.primary,
        )
      }
      if (videos.streams.isNotEmpty()) {
        Text(
          text = "Stream", modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
        )
        LazyRow(
          modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
        ) {
          items(videos.streams) { stream ->
            Button(
              onClick = {
                intent.data = Uri.parse(stream.url)
                context.startActivity(intent)
              },
              modifier = modifier.padding(horizontal = 4.dp),
              shape = VLRTheme.shapes.small
            ) {
              Text(text = stream.name)
            }
          }
        }
      }

      if (videos.vods.isNotEmpty()) {
        Text(
          text = stringResource(R.string.vods),
          modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
        )
        LazyRow(
          modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
        ) {
          items(videos.vods) { vod ->
            Button(
              onClick = {
                intent.data = Uri.parse(vod.url)
                context.startActivity(intent)
              },
              modifier = modifier.padding(horizontal = 4.dp),
              shape = VLRTheme.shapes.small
            ) {
              Text(text = vod.name)
            }
          }
        }
      }
    } else {
      Row(
        modifier
          .fillMaxWidth()
          .padding(Local16DPPadding.current)
          .clickable {
            streamAndVodsCard = streamAndVodsCard.not()
          },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = stringResource(R.string.streams_and_vods),
          style = VLRTheme.typography.titleSmall,
          color = VLRTheme.colorScheme.primary,
          textAlign = TextAlign.Center,
          modifier = modifier.weight(1f)
        )
        Icon(
          Icons.Outlined.ArrowDownward,
          contentDescription = stringResource(R.string.expand),
          modifier = modifier.size(16.dp),
          tint = VLRTheme.colorScheme.primary,
        )
      }
    }
  }
}
