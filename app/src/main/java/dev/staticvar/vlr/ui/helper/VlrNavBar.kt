package dev.staticvar.vlr.ui.helper

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun VlrNavBar(navController: NavController, items: List<NavItem>, isVisible: Boolean) {
  // https://issuetracker.google.com/issues/243852341#comment1
  AnimatedVisibility(
    visible = isVisible,
    enter =
    slideInVertically(
      // Enters by sliding up from offset 0 to fullHeight.
      initialOffsetY = { fullHeight -> fullHeight },
      animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
    ),
    exit =
    slideOutVertically(
      // Exits by sliding up from offset 0 to -fullHeight.
      targetOffsetY = { fullHeight -> fullHeight },
      animationSpec = tween(durationMillis = 400, easing = FastOutLinearInEasing),
    ),
    modifier = Modifier,
    label = ""
  ) {
    Column(modifier = Modifier) {
      NavigationBar(
        tonalElevation = 8.dp,
        modifier = Modifier.animateContentSize()
      ) {
        items.forEach { navItem ->
          val isCurrentDestination = navController.currentDestination?.route == navItem.route
          NavigationBarItem(
            selected = isCurrentDestination,
            icon = {
              Crossfade(isCurrentDestination, label = "NavBarItemIcon") {
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
}

@Stable
data class NavItem(
  val title: String,
  val route: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val onClick: () -> Unit
)
