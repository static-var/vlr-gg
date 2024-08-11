package dev.staticvar.vlr.ui.navabar

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector

@Stable
data class NavItem(
  val title: String,
  val route: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val onClick: () -> Unit,
  val topSlot: TopSlot? = null,
)
