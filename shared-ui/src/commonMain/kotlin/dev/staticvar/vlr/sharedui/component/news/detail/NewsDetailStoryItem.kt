/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.NewsArticle

/** Adds individually laid out article blocks in document order. */
public fun LazyListScope.newsDetailStoryItems(article: NewsArticle) {
  itemsIndexed(
    items = newsDetailContentBlocks(article),
    key = { index, _ -> "article-${article.id}-$index" },
    contentType = { _, block -> block::class.simpleName },
  ) { _, block ->
    NewsDetailArticleContent(block)
  }
}

@Composable
public fun NewsDetailStoryItem(article: NewsArticle, modifier: Modifier = Modifier) {
  val blocks = remember(article) { newsDetailContentBlocks(article) }
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    blocks.forEach { block -> NewsDetailArticleContent(block) }
  }
}

@Composable
private fun NewsDetailArticleContent(
  block: NewsDetailContentBlock,
) {
  when (block) {
    is NewsDetailContentBlock.Text -> ArticleText(block.runs)

    is NewsDetailContentBlock.Heading -> ArticleText(
      block.runs,
      modifier = Modifier.semantics { heading() },
      style = when (block.level) {
        1 -> Prism.typography.headline
        2 -> Prism.typography.sectionTitle
        else -> Prism.typography.cardTitle
      },
    )

    is NewsDetailContentBlock.Caption -> ArticleText(block.runs, style = Prism.typography.caption)

    is NewsDetailContentBlock.Quote -> ArticleChildren(
      block.children,
      Modifier.background(Prism.color.surfaceVariant).padding(Prism.dimens.spacingM),
    )

    is NewsDetailContentBlock.ListItem -> ArticleChildren(block.children)

    is NewsDetailContentBlock.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      block.children.forEachIndexed { index, child ->
        Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
          Text(
            if (block.ordered) "${block.start + index}." else "•",
            style = Prism.typography.bodyLarge,
            color = Prism.color.bodyColor,
          )
          Box(Modifier.weight(1f)) { NewsDetailArticleContent(child) }
        }
      }
    }

    is NewsDetailContentBlock.Image -> ArticleImage(block.url, block.alt)

    is NewsDetailContentBlock.Video -> ArticleMediaLink(block.url, "Watch video")
  }
}

@Composable
private fun ArticleChildren(
  children: List<NewsDetailContentBlock>,
  modifier: Modifier = Modifier,
) {
  Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
    children.forEach { child -> NewsDetailArticleContent(child) }
  }
}

@Composable
private fun ArticleText(
  runs: List<NewsDetailTextRun>,
  modifier: Modifier = Modifier,
  style: TextStyle = Prism.typography.bodyLarge,
  textAlign: TextAlign = TextAlign.Start,
) {
  val linkStyle = TextLinkStyles(
    SpanStyle(color = Prism.color.titleColor, textDecoration = TextDecoration.Underline),
  )
  val text = buildAnnotatedString {
    runs.forEach { run ->
      withStyle(
        SpanStyle(
          fontWeight = if (run.bold) FontWeight.Bold else null,
          fontStyle = if (run.italic) FontStyle.Italic else null,
        ),
      ) {
        if (run.url != null) {
          withLink(LinkAnnotation.Url(run.url, linkStyle)) { append(run.text) }
        } else {
          append(run.text)
        }
      }
    }
  }

  Text(
    text = text,
    modifier = modifier.fillMaxWidth(),
    style = style,
    color = Prism.color.bodyColor,
    textAlign = textAlign,
  )
}

@Composable
private fun ArticleImage(url: String, alt: String?) {
  val uriHandler = LocalUriHandler.current
  val painter = rememberAsyncImagePainter(url)
  val state by painter.state.collectAsState()
  val loadedImage = (state as? AsyncImagePainter.State.Success)?.result?.image
  val width = loadedImage?.width ?: 16
  val height = loadedImage?.height ?: 9
  val ratio = if (width > 0 && height > 0) width.toFloat() / height else 16f / 9f
  if (state is AsyncImagePainter.State.Error) {
    ArticleMediaLink(url, "Open article image")
  } else {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(ratio)
        .background(Prism.color.surfaceVariant)
        .clickable(role = Role.Button, onClickLabel = "Open full image") { uriHandler.openUri(url) },
      contentAlignment = Alignment.Center,
    ) {
      Image(
        painter = painter,
        contentDescription = alt?.takeIf { it.isNotBlank() } ?: "Article image",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
      )
      if (state !is AsyncImagePainter.State.Success) {
        Text("Loading image", style = Prism.typography.caption, color = Prism.color.labelColor)
      }
    }
  }
}

@Composable
private fun ArticleMediaLink(url: String, label: String) {
  val uriHandler = LocalUriHandler.current
  PrismButton(
    onClick = { uriHandler.openUri(url) },
    style = PrismButtonStyle.Secondary,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(label)
  }
}
