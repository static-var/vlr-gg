package dev.staticvar.vlr.ui.helper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun VLRTabIndicator(indicators: List<TabPosition>, position: Int) {
  Box(
    modifier =
      Modifier.tabIndicatorOffset(indicators[position])
        .fillMaxWidth()
        .height(12.dp)
        .padding(horizontal = 24.dp, vertical = 4.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(color = VLRTheme.colorScheme.primary)
  )
}
