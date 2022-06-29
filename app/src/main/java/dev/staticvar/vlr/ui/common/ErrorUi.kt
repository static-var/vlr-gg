package dev.staticvar.vlr.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.staticvar.vlr.R
import dev.staticvar.vlr.ui.Local4DPPadding
import dev.staticvar.vlr.ui.Local8DPPadding
import dev.staticvar.vlr.ui.helper.EmphasisCardView
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun ErrorUi(modifier: Modifier, exceptionMessage: String) {
  var launchDialog by remember { mutableStateOf(false) }
  EmphasisCardView() {
    Column(modifier = modifier.padding(Local8DPPadding.current).fillMaxWidth()) {
      Text(
        stringResource(id = R.string.error_occurred),
        style = VLRTheme.typography.titleSmall,
        modifier = modifier.padding(Local4DPPadding.current).fillMaxWidth(),
      )

      Button(onClick = { launchDialog = true }, modifier = modifier.fillMaxWidth()) {
        Text(text = stringResource(id = R.string.click_here))
      }
    }
  }
  if (launchDialog)
    ErrorDialog(modifier = modifier, message = exceptionMessage, onDismiss = { launchDialog = it })
}

@Composable
fun ErrorDialog(modifier: Modifier, message: String, onDismiss: (Boolean) -> Unit) {
  AlertDialog(
    onDismissRequest = { onDismiss.invoke(false) },
    title = { Text(text = stringResource(id = R.string.error_log)) },
    text = {
      Text(
        text = message,
        modifier =
          modifier
            .fillMaxWidth()
            .padding(Local4DPPadding.current)
            .verticalScroll(rememberScrollState()),
        style = VLRTheme.typography.bodySmall
      )
    },
    confirmButton = {
      Button(onClick = { onDismiss.invoke(false) }) {
        Text(text = stringResource(R.string.dismiss))
      }
    }
  )
}
