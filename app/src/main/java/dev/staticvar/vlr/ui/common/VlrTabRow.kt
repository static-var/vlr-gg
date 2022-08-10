package dev.staticvar.vlr.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.google.accompanist.pager.PagerState
import dev.staticvar.vlr.ui.Local16DPPadding
import dev.staticvar.vlr.ui.helper.VLRTabIndicator
import kotlinx.coroutines.launch

@Composable
fun VlrTabRowForViewPager(modifier: Modifier, pagerState: PagerState, tabs: List<String>) {
  val scope = rememberCoroutineScope()
  TabRow(
    modifier = modifier.fillMaxWidth(),
    selectedTabIndex = pagerState.currentPage,
    indicator = { indicators -> VLRTabIndicator(indicators, pagerState.currentPage) }
  ) {
    tabs.forEachIndexed { index, title ->
      Tab(
        selected = pagerState.currentPage == index,
        onClick = { scope.launch { pagerState.scrollToPage(index) } }
      ) {
        Text(
          text = title,
          modifier = modifier.padding(Local16DPPadding.current).fillMaxWidth(),
          textAlign = TextAlign.Center
        )
      }
    }
  }
}


@Composable
fun VlrScrollableTabRowForViewPager(modifier: Modifier, pagerState: PagerState, tabs: List<String>) {
  val scope = rememberCoroutineScope()
  ScrollableTabRow(
    modifier = modifier.fillMaxWidth(),
    selectedTabIndex = pagerState.currentPage,
    indicator = { indicators -> VLRTabIndicator(indicators, pagerState.currentPage) }
  ) {
    tabs.forEachIndexed { index, title ->
      Tab(
        selected = pagerState.currentPage == index,
        onClick = { scope.launch { pagerState.scrollToPage(index) } }
      ) {
        Text(
          text = title,
          modifier = modifier.padding(Local16DPPadding.current).fillMaxWidth(),
          textAlign = TextAlign.Center
        )
      }
    }
  }
}
