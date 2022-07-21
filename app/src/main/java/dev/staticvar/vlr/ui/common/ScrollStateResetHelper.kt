package dev.staticvar.vlr.ui.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.withContext

@Composable
fun LazyListState.ScrollHelper(resetScroll: Boolean, postScroll: () -> Unit) {
  val coroutineScope = rememberCoroutineScope()
  LaunchedEffect(key1 = resetScroll) {
    if (resetScroll) {
      withContext(coroutineScope.coroutineContext) { animateScrollToItem(0) }
      postScroll()
    }
  }
}
