package dev.staticvar.vlr.ui.helper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.MatchPreviewInfo
import dev.staticvar.vlr.ui.Local2DPPadding
import dev.staticvar.vlr.ui.Local4DP_2DPPadding
import dev.staticvar.vlr.ui.Local8DP_4DPPadding
import dev.staticvar.vlr.ui.match.MAX_SHARABLE_ITEMS
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.StableHolder
import dev.staticvar.vlr.utils.readableDateAndTimeWithZone
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@Composable
fun SharingAppBar(
  modifier: Modifier,
  items: List<MatchPreviewInfo>,
  shareMode: (Boolean) -> Unit,
  shareConfirm: (Boolean) -> Unit
) {
  Row(
    modifier.fillMaxWidth().height(40.dp).background(VLRTheme.colorScheme.background),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.Outlined.Close,
      contentDescription = stringResource(id = R.string.cancel),
      modifier =
        modifier.padding(Local8DP_4DPPadding.current).clickable { shareMode(false) }.size(32.dp),
      tint = VLRTheme.colorScheme.primary,
    )
    Spacer(modifier = modifier.weight(1f))
    Text(
      text = "${items.size}/$MAX_SHARABLE_ITEMS",
      modifier = modifier.padding(Local8DP_4DPPadding.current),
      color = VLRTheme.colorScheme.primary
    )
    Icon(
      imageVector = Icons.Outlined.Send,
      contentDescription = stringResource(R.string.share),
      modifier =
        modifier.padding(Local8DP_4DPPadding.current).clickable { shareConfirm(true) }.size(32.dp),
      tint = VLRTheme.colorScheme.primary
    )
  }
}

@Composable
fun ShareDialog(matches: StableHolder<List<MatchPreviewInfo>>, onDismiss: () -> Unit) {
  var shareToggle by remember { mutableStateOf(false) }
  val context = LocalContext.current
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.preview), color = VLRTheme.colorScheme.primary) },
    text = {
      CaptureBitmap(
        captureRequestKey = shareToggle,
        content = { SharableListUi(matches = matches) },
        onBitmapCaptured = { bitmap ->
          val imagePath = File(context.externalCacheDir, "my_images")
          if (!imagePath.exists()) imagePath.mkdirs()
          val file = File(imagePath, System.currentTimeMillis().toString() + ".png")

          val imageUri =
            FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
          val os: OutputStream = BufferedOutputStream(FileOutputStream(file))
          os.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, os) }
          fireIntent(context = context, file = imageUri, matches = matches.item)
        }
      )
    },
    confirmButton = {
      Button(onClick = { shareToggle = shareToggle.not() }) {
        Text(text = stringResource(R.string.share))
      }
    }
  )
}

@Composable
fun SharableListUi(modifier: Modifier = Modifier, matches: StableHolder<List<MatchPreviewInfo>>) {
  Column(modifier.fillMaxWidth().background(VLRTheme.colorScheme.primaryContainer)) {
    CardView(
      colors =
        CardDefaults.cardColors(
          contentColor = VLRTheme.colorScheme.onPrimaryContainer,
          containerColor = VLRTheme.colorScheme.primaryContainer
        )
    ) {
      matches.item.forEachIndexed { index, matchPreviewInfo ->
        SharableMatchUi(match = matchPreviewInfo)
        if (index != matches.item.size - 1)
          Divider(modifier = Modifier.fillMaxWidth().padding(2.dp).height(0.5.dp))
      }
    }
  }
}

@Composable
fun SharableMatchUi(modifier: Modifier = Modifier, match: MatchPreviewInfo) {
  Text(
    text =
      if (match.status.equals(stringResource(R.string.live), true)) stringResource(R.string.live)
      else match.time?.readableDateAndTimeWithZone ?: "",
    modifier = modifier.fillMaxWidth().padding(Local2DPPadding.current),
    textAlign = TextAlign.Center,
    style = VLRTheme.typography.labelSmall
  )
  Row(modifier = modifier.fillMaxWidth().padding(Local4DP_2DPPadding.current)) {
    Text(
      text = match.team1.name,
      overflow = TextOverflow.Ellipsis,
      maxLines = 1,
      modifier = modifier.weight(3f),
      textAlign = TextAlign.Start,
      style = VLRTheme.typography.bodySmall
    )
    Text(
      text = match.team1.score?.toString() ?: "-",
      overflow = TextOverflow.Ellipsis,
      maxLines = 1,
      modifier = modifier.weight(1f),
      textAlign = TextAlign.End,
      style = VLRTheme.typography.bodySmall
    )
  }
  Row(modifier = modifier.fillMaxWidth().padding(Local4DP_2DPPadding.current)) {
    Text(
      text = match.team2.name,
      overflow = TextOverflow.Ellipsis,
      maxLines = 1,
      modifier = modifier.weight(3f),
      textAlign = TextAlign.Start,
      style = VLRTheme.typography.bodySmall
    )
    Text(
      text = match.team2.score?.toString() ?: "-",
      overflow = TextOverflow.Ellipsis,
      maxLines = 1,
      modifier = modifier.weight(1f),
      textAlign = TextAlign.End,
      style = VLRTheme.typography.bodySmall
    )
  }
}

fun fireIntent(context: Context, file: Uri, matches: List<MatchPreviewInfo>) {
  val string = buildString {
    matches.forEach {
      appendLine(
        "${it.team1.name} vs ${it.team2.name} | ${it.time?.readableDateAndTimeWithZone} | ${it.id.urlFromId()}"
      )
      appendLine()
    }
    appendLine(context.resources.getString(R.string.shared_via))
  }

  val shareIntent =
    Intent(Intent.ACTION_SEND).apply {
      flags = Intent.FLAG_GRANT_READ_URI_PERMISSION + Intent.FLAG_GRANT_WRITE_URI_PERMISSION
      putExtra(Intent.EXTRA_STREAM, file)
      putExtra(Intent.EXTRA_TEXT, string)
      type = "image/png"
    }
  context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_with)))
}

private fun String.urlFromId() = "https://vlr.gg/$this"
