package dev.staticvar.vlr.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.jeziellago.compose.markdowntext.MarkdownText
import dev.staticvar.vlr.R
import dev.staticvar.vlr.ui.Local4DPPadding
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun ChangeLogDialog(
  modifier: Modifier = Modifier,
  text: String,
  onDismiss: (Boolean) -> Unit
) {
  AlertDialog(
    onDismissRequest = { onDismiss.invoke(false) },
    title = { Text(text = stringResource(id = R.string.changelog)) },
    text = {
      MarkdownText(
        markdown = text,
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
