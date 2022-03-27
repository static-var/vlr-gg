package dev.staticvar.vlr.ui.about

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.staticvar.vlr.R
import dev.staticvar.vlr.ui.*
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.helper.currentAppVersion
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.ui.theme.tintedBackground
import dev.staticvar.vlr.utils.openAsCustomTab

@Composable
fun AboutScreen(viewModel: VlrViewModel) {
  val context = LocalContext.current
  val currentAppVersion = context.currentAppVersion

  val remoteAppVersion by
    remember(viewModel) { viewModel.getLatestAppVersion() }.collectAsState(initial = null)

  val primaryContainer = VLRTheme.colorScheme.tintedBackground
  val systemUiController = rememberSystemUiController()
  SideEffect { systemUiController.setStatusBarColor(primaryContainer) }

  Column(modifier = Modifier.fillMaxSize()) {
    Spacer(modifier = Modifier.statusBarsPadding())
    Text(
      text = stringResource(id = R.string.app_description),
      modifier = Modifier.fillMaxWidth().padding(Local16DPPadding.current),
      textAlign = TextAlign.Center,
      style = VLRTheme.typography.titleSmall,
      color = VLRTheme.colorScheme.primary,
    )

    AndroidCard(context = context)
    BackendCard(context = context)

    Spacer(modifier = Modifier.weight(1f))

    VersionFooter(
      context = context,
      currentAppVersion = currentAppVersion,
      remoteAppVersion = remoteAppVersion
    )
  }
}

@Composable
fun AndroidCard(modifier: Modifier = Modifier, context: Context) {
  CardView(modifier = modifier) {
    Text(
      text = "Android",
      modifier = Modifier.padding(Local8DPPadding.current),
      style = VLRTheme.typography.titleSmall,
      color = VLRTheme.colorScheme.primary,
    )
    Row(
      modifier = modifier.fillMaxWidth().padding(Local4DP_2DPPadding.current),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Button(
        onClick = { "https://staticvar.dev".openAsCustomTab(context) },
        modifier = modifier.weight(1f).padding(Local2DPPadding.current)
      ) {
        Icon(
          imageVector = Icons.Outlined.Language,
          modifier = modifier.padding(Local2DPPadding.current),
          contentDescription = stringResource(id = R.string.website),
        )
        Text(text = "staticvar.dev", overflow = TextOverflow.Ellipsis, maxLines = 1)
      }
      Button(
        onClick = { "https://github.com/static-var".openAsCustomTab(context) },
        modifier = modifier.weight(1f).padding(Local2DPPadding.current)
      ) {
        Icon(
          painterResource(id = R.drawable.github_logo),
          modifier = modifier.padding(Local2DPPadding.current),
          contentDescription = stringResource(id = R.string.developer)
        )
        Text(text = "static-var")
      }
    }
    Row(
      modifier = modifier.fillMaxWidth().padding(Local4DP_2DPPadding.current),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Button(
        onClick = { "https://github.com/static-var/vlr-gg".openAsCustomTab(context) },
        modifier = modifier.weight(1f).padding(Local2DPPadding.current)
      ) {
        Icon(
          imageVector = Icons.Outlined.Code,
          modifier = modifier.padding(Local2DPPadding.current),
          contentDescription = stringResource(id = R.string.website),
        )
        Text(
          text = stringResource(id = R.string.source_code),
          overflow = TextOverflow.Ellipsis,
          maxLines = 1
        )
      }
      Button(
        onClick = { "https://github.com/static-var/vlr-gg/releases".openAsCustomTab(context) },
        modifier = modifier.weight(1f).padding(Local2DPPadding.current)
      ) {
        Icon(
          imageVector = Icons.Outlined.DownloadForOffline,
          modifier = modifier.padding(Local2DPPadding.current),
          contentDescription = stringResource(id = R.string.website),
        )
        Text(text = stringResource(id = R.string.release))
      }
    }
  }
}

@Composable
fun ColumnScope.BackendCard(modifier: Modifier = Modifier, context: Context) {
  CardView() {
    Text(
      text = "Backend",
      modifier = modifier.padding(Local8DPPadding.current),
      style = VLRTheme.typography.titleSmall,
      color = VLRTheme.colorScheme.primary,
    )
    Row(
      modifier = modifier.fillMaxWidth().padding(Local4DP_2DPPadding.current),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Button(
        onClick = { "https://akhilnarang.dev".openAsCustomTab(context) },
        modifier = modifier.weight(1f).padding(Local2DPPadding.current)
      ) {
        Icon(
          imageVector = Icons.Outlined.Language,
          modifier = modifier.padding(Local2DPPadding.current),
          contentDescription = stringResource(id = R.string.website),
        )
        Text(text = "akhilnarang.dev", overflow = TextOverflow.Ellipsis, maxLines = 1)
      }
      Button(
        onClick = { "https://github.com/akhilnarang".openAsCustomTab(context) },
        modifier = modifier.weight(1f).padding(Local2DPPadding.current)
      ) {
        Icon(
          painterResource(id = R.drawable.github_logo),
          modifier = modifier.padding(Local2DPPadding.current),
          contentDescription = stringResource(id = R.string.developer)
        )
        Text(text = "akhilnarang")
      }
    }
    Button(
      onClick = { "https://github.com/akhilnarang/vlrgg-scraper".openAsCustomTab(context) },
      modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
      Icon(
        imageVector = Icons.Outlined.Code,
        modifier = modifier.padding(Local2DPPadding.current),
        contentDescription = stringResource(id = R.string.website),
      )
      Text(text = stringResource(id = R.string.source_code))
    }
  }
}

@Composable
fun ColumnScope.VersionFooter(
  context: Context,
  currentAppVersion: String,
  remoteAppVersion: String?
) {
  Text(
    text = "${stringResource(id = R.string.package_name)} - ${context.packageName}",
    modifier = Modifier.fillMaxWidth(),
    style = VLRTheme.typography.bodySmall,
    textAlign = TextAlign.Center,
    color = VLRTheme.colorScheme.primary
  )
  Text(
    text = "${stringResource(id = R.string.app_version)} - $currentAppVersion",
    modifier = Modifier.fillMaxWidth(),
    style = VLRTheme.typography.bodySmall,
    textAlign = TextAlign.Center,
    color = VLRTheme.colorScheme.primary
  )
  Text(
    text =
      "${stringResource(id = R.string.latest_app_version)} - ${remoteAppVersion ?: "finding..."}",
    modifier = Modifier.fillMaxWidth(),
    style = VLRTheme.typography.bodySmall,
    textAlign = TextAlign.Center,
    color = VLRTheme.colorScheme.primary
  )
}
