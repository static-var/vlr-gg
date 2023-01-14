package dev.staticvar.vlr.ui.news

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import dev.staticvar.vlr.data.api.response.NewsResponseItem
import dev.staticvar.vlr.ui.*
import dev.staticvar.vlr.ui.common.ErrorUi
import dev.staticvar.vlr.ui.common.ScrollHelper
import dev.staticvar.vlr.ui.common.StatusBarColorForHome
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.*

@Composable
fun NewsScreen(viewModel: VlrViewModel) {
  StatusBarColorForHome()

  val newsInfo by
    remember(viewModel) { viewModel.getNews() }
      .collectAsStateWithLifecycle(initialValue = Waiting())
  var triggerRefresh by remember(viewModel) { mutableStateOf(true) }
  val updateState by
    remember(triggerRefresh) { viewModel.refreshNews() }
      .collectAsStateWithLifecycle(initialValue = Ok(false))

  val swipeRefresh =
    rememberPullRefreshState(updateState.get() ?: false, { triggerRefresh = triggerRefresh.not() })

  val modifier: Modifier = Modifier

  val resetScroll by
    remember { viewModel.resetScroll }.collectAsStateWithLifecycle(initialValue = false)
  val scrollState = rememberLazyListState()
  scrollState.ScrollHelper(resetScroll = resetScroll) { viewModel.postResetScroll() }

  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    newsInfo
      .onPass {
        data?.let { list ->
          val safeConvertedList =
            kotlin.runCatching { list.sortedByDescending { it.date.timeToEpoch } }

          AnimatedVisibility(
            visible = updateState.get() == true || swipeRefresh.progress != 0f,
            modifier = Modifier
              .statusBarsPadding(),
          ) {
            LinearProgressIndicator(
              modifier
                .fillMaxWidth()
                .padding(Local16DPPadding.current)
                .animateContentSize()
                .testTag("common:loader")
            )
          }
          Box(
            modifier = Modifier
              .pullRefresh(swipeRefresh)
              .fillMaxSize(),
          ) {
            LazyColumn(state = scrollState, modifier = modifier.testTag("newsOverview:root")) {
              item { Spacer(modifier = modifier.statusBarsPadding()) }

              updateState.getError()?.let {
                item { ErrorUi(modifier = modifier, exceptionMessage = it.stackTraceToString()) }
              }

              items(
                if (safeConvertedList.isFailure) list else safeConvertedList.getOrElse { listOf() },
                key = { item -> item.link }
              ) { NewsItem(modifier, newsResponseItem = it, action = viewModel.action) }
            }
          }
        }
      }
      .onWaiting { LinearProgressIndicator(modifier.animateContentSize()) }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun NewsItem(modifier: Modifier = Modifier, newsResponseItem: NewsResponseItem, action: Action) {
  CardView(
    modifier = modifier.clickable { action.news(newsResponseItem.link.split("/")[3]) },
  ) {
    Column(modifier = modifier.padding(Local8DPPadding.current)) {
      Text(
        text = newsResponseItem.title,
        style = VLRTheme.typography.titleLarge,
        modifier = modifier.padding(Local4DPPadding.current),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        color = VLRTheme.colorScheme.primary,
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.Person,
          contentDescription = "",
          modifier = modifier.size(16.dp),
        )
        Text(
          text = newsResponseItem.author,
          style = VLRTheme.typography.labelSmall,
          modifier = modifier.padding(Local4DPPadding.current).weight(1f)
        )
        Icon(
          imageVector = Icons.Outlined.DateRange,
          contentDescription = "",
          modifier = modifier.size(16.dp),
        )
        val convertedDate = kotlin.runCatching { newsResponseItem.date.readableDate }
        Text(
          text =
            if (convertedDate.isSuccess) convertedDate.getOrDefault(newsResponseItem.date)
            else newsResponseItem.date,
          style = VLRTheme.typography.labelSmall,
          modifier = modifier.padding(Local4DPPadding.current)
        )
      }

      Text(
        text = newsResponseItem.description,
        style = VLRTheme.typography.bodyMedium,
        modifier = modifier.padding(Local4DPPadding.current),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}
