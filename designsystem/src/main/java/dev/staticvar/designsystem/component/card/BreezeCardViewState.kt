package dev.staticvar.designsystem.component.card

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

data class BreezeCardViewState(
  val content: @Composable ColumnScope.() -> Unit = {},
  val style: BreezeCardStyle = BreezeCardStyle.Filled,
)
