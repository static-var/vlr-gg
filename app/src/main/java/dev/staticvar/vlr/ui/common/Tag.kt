package dev.staticvar.vlr.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.staticvar.vlr.R
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun Tag(modifier: Modifier = Modifier, text: String, icon: ImageVector?) {
  Row(modifier = modifier) {
    icon?.let {
      Icon(
        modifier = Modifier.size(16.dp).padding(end = 4.dp),
        imageVector = it,
        contentDescription = stringResource(R.string.get_notified),
        tint = VLRTheme.colorScheme.primary
      )
    }
    Text(
      modifier = Modifier
        .clip(RoundedCornerShape(4.dp))
        .background(VLRTheme.colorScheme.primary)
        .padding(vertical = 2.dp, horizontal = 4.dp),
      text = text,
      style = VLRTheme.typography.bodySmall,
      color = VLRTheme.colorScheme.onPrimary
    )
  }

}