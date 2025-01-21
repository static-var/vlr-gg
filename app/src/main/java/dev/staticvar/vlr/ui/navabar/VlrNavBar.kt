package dev.staticvar.vlr.ui.navabar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.staticvar.vlr.R
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.ui.theme.transparent

@Composable
fun VlrNavBar(
  navController: NavController,
  items: List<NavItem>,
  hazeState: HazeState,
  isVisible: Boolean,
  topSlotSelectedItem: Int? = null,
  topSlotAction: ((Int) -> Unit)? = null,
) {
  val tweenAnimSpec = tween<Float>(600)
  // https://issuetracker.google.com/issues/243852341#comment1
  NavBarAnimatedVisibility(isVisible = isVisible) {
    FloatingNavigationBar(
      tonalElevation = 16.dp,
      modifier =
        Modifier
          .padding(horizontal = 16.dp)
          .navigationBarsPadding()
          .clip(VLRTheme.shapes.extraLarge)
          .hazeEffect(
            state = hazeState,
            style = HazeMaterials.ultraThin()
          )
          .border(
            width = 0.5.dp,
            brush = Brush.verticalGradient(
              colors = listOf(
                VLRTheme.colorScheme.primary,
                VLRTheme.colorScheme.transparent,
              )
            ), shape = VLRTheme.shapes.extraLarge
          ),
      containerColor = Color.Transparent,
    ) {
      var topSlot: TopSlot? by remember { mutableStateOf(null) }

      AnimatedContent(
        targetState = topSlot,
        transitionSpec = {
          scaleIn(tweenAnimSpec) + fadeIn(tweenAnimSpec) togetherWith scaleOut(
            tweenAnimSpec
          ) + fadeOut(tweenAnimSpec)
        },
      ) {
        when (it) {
          TopSlot.MATCH ->
            MatchTopSlot(currentItem = topSlotSelectedItem ?: 0) { index ->
              topSlotAction?.invoke(index)
            }

          TopSlot.EVENT ->
            EventTopSlot(currentItem = topSlotSelectedItem ?: 0) { index ->
              topSlotAction?.invoke(index)
            }

          else -> {}
        }
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items.forEach { navItem ->
          val isCurrentDestination = navController.currentDestination?.route == navItem.route
          if (isCurrentDestination) {
            topSlot = navItem.topSlot
          }

          NavigationBarItem(
            selected = isCurrentDestination,
            icon = {
              Crossfade(isCurrentDestination, label = "NavBarItemIcon") {
                Icon(
                  imageVector = if (it) navItem.selectedIcon else navItem.unselectedIcon,
                  contentDescription = navItem.title,
                  tint = if(it) VLRTheme.colorScheme.onPrimaryContainer else VLRTheme.colorScheme.primaryContainer,
                )
              }
            },
            label = { Text(text = navItem.title) },
            onClick = navItem.onClick,
            modifier = Modifier.alpha(if (isCurrentDestination) 1f else 0.7f),
            colors =
              NavigationBarItemDefaults.colors(selectedTextColor = VLRTheme.colorScheme.primary, indicatorColor = VLRTheme.colorScheme.primary),
          )
        }
      }
    }
  }
}

@Composable
fun MatchTopSlot(modifier: Modifier = Modifier, currentItem: Int, action: (Int) -> Unit) {
  val tabs =
    listOf(
      stringResource(id = R.string.live),
      stringResource(id = R.string.upcoming),
      stringResource(id = R.string.completed),
    )
  TopSlotContainer(modifier = modifier, tabs = tabs, currentItem = currentItem, action = action)
}

@Composable
fun EventTopSlot(modifier: Modifier = Modifier, currentItem: Int, action: (Int) -> Unit) {
  val tabs =
    listOf(
      stringResource(id = R.string.ongoing),
      stringResource(id = R.string.upcoming),
      stringResource(id = R.string.completed),
    )
  TopSlotContainer(modifier = modifier, tabs = tabs, currentItem = currentItem, action = action)
}

@Composable
fun TopSlotContainer(
  modifier: Modifier = Modifier,
  tabs: List<String>,
  currentItem: Int,
  action: (Int) -> Unit,
) {
  var localItem by remember(currentItem) { mutableIntStateOf(currentItem) }
  Row(
    modifier = Modifier
      .padding(top = 4.dp)
      .fillMaxWidth()
      .selectableGroup(),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    tabs.forEachIndexed { index, tab ->
      val isSelected = localItem == index
      Button(
        modifier = modifier
          .alpha(if (isSelected) 1f else 0.7f)
          .weight(1f),
        onClick = {
          localItem = index
          action(index)
        },
        colors =
          if (isSelected) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
      ) {
        Text(text = tab, maxLines = 1)
      }
    }
  }
}
