package dev.staticvar.vlr.ui.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.staticvar.vlr.R
import dev.staticvar.vlr.ui.*
import dev.staticvar.vlr.ui.common.ChangeLogDialog
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.helper.currentAppVersion
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.openAsCustomTab

@Composable
fun AboutScreen(viewModel: VlrViewModel) {
  val context = LocalContext.current
  val currentAppVersion = context.currentAppVersion

  val remoteAppVersion by
    remember(viewModel) { viewModel.getLatestAppVersion() }
      .collectAsStateWithLifecycle(initialValue = null)

  val changelogText by
    remember(viewModel) { viewModel.getLatestChangelog() }
      .collectAsStateWithLifecycle(initialValue = null)

  val primaryContainer = Color.Transparent
  val systemUiController = rememberSystemUiController()
  val isDarkMode = isSystemInDarkTheme()

  SideEffect { systemUiController.setStatusBarColor(primaryContainer, darkIcons = !isDarkMode) }

  Column(modifier = Modifier.fillMaxSize()) {
    Spacer(modifier = Modifier.statusBarsPadding())
    Text(
      text = stringResource(id = R.string.app_description),
      modifier = Modifier.fillMaxWidth().padding(Local16DPPadding.current),
      textAlign = TextAlign.Center,
      style = VLRTheme.typography.headlineSmall,
      color = VLRTheme.colorScheme.primary,
    )

    AndroidCard(changelog = changelogText)
    BackendCard()

    Spacer(modifier = Modifier.weight(1f))

    VersionFooter(currentAppVersion = currentAppVersion, remoteAppVersion = remoteAppVersion)
  }
}

@Composable
fun AndroidCard(modifier: Modifier = Modifier, changelog: String? = null) {
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

    ChangelogPreview(modifier.padding(Local2DPPadding.current), changelog)
  }
}

@Composable
fun ColumnScope.BackendCard(modifier: Modifier = Modifier) {
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
fun ColumnScope.VersionFooter(currentAppVersion: String, remoteAppVersion: String?) {
  val context = LocalContext.current
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

@Composable
fun ChangelogPreview(modifier: Modifier = Modifier, text: String? = null) {
  AnimatedVisibility(visible = text != null) {
    var launchDialog by remember(text) { mutableStateOf(false) }

    Button(
      onClick = { launchDialog = true },
      modifier = modifier.fillMaxWidth().padding(Local2DPPadding.current)
    ) {
      Icon(
        imageVector = Icons.Filled.TextSnippet,
        modifier = modifier.padding(Local2DPPadding.current),
        contentDescription = stringResource(id = R.string.developer)
      )
      Text(text = stringResource(id = R.string.changelog))
    }
    if (launchDialog && text != null) {
      ChangeLogDialog(text = text, onDismiss = { launchDialog = it })
    }
  }
}
