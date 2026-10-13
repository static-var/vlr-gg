/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.section

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntSize
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Compact outlined action for the trailing content of a [PrismSectionTitle].
 *
 * It uses the title's type and padding, so it never makes the header taller than the tab and
 * tracks the tab's height at every font scale. The visible box is shorter than 48 dp, but it
 * reserves no extra layout space: Compose extends touches near a small clickable to the platform
 * minimum touch target, so taps still land on a 48 dp area. Width changes, such as a toggled
 * label, animate with the theme's standard motion.
 *
 * @param text Visible label. Add a longer spoken label with `Modifier.semantics` when the short
 * text needs context.
 * @param style Must match the [PrismSectionTitle] style the action sits in.
 */
@Composable
public fun PrismSectionTitleAction(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  style: PrismSectionTitleStyle = PrismSectionTitleStyle.Tab,
) {
  val shape = Prism.shapes.small
  val animation = Prism.anim.standard
  PrismSurface(
    modifier = modifier
      .animateContentSize(tween<IntSize>(durationMillis = animation.durationMillis, easing = animation.easing))
      .clip(shape)
      .clickable(
        enabled = enabled,
        role = Role.Button,
        indication = ripple(),
        interactionSource = null,
        onClick = onClick,
      ),
    color = Prism.color.background,
    contentColor = if (enabled) Prism.color.titleColor else Prism.color.labelColor,
    shape = shape,
    border = BorderStroke(
      width = Prism.dimens.strokeDefault,
      color = if (enabled) Prism.color.stroke else Prism.color.strokeVariant,
    ),
  ) {
    Text(
      text = if (style.uppercase) text.uppercase() else text,
      modifier = Modifier.padding(style.actionPadding),
      style = style.titleTextStyle,
      maxLines = 1,
    )
  }
}
