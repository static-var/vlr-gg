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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import dev.staticvar.designsystem.component.card.PrismCardHost
import dev.staticvar.designsystem.component.card.cardMascotEligible
import dev.staticvar.designsystem.component.card.cardMascotViewport
import dev.staticvar.designsystem.component.divider.PrismDivider
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.telemetry.AppTelemetry
import dev.staticvar.vlr.sharedui.mascot.LynxMascot
import dev.staticvar.vlr.sharedui.mascot.RosieMascot
import org.jetbrains.compose.resources.stringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.about
import vlr.feature_about.generated.resources.about_data_attribution
import vlr.feature_about.generated.resources.about_description
import vlr.feature_about.generated.resources.akhil_narang
import vlr.feature_about.generated.resources.an_independent_valorant_esports_app
import vlr.feature_about.generated.resources.app_design_development_staticvar
import vlr.feature_about.generated.resources.app_source_code
import vlr.feature_about.generated.resources.backend_developer
import vlr.feature_about.generated.resources.backend_source_code
import vlr.feature_about.generated.resources.donate_via_github_sponsors
import vlr.feature_about.generated.resources.explore_the_service_that_powers_the_app
import vlr.feature_about.generated.resources.for_the_matches_you_care_about
import vlr.feature_about.generated.resources.from_the_scene_for_the_scene
import vlr.feature_about.generated.resources.help_support_continued_development
import vlr.feature_about.generated.resources.make_it_better
import vlr.feature_about.generated.resources.open_link_in_browser
import vlr.feature_about.generated.resources.privacy_policy
import vlr.feature_about.generated.resources.read_the_code_or_contribute_on_github
import vlr.feature_about.generated.resources.report_an_issue
import vlr.feature_about.generated.resources.send_feedback
import vlr.feature_about.generated.resources.shreyansh_lodha
import vlr.feature_about.generated.resources.share_a_problem_or_an_idea_with_the_developer
import vlr.feature_about.generated.resources.something_off_help_us_get_it_right
import vlr.feature_about.generated.resources.support_the_project
import vlr.feature_about.generated.resources.terms_of_service
import vlr.feature_about.generated.resources.the_people_behind_it
import vlr.feature_about.generated.resources.val_esports
import vlr.feature_about.generated.resources.visit_val_esports
import vlr.feature_about.generated.resources.visit_vlr_gg
import vlr.feature_about.generated.resources.website_policies
import vlr.feature_about.generated.resources.write_feedback

@Composable
public fun AboutRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
  AboutScreen(onBack = onBack, modifier = modifier)
}

@Composable
internal fun AboutScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
  var showFeedback by rememberSaveable { mutableStateOf(false) }
  if (showFeedback) {
    AboutFeedbackDialog(
      onDismiss = { showFeedback = false },
      onSubmit = AppTelemetry::submitFeedback,
    )
  }
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
      PrismScreenTitleBar(
        title = stringResource(Res.string.about),
        onBackPress = onBack,
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
      Column(
        modifier = Modifier
          .weight(1f)
          .cardMascotViewport()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = Prism.dimens.spacingM)
          .padding(bottom = Prism.dimens.spacingXl),
        verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXl),
      ) {
        AboutIdentity()
        AboutIntroduction()
        AboutWebsiteLinks()
        AboutProjectLinks(onFeedback = { showFeedback = true })
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
          text = stringResource(Res.string.val_esports),
          style = Prism.typography.display.copy(fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1).sp),
          color = Prism.color.accent,
        )
        Row {
          LynxMascot(modifier = Modifier.size(88.dp), animated = true)
          RosieMascot(modifier = Modifier.size(88.dp), animated = true)
        }
      }
      Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
        Text(
          text = stringResource(Res.string.for_the_matches_you_care_about),
          style = Prism.typography.headline,
          color = Prism.color.titleColor,
          modifier = Modifier.semantics { heading() },
        )
        Text(
          text = stringResource(Res.string.an_independent_valorant_esports_app),
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
    text = stringResource(Res.string.about_description),
    style = Prism.typography.bodyLarge,
    color = Prism.color.bodyColor,
  )
}

