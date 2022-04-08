package dev.staticvar.vlr.ui.helper

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.R
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.PackageHelper
import kotlinx.coroutines.flow.flowOf

val Context.currentAppVersion: String
  get() = packageManager.getPackageInfo(packageName, 0).versionName ?: ""

@Composable
fun AppUpdateDownloadPopup(viewModel: VlrViewModel) {
  val url by remember { viewModel.getApkUrl() }.collectAsState(initial = null)
  var show by remember { mutableStateOf(true) }
  var downloadClicked by remember { mutableStateOf(false) }

  // initiate download process if dialog is still being shown and URL is not null
  val downloadProgress by
    produceState(initialValue = flowOf(Pair(0, ByteArray(0))), downloadClicked, url) {
        if (url != null && downloadClicked) {
          value = viewModel.downloadApkWithProgress(url!!)
        }
      }
      .value
      .collectAsState(initial = Pair(0, ByteArray(0)))

  // Once download is complete, initiate Installation process
  if (downloadProgress.first == 100 && downloadProgress.second.isNotEmpty()) {
    PackageHelper.convertByteToFileAndInstall(LocalContext.current, downloadProgress.second)
  }

  // Show dialog only when URL is available and make dialog dismissible only till download has not
  // begun
  if (show && url != null)
    AlertDialog(
      onDismissRequest = { if (!downloadClicked) show = false },
      title = { Text(text = "Update available", style = VLRTheme.typography.titleMedium) },
      text = {
        AnimatedVisibility(visible = show) {
          Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            if (downloadClicked) {
              Text(
                text =
                  if (downloadProgress.first == 100) stringResource(R.string.download_complete)
                  else stringResource(R.string.downloading_percent, downloadProgress.first)
              )
              LinearProgressIndicator(
                progress = downloadProgress.first.div(100f),
                modifier = Modifier.padding(4.dp)
              )
            } else {
              Text(text = stringResource(R.string.begin_download))
            }
          }
        }
      },
      confirmButton = {
        AnimatedVisibility(visible = show) {
          if (!downloadClicked)
            Button(onClick = { downloadClicked = true }) {
              Text(text = stringResource(R.string.download))
            }
        }
      },
      dismissButton = {
        if (!downloadClicked)
          Button(onClick = { show = false }) { Text(text = stringResource(R.string.cancel)) }
      }
    )
}
