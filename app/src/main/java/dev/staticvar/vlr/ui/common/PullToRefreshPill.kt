package dev.staticvar.vlr.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun BoxScope.PullToRefreshPill(modifier: Modifier = Modifier, show: Boolean) {
  AnimatedVisibility(
    visible = show,
    modifier = Modifier.align(Alignment.TopCenter).zIndex(1f),
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically(),
  ) {
    Surface(
      modifier = modifier,
      shape = VLRTheme.shapes.extraLarge,
      color = VLRTheme.colorScheme.primary,
      contentColor = VLRTheme.colorScheme.onPrimary,
    ) {
      Row(
        modifier =
          Modifier.wrapContentSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("common:loader"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Text(
          "Fetching latest data",
          style = VLRTheme.typography.labelLarge,
          modifier = Modifier.padding(end = 8.dp),
        )
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          color = VLRTheme.colorScheme.onPrimary,
          strokeWidth = 2.dp,
          strokeCap = StrokeCap.Round,
        )
      }
    }
  }
}
