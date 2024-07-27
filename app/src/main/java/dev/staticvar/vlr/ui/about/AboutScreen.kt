package dev.staticvar.vlr.ui.about

import android.content.Context
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.messaging.FirebaseMessaging
import dev.staticvar.vlr.R
import dev.staticvar.vlr.ui.Local16DPPadding
import dev.staticvar.vlr.ui.Local2DPPadding
import dev.staticvar.vlr.ui.Local4DP_2DPPadding
import dev.staticvar.vlr.ui.Local8DPPadding
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.scrim.StatusBarSpacer
import dev.staticvar.vlr.ui.scrim.StatusBarType
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.e
import dev.staticvar.vlr.utils.openAsCustomTab

@Composable
fun AboutScreen() {

  val context = LocalContext.current
  val currentAppVersion = context.currentAppVersion

  var simpleEasterEgg by remember { mutableStateOf(false) }
  Column(modifier = Modifier.fillMaxSize()) {
    StatusBarSpacer(statusBarType = StatusBarType.TRANSPARENT)
    Text(
      text = stringResource(id = R.string.app_name),
      modifier =
        Modifier.fillMaxWidth()
          .padding(Local16DPPadding.current)
          .combinedClickable(onLongClick = { simpleEasterEgg = true }, onClick = {}),
      textAlign = TextAlign.Center,
      style = VLRTheme.typography.headlineSmall,
      color = VLRTheme.colorScheme.primary,
    )

    AndroidCard()
    BackendCard()
    FeedbackCard()

    Spacer(modifier = Modifier.weight(1f))

    VersionFooter(currentAppVersion = currentAppVersion, simpleEasterEgg)
  }
}

@Composable
fun AndroidCard(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  CardView(modifier = modifier) {
    Text(
      text = "Android",
      modifier = Modifier.padding(Local8DPPadding.current),
      style = VLRTheme.typography.titleMedium,
      color = VLRTheme.colorScheme.primary,
    )
    Row(
      modifier = modifier.fillMaxWidth().padding(Local4DP_2DPPadding.current),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Button(
        onClick = { "https://staticvar.dev".openAsCustomTab(context) },
        modifier = modifier.weight(1f).padding(Local2DPPadding.current),
        shape = VLRTheme.shapes.small,
      ) {
        Icon(
          imageVector = Icons.Outlined.Language,
          modifier = modifier.padding(Local2DPPadding.current).size(18.dp),
          contentDescription = stringResource(id = R.string.website),
        )
        Text(text = "staticvar.dev", overflow = TextOverflow.Ellipsis, maxLines = 1)
      }
      Button(
        onClick = { "https://github.com/static-var".openAsCustomTab(context) },
        modifier = modifier.weight(1f).padding(Local2DPPadding.current),
        shape = VLRTheme.shapes.small,
      ) {
        Icon(
          painterResource(id = R.drawable.github_logo),
          modifier = modifier.padding(Local2DPPadding.current).size(18.dp),
          contentDescription = stringResource(id = R.string.developer),
        )
        Text(text = "static-var")
      }
    }
    Row(
      modifier = modifier.fillMaxWidth().padding(Local4DP_2DPPadding.current),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Button(
        onClick = { "https://github.com/static-var/vlr-gg".openAsCustomTab(context) },
        modifier = modifier.weight(1f).padding(Local2DPPadding.current),
        shape = VLRTheme.shapes.small,
      ) {
        Icon(
          imageVector = Icons.Outlined.Code,
          modifier = modifier.padding(Local2DPPadding.current).size(18.dp),
          contentDescription = stringResource(id = R.string.source_code),
        )
        Text(
          text = stringResource(id = R.string.source_code),
          overflow = TextOverflow.Ellipsis,
          maxLines = 1,
        )
      }
      Button(
        onClick = { "https://github.com/sponsors/static-var".openAsCustomTab(context) },
        modifier = modifier.weight(1f).padding(Local2DPPadding.current),
        shape = VLRTheme.shapes.small,
      ) {
        Icon(
          imageVector = Icons.Outlined.Paid,
          modifier = modifier.padding(Local2DPPadding.current).size(18.dp),
          contentDescription = stringResource(id = R.string.sponsor),
        )
        Text(text = stringResource(id = R.string.sponsor))
      }
    }
  }
}

