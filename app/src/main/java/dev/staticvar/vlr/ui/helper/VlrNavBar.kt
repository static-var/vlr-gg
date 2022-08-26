package dev.staticvar.vlr.ui.helper

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun NewNavBar(navController: NavHostController, items: List<NavItem>, currentNav: String) {
  AnimatedVisibility(
    visible = items.any { it.route == (navController.currentDestination?.route ?: "") }
  ) {
    NavigationBar(tonalElevation = 8.dp, modifier = Modifier.navigationBarsPadding()) {
      items.forEach { navItem ->
        NavigationBarItem(
          selected = navController.currentDestination?.route == navItem.route,
          icon = {
            Crossfade(currentNav == navItem.route) {
              Icon(
                imageVector = if (it) navItem.selectedIcon else navItem.unselectedIcon,
                contentDescription = navItem.title,
                tint = VLRTheme.colorScheme.onPrimaryContainer
              )
            }
          },
          label = { Text(text = navItem.title) },
          onClick = navItem.onClick
        )
      }
    }
  }
}

@Stable
data class NavItem(
  val title: String,
  val route: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val onClick: () -> Unit
)
