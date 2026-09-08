/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.sharedui.mascot.LynxMascot
import dev.staticvar.vlr.sharedui.mascot.RosieMascot

@Composable
public fun AboutRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
  AboutScreen(onBack = onBack, modifier = modifier)
}

@Composable
internal fun AboutScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
      PrismScreenTitleBar(
        title = "About",
        onBackPress = onBack,
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
      Column(
        modifier = Modifier
          .weight(1f)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = Prism.dimens.spacingM)
          .padding(bottom = Prism.dimens.spacingXl),
        verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXl),
      ) {
        AboutIdentity()
        AboutIntroduction()
        AboutProjectLinks()
        AboutContributors()
        AboutDonations()
        AboutSource()
      }
    }
  }
}

@Composable
private fun AboutIdentity() {
  PrismSurface(
    modifier = Modifier.fillMaxWidth(),
    color = Prism.color.accentSubtle,
    contentColor = Prism.color.titleColor,
    shape = Prism.shapes.medium,
  ) {
    Column(
      modifier = Modifier.padding(Prism.dimens.spacingL),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingL),
    ) {
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        itemVerticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "VLR",
          style = Prism.typography.display.copy(fontSize = 72.sp, lineHeight = 76.sp, letterSpacing = (-3).sp),
          color = Prism.color.accent,
        )
        Row {
          LynxMascot(modifier = Modifier.size(88.dp), animated = true)
          RosieMascot(modifier = Modifier.size(88.dp), animated = true)
        }
      }
      Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        Text(
          text = "For the matches you care about.",
          style = Prism.typography.headline,
          color = Prism.color.titleColor,
          modifier = Modifier.semantics { heading() },
        )
        Text(
          text = "An independent VALORANT esports app",
          style = Prism.typography.label,
          color = Prism.color.bodyColor,
        )
      }
    }
  }
}

@Composable
private fun AboutIntroduction() {
  Text(
    text = "See who's playing next, catch up on results, and dig into the rounds that decided a match. Follow your favorite teams and players through the season.",
    style = Prism.typography.bodyLarge,
    color = Prism.color.bodyColor,
  )
}

@Composable
private fun AboutProjectLinks() {
  Column {
    AboutSectionHeading("Make it better")
    AboutLink(
      title = "Report an issue",
      description = "Something off? Help us get it right.",
      url = "https://github.com/static-var/vlr-gg/issues",
    )
    PrismDivider()
    AboutLink(
      title = "App source code",
      description = "Read the code or contribute on GitHub.",
      url = "https://github.com/static-var/vlr-gg",
    )
    PrismDivider()
    AboutLink(
      title = "Backend source code",
      description = "Explore the service that powers the app.",
      url = "https://github.com/akhilnarang/vlrgg-scraper",
    )
  }
}

@Composable
private fun AboutContributors() {
  Column {
    AboutSectionHeading("The people behind it")
    AboutLink(
      title = "Shreyansh Lodha",
      description = "App design & development · staticvar",
      url = "https://staticvar.dev",
    )
    PrismDivider()
    AboutLink(
      title = "Akhil Narang",
      description = "Backend developer",
      url = "https://akhilnarang.dev",
    )
  }
}

@Composable
private fun AboutDonations() {
  Column {
    AboutSectionHeading("Support the project")
    AboutLink(
      title = "Donate via GitHub Sponsors",
      description = "Help support continued development.",
      url = "https://github.com/sponsors/static-var",
    )
  }
}

@Composable
private fun AboutSource() {
  PrismSurface(
    modifier = Modifier.fillMaxWidth(),
    color = Prism.color.surfaceVariant,
    shape = Prism.shapes.medium,
  ) {
    Column(modifier = Modifier.padding(Prism.dimens.spacingM)) {
      Text(
        text = "From the scene, for the scene.",
        style = Prism.typography.cardTitle,
        color = Prism.color.titleColor,
        modifier = Modifier.semantics { heading() },
      )
      Text(
        text = "Match coverage, statistics, and news come from VLR.gg. This is an unofficial fan project, unaffiliated with VLR.gg or Riot Games.",
        modifier = Modifier.padding(top = Prism.dimens.spacingS),
        style = Prism.typography.bodySmall,
        color = Prism.color.bodyColor,
      )
      AboutLink(title = "Visit VLR.gg", url = "https://www.vlr.gg")
    }
  }
}

@Composable
private fun AboutSectionHeading(title: String) {
  Text(
    text = title,
    modifier = Modifier.padding(bottom = Prism.dimens.spacingS).semantics { heading() },
    style = Prism.typography.sectionTitle,
    color = Prism.color.titleColor,
  )
}

@Composable
private fun AboutLink(title: String, url: String, description: String? = null) {
  val uriHandler = LocalUriHandler.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(role = Role.Button, onClickLabel = "Open $title in browser") { uriHandler.openUri(url) }
      .heightIn(min = Prism.dimens.touchTargetMin)
      .padding(vertical = Prism.dimens.spacingM),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
      Text(text = title, style = Prism.typography.cardTitle, color = Prism.color.accent)
      description?.let {
        Text(text = it, style = Prism.typography.bodySmall, color = Prism.color.bodyColor)
      }
    }
    AboutExternalLinkArrow()
  }
}

@Composable
private fun AboutExternalLinkArrow() {
  val color = Prism.color.accent
  Canvas(modifier = Modifier.size(Prism.dimens.iconS)) {
    val inset = size.width * 0.2f
    val end = size.width - inset
    val strokeWidth = 1.5.dp.toPx()
    drawLine(color, Offset(inset, end), Offset(end, inset), strokeWidth)
    drawLine(color, Offset(inset, inset), Offset(end, inset), strokeWidth)
    drawLine(color, Offset(end, inset), Offset(end, end), strokeWidth)
  }
}
