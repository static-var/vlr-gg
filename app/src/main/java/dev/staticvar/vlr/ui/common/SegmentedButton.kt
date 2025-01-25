package dev.staticvar.vlr.ui.common

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun VlrSegmentedButtons(
  modifier: Modifier = Modifier,
  highlighted: Int,
  items: List<String>,
  selected: (String, Int) -> Unit,
) {
  SingleChoiceSegmentedButtonRow(modifier = modifier) {
    items.forEachIndexed { index, item ->
      SegmentedButton(
        modifier =
          Modifier.clip(SegmentedButtonDefaults.itemShape(index = index, count = items.size)),
        selected = highlighted == index,
        onClick = { selected(item, index) },
        shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
        icon = {},
        colors = SegmentedButtonDefaults.colors(
          inactiveContentColor = VLRTheme.colorScheme.onSurface,
          inactiveContainerColor = VLRTheme.colorScheme.surfaceContainer,
          activeContentColor = VLRTheme.colorScheme.onPrimaryContainer,
          activeContainerColor = VLRTheme.colorScheme.primaryContainer,
          inactiveBorderColor = VLRTheme.colorScheme.primary,
          activeBorderColor = VLRTheme.colorScheme.primary
        )
      ) {
        Text(text = item)
      }
    }
  }
}
