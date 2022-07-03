package dev.staticvar.vlr.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.google.accompanist.pager.PagerState
import dev.staticvar.vlr.ui.Local16DPPadding
import dev.staticvar.vlr.ui.helper.VLRTabIndicator
import kotlinx.coroutines.launch

@Composable
fun VlrTabRowForViewPager(modifier: Modifier, pagerState: PagerState, tabs: List<String>) {
  val scope = rememberCoroutineScope()
  TabRow(
    selectedTabIndex = pagerState.currentPage,
    indicator = { indicators -> VLRTabIndicator(indicators, pagerState.currentPage) }
  ) {
    tabs.forEachIndexed { index, title ->
      Tab(
        selected = pagerState.currentPage == index,
        onClick = { scope.launch { pagerState.scrollToPage(index) } }
      ) { Text(text = title, modifier = modifier.padding(Local16DPPadding.current)) }
    }
  }
}
