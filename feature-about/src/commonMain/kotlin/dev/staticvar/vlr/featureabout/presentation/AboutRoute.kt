/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.prism.Prism

@Composable
public fun AboutRoute(modifier: Modifier = Modifier) {
  AboutScreen(modifier = modifier)
}

@Composable
internal fun AboutScreen(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = "About VLR",
      subtitle = "What this app is, where the data comes from, and why the UI looks like it means business.",
    )

    AboutSection(
      title = "Product",
      preLabel = "app",
      lines =
      listOf(
        "Cross-platform VLR companion built with Kotlin Multiplatform and Compose.",
        "Shared domain/data layers feed match, event, ranking, team, player, and news experiences.",
        "Large-screen navigation is designed to scale beyond phone-sized assumptions.",
      ),
    )

    AboutSection(
      title = "Android",
      preLabel = "credits",
      lines =
      listOf(
        "staticvar.dev",
        "github.com/static-var",
        "github.com/static-var/vlr-gg",
        "github.com/sponsors/static-var",
      ),
    )

    AboutSection(
      title = "Backend",
      preLabel = "data",
      lines =
      listOf(
        "akhilnarang.dev",
        "github.com/akhilnarang",
        "github.com/akhilnarang/vlrgg-scraper",
        "API host: vlr-scraper.akhilnarang.dev",
      ),
    )

    PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
      PrismSectionTitle(title = "Design principles", preLabel = "ui")
      Text(
        text = "Flat surfaces. Sharp borders. High contrast. Minimal ceremony. If a component needs a motivational speech before it makes sense, it probably shouldn’t exist.",
        modifier = Modifier.padding(top = Prism.dimens.spacingS),
        style = Prism.typography.bodyLarge,
        color = Prism.color.bodyColor,
      )
    }
  }
}

@Composable
private fun AboutSection(title: String, preLabel: String, lines: List<String>) {
  PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
    PrismSectionTitle(title = title, preLabel = preLabel)
    lines.forEachIndexed { index, line ->
      Text(
        text = line,
        modifier = Modifier.padding(top = if (index == 0) Prism.dimens.spacingS else Prism.dimens.spacingXs),
        style = Prism.typography.bodySmall,
        color = Prism.color.bodyColor,
      )
    }
  }
}
