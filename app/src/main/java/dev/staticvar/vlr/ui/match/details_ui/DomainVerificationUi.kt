package dev.staticvar.vlr.ui.match.details_ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.R
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun DomainVerificationUi(
  modifier: Modifier = Modifier,
) {

  val context = LocalContext.current
  val intent =
    if (android.os.Build.MANUFACTURER.equals("samsung", true)) {
      Intent("android.settings.MANAGE_DOMAIN_URLS")
    } else {
      Intent(
        Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
        Uri.parse("package:${context.packageName}")
      )
    }

  if (intent.resolveActivity(context.packageManager) != null) {
    CardView(modifier = modifier.fillMaxWidth()) {
      Column(modifier = modifier.padding(8.dp)) {
        Text(
          text = stringResource(id = R.string.domain_verification),
          style = VLRTheme.typography.titleMedium,
        )
        Text(
          text = stringResource(id = R.string.domain_verification_desc),
          style = VLRTheme.typography.labelMedium
        )
        Text(
          text = if (android.os.Build.MANUFACTURER.equals(
              "samsung",
              true
            )
          ) stringResource(id = R.string.domain_verification_samsung_steps) else stringResource(id = R.string.domain_verification_steps),
          style = VLRTheme.typography.labelMedium
        )
        OutlinedButton(
          onClick = { context.startActivity(intent) },
          modifier = modifier.fillMaxWidth(),
          shape = VLRTheme.shapes.small
        ) {
          Text(text = stringResource(id = R.string.verify))
        }
      }
    }
  }
}
