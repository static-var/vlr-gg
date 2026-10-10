/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Section header with an optional accent prefix, a title, and right-aligned trailing content.
 *
 * [style] owns the visuals. The default [PrismSectionTitleStyle.Tab] draws the prefix and title in
 * an inverted tab on a full-width rule, and adapts its shape, type, and shadow to the active theme
 * family. Use [PrismSectionTitleStyle.Subtle] for groups nested under a tabbed section.
 *
 * @param title Section name. It is announced as a heading together with [prefix].
 * @param prefix Optional short label shown before the title in the style's prefix color, such as an
 * index or a stage.
 * @param trailing Optional content aligned to the end of the header. Text inside it defaults to the
 * style's trailing text style and color.
 */
@Composable
public fun PrismSectionTitle(
  title: String,
  modifier: Modifier = Modifier,
  prefix: String? = null,
  style: PrismSectionTitleStyle = PrismSectionTitleStyle.Tab,
  trailing: (@Composable RowScope.() -> Unit)? = null,
) {
  val hasRule = style.ruleThickness > 0.dp
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      PrismSectionTitleTab(
        title = title,
        prefix = prefix,
        style = style,
        modifier = Modifier.weight(1f, fill = false)
          .padding(end = if (trailing != null) Prism.dimens.spacingS else 0.dp),
      )
      if (trailing != null) {
        CompositionLocalProvider(
          LocalContentColor provides style.trailingContentColor,
          LocalTextStyle provides style.trailingTextStyle,
        ) {
          Row(
            modifier = Modifier.padding(bottom = if (hasRule) Prism.dimens.spacingXs else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS, Alignment.End),
            content = trailing,
          )
        }
      }
    }
    if (hasRule) {
      Box(
        modifier = Modifier.fillMaxWidth().height(style.ruleThickness).background(style.ruleColor),
      )
    }
  }
}

@Composable
private fun PrismSectionTitleTab(
  title: String,
  prefix: String?,
  style: PrismSectionTitleStyle,
  modifier: Modifier = Modifier,
) {
  PrismSurface(
    modifier = modifier.semantics(mergeDescendants = true) { heading() },
    color = style.tabColor,
    contentColor = style.titleColor,
    shape = style.tabShape,
    frame = style.tabFrame,
  ) {
    Row(
      modifier = Modifier.padding(style.tabPadding),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      if (prefix != null) {
        Text(
          text = if (style.uppercase) prefix.uppercase() else prefix,
          style = style.titleTextStyle,
          color = style.prefixColor,
          maxLines = 1,
        )
      }
      Text(
        text = if (style.uppercase) title.uppercase() else title,
        modifier = Modifier.weight(1f, fill = false),
        style = style.titleTextStyle,
        color = style.titleColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
