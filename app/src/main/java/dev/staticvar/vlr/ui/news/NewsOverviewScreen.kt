package dev.staticvar.vlr.ui.news

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.staticvar.vlr.data.api.response.NewsResponseItem
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.ui.theme.tintedBackground
import dev.staticvar.vlr.utils.*

@Composable
fun NewsScreen(viewModel: VlrViewModel) {
  val newsInfo by remember(viewModel) { viewModel.getNews() }.collectAsState(initial = Waiting())

  val primaryContainer = VLRTheme.colorScheme.tintedBackground
  val systemUiController = rememberSystemUiController()
  SideEffect {
    systemUiController.setStatusBarColor(primaryContainer)
  }

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    newsInfo
      .onPass {
        data?.let { list ->
          val safeConvertedList =
            kotlin.runCatching { list.sortedByDescending { it.date.timeToEpoch } }

          LazyColumn() {
            item { Spacer(modifier = Modifier.statusBarsPadding()) }
            items(
              if (safeConvertedList.isFailure) list else safeConvertedList.getOrElse { listOf() }
            ) { NewsItem(newsResponseItem = it) }
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
  CardView(
    modifier =
      Modifier.clickable {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(context, Uri.parse(newsResponseItem.link))
      },
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Text(
        text = newsResponseItem.title,
        style = VLRTheme.typography.titleSmall,
        modifier = Modifier.padding(4.dp),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        color = VLRTheme.colorScheme.primary,
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.Person,
          contentDescription = "",
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = newsResponseItem.author,
          style = VLRTheme.typography.bodySmall,
          modifier = Modifier.padding(4.dp).weight(1f)
        )
        Icon(
          imageVector = Icons.Outlined.DateRange,
          contentDescription = "",
          modifier = Modifier.size(16.dp),
        )
        val convertedDate = kotlin.runCatching { newsResponseItem.date.readableDate }
        Text(
          text =
            if (convertedDate.isSuccess) convertedDate.getOrDefault(newsResponseItem.date)
            else newsResponseItem.date,
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
