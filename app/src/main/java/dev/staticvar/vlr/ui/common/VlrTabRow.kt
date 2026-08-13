package dev.staticvar.vlr.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.staticvar.vlr.ui.Local16DPPadding
import dev.staticvar.vlr.ui.helper.VLRTabIndicator
import kotlinx.coroutines.launch

@Composable
fun VlrTabRowForViewPager(modifier: Modifier, pagerState: PagerState, tabs: List<String>) {
  val scope = rememberCoroutineScope()
  SecondaryTabRow(
    modifier = modifier.fillMaxWidth(),
    selectedTabIndex = pagerState.currentPage,
    containerColor = TabRowDefaults.primaryContainerColor,
    contentColor = TabRowDefaults.primaryContentColor,
    indicator = { VLRTabIndicator(pagerState.currentPage) }
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
fun VlrScrollableTabRowForViewPager(
  modifier: Modifier,
  pagerState: PagerState,
  tabs: List<String>
) {
  val scope = rememberCoroutineScope()
  SecondaryScrollableTabRow(
    modifier = modifier.fillMaxWidth(),
    selectedTabIndex = pagerState.currentPage,
    containerColor = TabRowDefaults.primaryContainerColor,
    contentColor = TabRowDefaults.primaryContentColor,
    indicator = { VLRTabIndicator(pagerState.currentPage) }
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
