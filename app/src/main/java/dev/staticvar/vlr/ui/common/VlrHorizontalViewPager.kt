package dev.staticvar.vlr.ui.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun VlrHorizontalViewPager(
  modifier: Modifier,
  pagerState: PagerState,
  vararg contents: @Composable () -> Unit
) {
  HorizontalPager(state = pagerState, modifier = modifier.fillMaxSize(), beyondViewportPageCount = 1) {
    tabPosition ->
    contents[tabPosition]()
  }
}
