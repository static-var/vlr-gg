package dev.staticvar.vlr.ui.news

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.data.api.response.NewsResponseItem
import dev.staticvar.vlr.ui.CARD_ALPHA
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.Waiting
import dev.staticvar.vlr.utils.onFail
import dev.staticvar.vlr.utils.onPass
import dev.staticvar.vlr.utils.onWaiting

@Composable
fun NewsScreen(viewModel: VlrViewModel) {
  val newsInfo by remember(viewModel) { viewModel.getNews() }.collectAsState(initial = Waiting())

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    newsInfo
      .onPass {
        data?.let { list ->
          LazyColumn() {
            item { Spacer(modifier = Modifier.statusBarsPadding()) }
            items(list) { NewsItem(newsResponseItem = it) }
          }
        }
      }
      .onWaiting { LinearProgressIndicator() }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun NewsItem(newsResponseItem: NewsResponseItem) {
  val context = LocalContext.current
  Card(
    modifier =
      Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).clickable {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(context, Uri.parse(newsResponseItem.link))
      },
    shape = RoundedCornerShape(16.dp),
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(CARD_ALPHA)
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Text(
        text = newsResponseItem.title,
        style = VLRTheme.typography.titleSmall,
        modifier = Modifier.padding(4.dp),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.Person,
          contentDescription = "",
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = newsResponseItem.author,
          style = VLRTheme.typography.bodySmall,
          modifier = Modifier.padding(4.dp).weight(1f)
        )
        Icon(
          imageVector = Icons.Outlined.DateRange,
          contentDescription = "",
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = newsResponseItem.date,
          style = VLRTheme.typography.bodySmall,
          modifier = Modifier.padding(4.dp)
        )
      }

      Text(
        text = newsResponseItem.description,
        style = VLRTheme.typography.bodySmall,
        modifier = Modifier.padding(4.dp),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}
