package dev.staticvar.vlr.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun DateChip(modifier: Modifier = Modifier, date: String) {
  Column(
    modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    ElevatedAssistChip(
      onClick = {},
      label = {
        Text(
          date,
          textAlign = TextAlign.Center,
        )
      },
      colors =
        AssistChipDefaults.assistChipColors(
          containerColor = VLRTheme.colorScheme.primaryContainer,
          labelColor = VLRTheme.colorScheme.onPrimaryContainer,
        ),
      //      leadingIcon = { Icon(imageVector = Icons.Outlined.CalendarMonth, contentDescription =
      // date, tint = VLRTheme.colorScheme.onPrimaryContainer,) },
      shape = RoundedCornerShape(16.dp)
    )
  }
}
