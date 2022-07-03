package dev.staticvar.vlr.ui.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState

@Composable
fun VlrHorizontalViewPager(
    modifier: Modifier,
    pagerState: PagerState,
    vararg contents: @Composable () -> Unit
) {
    HorizontalPager(count = contents.size, state = pagerState, modifier = modifier.fillMaxSize()) {
            tabPosition ->
        contents[tabPosition]()
    }
}