package dev.staticvar.vlr.ui.about

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.R
import dev.staticvar.vlr.ui.CARD_ALPHA
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.currentAppVersion

@Composable
fun AboutScreen(viewModel: VlrViewModel) {
  val remoteAppVersion by
    remember(viewModel) { viewModel.getLatestAppVersion() }.collectAsState(initial = null)

  val context = LocalContext.current

  val currentAppVersion = context.currentAppVersion

  Column(modifier = Modifier.fillMaxSize()) {
    Spacer(modifier = Modifier.statusBarsPadding())
    Text(
      text = stringResource(id = R.string.app_description),
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      textAlign = TextAlign.Center,
      style = VLRTheme.typography.titleSmall
    )

    Card(
      modifier =
        Modifier.fillMaxWidth().padding(8.dp).clickable {
          val builder = CustomTabsIntent.Builder()
          val customTabsIntent = builder.build()
          customTabsIntent.launchUrl(context, Uri.parse(""))
        },
      shape = RoundedCornerShape(16.dp),
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      containerColor = MaterialTheme.colorScheme.primaryContainer.copy(CARD_ALPHA)
    ) {
      Text(
        text = "Android",
        modifier = Modifier.padding(8.dp),
        style = VLRTheme.typography.titleSmall
      )
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Button(
          onClick = {
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(context, Uri.parse("https://staticvar.dev"))
          },
          modifier = Modifier.weight(1f).padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.Language,
            modifier = Modifier.padding(horizontal = 2.dp),
            contentDescription = stringResource(id = R.string.website),
          )
          Text(text = "staticvar.dev", overflow = TextOverflow.Ellipsis, maxLines = 1)
        }
        Button(
          onClick = {
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(context, Uri.parse("https://github.com/static-var"))
          },
          modifier = Modifier.weight(1f).padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
          Icon(
            painterResource(id = R.drawable.github_logo),
            modifier = Modifier.padding(horizontal = 2.dp),
            contentDescription = stringResource(id = R.string.developer)
          )
          Text(text = "static-var")
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Button(
          onClick = {
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(context, Uri.parse("https://github.com/static-var/vlr-gg"))
          },
          modifier = Modifier.weight(1f).padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.Code,
            modifier = Modifier.padding(horizontal = 2.dp),
            contentDescription = stringResource(id = R.string.website),
          )
          Text(
            text = stringResource(id = R.string.source_code),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
          )
        }
        Button(
          onClick = {
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(
              context,
              Uri.parse("https://github.com/static-var/vlr-gg/releases")
            )
          },
          modifier = Modifier.weight(1f).padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.DownloadForOffline,
            modifier = Modifier.padding(horizontal = 2.dp),
            contentDescription = stringResource(id = R.string.website),
          )
          Text(text = stringResource(id = R.string.release))
        }
      }
    }

    Card(
      modifier =
        Modifier.fillMaxWidth().padding(8.dp).clickable {
          val builder = CustomTabsIntent.Builder()
          val customTabsIntent = builder.build()
          customTabsIntent.launchUrl(context, Uri.parse(""))
        },
      shape = RoundedCornerShape(16.dp),
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      containerColor = MaterialTheme.colorScheme.primaryContainer.copy(CARD_ALPHA)
    ) {
      Text(
        text = "Backend",
        modifier = Modifier.padding(8.dp),
        style = VLRTheme.typography.titleSmall
      )
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Button(
          onClick = {
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(context, Uri.parse("https://akhilnarang.dev"))
          },
          modifier = Modifier.weight(1f).padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.Language,
            modifier = Modifier.padding(horizontal = 2.dp),
            contentDescription = stringResource(id = R.string.website),
          )
          Text(text = "akhilnarang.dev", overflow = TextOverflow.Ellipsis, maxLines = 1)
        }
        Button(
          onClick = {
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(context, Uri.parse("https://github.com/akhilnarang"))
          },
          modifier = Modifier.weight(1f).padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
          Icon(
            painterResource(id = R.drawable.github_logo),
            modifier = Modifier.padding(horizontal = 2.dp),
            contentDescription = stringResource(id = R.string.developer)
          )
          Text(text = "akhilnarang")
        }
      }
      Button(
        onClick = {
          val builder = CustomTabsIntent.Builder()
          val customTabsIntent = builder.build()
          customTabsIntent.launchUrl(
            context,
            Uri.parse("https://github.com/akhilnarang/vlrgg-scraper")
          )
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
      ) {
        Icon(
          imageVector = Icons.Outlined.Code,
          modifier = Modifier.padding(horizontal = 2.dp),
          contentDescription = stringResource(id = R.string.website),
        )
        Text(text = stringResource(id = R.string.source_code))
      }
    }

    Spacer(modifier = Modifier.weight(1f))
    Text(
      text = context.packageName,
      modifier = Modifier.fillMaxWidth(),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
    Text(
      text = "${stringResource(id = R.string.app_version)} - $currentAppVersion",
      modifier = Modifier.fillMaxWidth(),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
    Text(
      text = "${stringResource(id = R.string.latest_app_version)} - $remoteAppVersion",
      modifier = Modifier.fillMaxWidth(),
      style = VLRTheme.typography.bodySmall,
      textAlign = TextAlign.Center
    )
  }
}
