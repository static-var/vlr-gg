package dev.staticvar.vlr.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun DateChip(modifier: Modifier = Modifier, date: String) {
  Column(
    modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    SuggestionChip(
      onClick = {},
      label = {
        Text(
          text = date,
          textAlign = TextAlign.Center,
          color = VLRTheme.colorScheme.onPrimaryContainer,
        )
      },
      shape = VLRTheme.shapes.small,
      colors = SuggestionChipDefaults.suggestionChipColors(
        containerColor = VLRTheme.colorScheme.primaryContainer
      ),
      border = SuggestionChipDefaults.suggestionChipBorder(
        enabled = true,
        borderColor = VLRTheme.colorScheme.primary
      ),
    )
  }
}