@Composable
private fun AboutWebsiteLinks() {
  Column {
    AboutSectionHeading(stringResource(Res.string.website_policies))
    AboutLink(title = stringResource(Res.string.visit_val_esports), url = AppWebsite.Home)
    PrismDivider()
    AboutLink(title = stringResource(Res.string.privacy_policy), url = AppWebsite.Privacy)
    PrismDivider()
    AboutLink(title = stringResource(Res.string.terms_of_service), url = AppWebsite.Terms)
  }
}

@Composable
private fun AboutProjectLinks(onFeedback: () -> Unit) {
  Column {
    AboutSectionHeading(stringResource(Res.string.make_it_better))
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(role = Role.Button, onClickLabel = stringResource(Res.string.write_feedback), onClick = onFeedback)
        .heightIn(min = Prism.dimens.touchTargetMin)
        .padding(vertical = Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
    ) {
      Text(text = stringResource(Res.string.send_feedback), style = Prism.typography.cardTitle, color = Prism.color.accent)
      Text(
        text = stringResource(Res.string.share_a_problem_or_an_idea_with_the_developer),
        style = Prism.typography.bodySmall,
        color = Prism.color.bodyColor,
      )
    }
    PrismDivider()
    AboutLink(
      title = stringResource(Res.string.report_an_issue),
      description = stringResource(Res.string.something_off_help_us_get_it_right),
      url = "https://github.com/static-var/vlr-gg/issues",
    )
    PrismDivider()
    AboutLink(
      title = stringResource(Res.string.app_source_code),
      description = stringResource(Res.string.read_the_code_or_contribute_on_github),
      url = "https://github.com/static-var/vlr-gg",
    )
    PrismDivider()
    AboutLink(
      title = stringResource(Res.string.backend_source_code),
      description = stringResource(Res.string.explore_the_service_that_powers_the_app),
      url = "https://github.com/akhilnarang/vlrgg-scraper",
    )
  }
}

@Composable
private fun AboutContributors() {
  Column {
    AboutSectionHeading(stringResource(Res.string.the_people_behind_it))
    AboutLink(
      title = stringResource(Res.string.shreyansh_lodha),
      description = stringResource(Res.string.app_design_development_staticvar),
      url = "https://staticvar.dev",
    )
    PrismDivider()
    AboutLink(
      title = stringResource(Res.string.akhil_narang),
      description = stringResource(Res.string.backend_developer),
      url = "https://akhilnarang.dev",
    )
  }
}

@Composable
private fun AboutDonations() {
  Column {
    AboutSectionHeading(stringResource(Res.string.support_the_project))
    AboutLink(
      title = stringResource(Res.string.donate_via_github_sponsors),
      description = stringResource(Res.string.help_support_continued_development),
      url = "https://github.com/sponsors/static-var",
    )
  }
}

@Composable
private fun AboutSource() {
  PrismCardHost(modifier = Modifier.fillMaxWidth().cardMascotEligible(topClearance = Prism.dimens.spacingXl)) {
    PrismSurface(
      modifier = Modifier,
      color = Prism.color.surfaceVariant,
      shape = Prism.shapes.medium,
    ) {
      Column(modifier = Modifier.padding(Prism.dimens.spacingM)) {
        Text(
          text = stringResource(Res.string.from_the_scene_for_the_scene),
          style = Prism.typography.cardTitle,
          color = Prism.color.titleColor,
          modifier = Modifier.semantics { heading() },
        )
        Text(
          text = stringResource(Res.string.about_data_attribution),
          modifier = Modifier.padding(top = Prism.dimens.spacingS),
          style = Prism.typography.bodySmall,
          color = Prism.color.bodyColor,
        )
        AboutLink(title = stringResource(Res.string.visit_vlr_gg), url = "https://www.vlr.gg")
      }
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
      .clickable(role = Role.Button, onClickLabel = stringResource(Res.string.open_link_in_browser, title)) { uriHandler.openUri(url) }
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