@Composable
fun BackendCard(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  CardView() {
    Text(
      text = "Backend",
      modifier = modifier.padding(Local8DPPadding.current),
      style = VLRTheme.typography.titleMedium,
      color = VLRTheme.colorScheme.primary,
    )
    Row(
      modifier = modifier.fillMaxWidth().padding(Local4DP_2DPPadding.current),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Button(
        onClick = { "https://akhilnarang.dev".openAsCustomTab(context) },
        modifier = modifier.weight(1f).padding(Local2DPPadding.current),
        shape = VLRTheme.shapes.small,
      ) {
        Icon(
          imageVector = Icons.Outlined.Language,
          modifier = modifier.padding(Local2DPPadding.current).size(18.dp),
          contentDescription = stringResource(id = R.string.website),
        )
        Text(text = "akhilnarang.dev", overflow = TextOverflow.Ellipsis, maxLines = 1)
      }
      Button(
        onClick = { "https://github.com/akhilnarang".openAsCustomTab(context) },
        modifier = modifier.weight(1f).padding(Local2DPPadding.current),
        shape = VLRTheme.shapes.small,
      ) {
        Icon(
          painterResource(id = R.drawable.github_logo),
          modifier = modifier.padding(Local2DPPadding.current).size(18.dp),
          contentDescription = stringResource(id = R.string.developer),
        )
        Text(text = "akhilnarang")
      }
    }
    Button(
      onClick = { "https://github.com/akhilnarang/vlrgg-scraper".openAsCustomTab(context) },
      modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
      shape = VLRTheme.shapes.small,
    ) {
      Icon(
        imageVector = Icons.Outlined.Code,
        modifier = modifier.padding(Local2DPPadding.current).size(18.dp),
        contentDescription = stringResource(id = R.string.website),
      )
      Text(text = stringResource(id = R.string.source_code))
    }
  }
}

@Composable
fun FeedbackCard(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  CardView() {
    Text(
      text = stringResource(id = R.string.feedback),
      modifier = modifier.padding(Local8DPPadding.current),
      style = VLRTheme.typography.titleMedium,
      color = VLRTheme.colorScheme.primary,
    )
    Button(
      onClick = { "https://t.me/vlr_gg_chat".openAsCustomTab(context) },
      modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
      shape = VLRTheme.shapes.small,
    ) {
      Icon(
        imageVector = Icons.Outlined.Language,
        modifier = modifier.padding(Local2DPPadding.current).size(18.dp),
        contentDescription = "Telegram",
      )
      Text(text = "Telegram")
    }
  }
}

@Composable
fun VersionFooter(currentAppVersion: String, simpleEasterEgg: Boolean) {
  val context = LocalContext.current
  Text(
    text = "${stringResource(id = R.string.package_name)} - ${context.packageName}",
    modifier = Modifier.fillMaxWidth(),
    style = VLRTheme.typography.bodySmall,
    textAlign = TextAlign.Center,
    color = VLRTheme.colorScheme.primary,
  )
  Text(
    text = "${stringResource(id = R.string.app_version)} - $currentAppVersion",
    modifier = Modifier.fillMaxWidth(),
    style = VLRTheme.typography.bodySmall,
    textAlign = TextAlign.Center,
    color = VLRTheme.colorScheme.primary,
  )
  if (simpleEasterEgg) {
    var token by remember(simpleEasterEgg) { mutableStateOf("processing") }

    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
      if (task.isSuccessful) {
        token = task.result
        println("Token $token")
      } else {
        e { "FCM Token error" }
      }
    }

    SelectionContainer() {
      Text(
        text = token,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        style = VLRTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = VLRTheme.colorScheme.primary,
      )
    }
  }
}

private val Context.currentAppVersion: String
  get() = packageManager.getPackageInfo(packageName, 0).versionName ?: ""
