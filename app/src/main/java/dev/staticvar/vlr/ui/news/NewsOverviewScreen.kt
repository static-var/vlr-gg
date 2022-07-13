package dev.staticvar.vlr.ui.news

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.staticvar.vlr.data.api.response.NewsResponseItem
import dev.staticvar.vlr.ui.*
import dev.staticvar.vlr.ui.common.ErrorUi
import dev.staticvar.vlr.ui.common.ScrollHelper
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.*

@Composable
fun NewsScreen(viewModel: VlrViewModel) {
  val newsInfo by remember(viewModel) { viewModel.getNews() }.collectAsState(initial = Waiting())
  var triggerRefresh by remember(viewModel) { mutableStateOf(true) }
  val updateState by
    remember(triggerRefresh) { viewModel.refreshNews() }.collectAsState(initial = Ok(false))

  val swipeRefresh = rememberSwipeRefreshState(isRefreshing = updateState.get() ?: false)

  val primaryContainer = Color.Transparent
  val systemUiController = rememberSystemUiController()
  val isDarkMode = isSystemInDarkTheme()

  SideEffect { systemUiController.setStatusBarColor(primaryContainer, darkIcons = !isDarkMode) }

  val modifier: Modifier = Modifier

  val resetScroll by remember { viewModel.resetScroll }.collectAsState(initial = false)
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

          SwipeRefresh(
            state = swipeRefresh,
            onRefresh = { triggerRefresh = triggerRefresh.not() },
            indicator = { _, _ -> }
          ) {
            LazyColumn(state = scrollState) {
              item { Spacer(modifier = modifier.statusBarsPadding()) }
              item {
                AnimatedVisibility(
                  visible = updateState.get() == true || swipeRefresh.isSwipeInProgress
                ) {
                  LinearProgressIndicator(
                    modifier.fillMaxWidth().padding(Local16DPPadding.current).animateContentSize()
                  )
                }
              }
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
