package dev.staticvar.vlr.ui.helper

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun VlrNavBar(
  navController: NavController,
  items: List<NavItem>,
  hazeState: HazeState,
  isVisible: Boolean,
) {
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
    label = "",
  ) {
    Box(
      modifier =
      Modifier
        .padding(bottom = 8.dp)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .fillMaxWidth()
    ) {
      FloatingNavigationBar(
        tonalElevation = 8.dp,
        modifier = Modifier
          .animateContentSize()
          .padding(horizontal = 32.dp),
        containerColor = Color.Transparent,
        hazeState = hazeState,
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
                  tint = VLRTheme.colorScheme.onPrimaryContainer,
                )
              }
            },
            label = { Text(text = navItem.title) },
            onClick = navItem.onClick,
          )
        }
      }
    }
  }
}

@Composable
fun FloatingNavigationBar(
  modifier: Modifier = Modifier,
  shape: Shape = MaterialTheme.shapes.extraLarge,
  containerColor: Color = NavigationBarDefaults.containerColor,
  contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
  tonalElevation: Dp = NavigationBarDefaults.Elevation,
  hazeState: HazeState,
  content: @Composable RowScope.() -> Unit,
) {
  Surface(
    color = containerColor,
    contentColor = contentColor,
    tonalElevation = tonalElevation,
    shape = shape,
    border =
      BorderStroke(
        width = 0.5.dp,
        brush =
          Brush.verticalGradient(
            colors =
              listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
              )
          ),
      ),
    modifier =
      modifier.hazeChild(
        state = hazeState,
        style = HazeMaterials.thin(),
        shape = MaterialTheme.shapes.extraLarge,
      ),
  ) {
    Row(
      modifier = Modifier
        .padding(horizontal = 8.dp)
        .fillMaxWidth()
        .height(80.dp)
        .selectableGroup(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      content = content,
    )
  }
}

@Stable
data class NavItem(
  val title: String,
  val route: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val onClick: () -> Unit,
)
