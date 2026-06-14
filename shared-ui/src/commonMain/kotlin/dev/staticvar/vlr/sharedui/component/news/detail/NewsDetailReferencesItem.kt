/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.divider.PrismDividerStyle
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.ArticleLink

/**
 * Compact article reference list.
 *
 * Links are rows instead of tags so long labels and URLs can truncate predictably on small screens.
 */
@Composable
public fun NewsDetailReferencesItem(
  links: List<ArticleLink>,
  modifier: Modifier = Modifier,
  onLinkSelected: ((ArticleLink) -> Unit)? = null,
) {
  if (links.isEmpty()) return

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    PrismSectionTitle(
      title = "References",
      preLabel = "media",
      trailing = {
        PrismTag(text = links.size.newsDetailReferenceCountLabel(), style = PrismTagStyle.Neutral)
      },
    )
    PrismSurface(
      modifier = Modifier.fillMaxWidth(),
      color = Prism.color.surfaceVariant,
      border = BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        links.forEachIndexed { index, link ->
          if (index > 0) {
            PrismDivider(style = PrismDividerStyle.Hairline)
          }
          NewsDetailReferenceRow(link = link, onLinkSelected = onLinkSelected)
        }
      }
    }
  }
}

@Composable
private fun NewsDetailReferenceRow(link: ArticleLink, onLinkSelected: ((ArticleLink) -> Unit)?) {
  val enabled = onLinkSelected != null && link.url.isNotBlank()
  val clickModifier = if (enabled && onLinkSelected != null) {
    Modifier.clickable(role = Role.Button) { onLinkSelected(link) }
  } else {
    Modifier
  }

  Row(
    modifier = clickModifier
      .fillMaxWidth()
      .heightIn(min = Prism.dimens.touchTargetMin)
      .padding(horizontal = Prism.dimens.spacingS, vertical = Prism.dimens.spacingXs),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = link.text.ifBlank { link.url.ifBlank { "Reference" } },
        style = Prism.typography.cardTitle,
        color = Prism.color.titleColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (link.url.isNotBlank()) {
        Text(
          text = link.url,
          modifier = Modifier.padding(top = Prism.dimens.spacingXs),
          style = Prism.typography.caption,
          color = Prism.color.labelColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    PrismTag(text = "open", style = PrismTagStyle.Info, enabled = enabled)
  }
}

private fun Int.newsDetailReferenceCountLabel(): String = when (this) {
  1 -> "1 link"
  else -> "$this links"
}
