/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBarStyle
import dev.staticvar.designsystem.component.button.PrismIconButton
import dev.staticvar.designsystem.component.button.PrismIconButtonSize
import dev.staticvar.designsystem.component.header.PrismHeader
import dev.staticvar.designsystem.component.selection.PrismSwitch
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.icon.about.StairStepAbout
import org.jetbrains.compose.resources.stringResource
import vlr.feature_rankings.generated.resources.Res
import vlr.feature_rankings.generated.resources.rankings_beta
import vlr.feature_rankings.generated.resources.rankings_info_title
import vlr.feature_rankings.generated.resources.rankings_regional
import vlr.feature_rankings.generated.resources.rankings_subtitle
import vlr.feature_rankings.generated.resources.rankings_title

@Composable
internal fun RankingsTitleBar(view: RankingsView, onViewSelected: (RankingsView) -> Unit, onInfo: () -> Unit) {
  val regionalLabel = stringResource(Res.string.rankings_regional)
  val spacing = Prism.dimens.spacingS
  PrismScreenTitleBar {
    Layout(
      modifier = Modifier.fillMaxWidth(),
      content = {
        PrismHeader(text = stringResource(Res.string.rankings_subtitle))
        Text(
          regionalLabel,
          style = Prism.typography.label,
          color = Prism.color.labelColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          modifier = Modifier.padding(start = Prism.dimens.spacingXs),
          horizontalArrangement = Arrangement.spacedBy(spacing),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            stringResource(Res.string.rankings_title),
            modifier = Modifier.weight(1f, fill = false),
            style = PrismScreenTitleBarStyle.Default.titleTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          PrismTag(text = stringResource(Res.string.rankings_beta), style = PrismTagStyle.Accent)
        }
        PrismSwitch(
          checked = view == RankingsView.Regional,
          onCheckedChange = { onViewSelected(if (it) RankingsView.Regional else RankingsView.Explore) },
          modifier = Modifier.testTag("rankings_regional_switch").semantics { contentDescription = regionalLabel },
          trackAlignment = Alignment.BottomCenter,
        )
        PrismIconButton(
          icon = StairStepAbout,
          contentDescription = stringResource(Res.string.rankings_info_title),
          size = PrismIconButtonSize.Toolbar,
          onClick = onInfo,
        )
      },
    ) { measurables, constraints ->
      val loose = constraints.copy(minWidth = 0, minHeight = 0)
      val switch = measurables[3].measure(loose)
      val info = measurables[4].measure(loose)
      val gap = spacing.roundToPx()
      val regional = measurables[1].measure(loose.copy(maxWidth = (constraints.maxWidth - info.width - gap).coerceAtLeast(0)))
      val regionalWidth = maxOf(regional.width, switch.width)
      val leftWidth = (constraints.maxWidth - regionalWidth - info.width - gap * 2).coerceAtLeast(0)
      val subtitle = measurables[0].measure(loose.copy(maxWidth = leftWidth))
      val title = measurables[2].measure(loose.copy(maxWidth = leftWidth))
      val baseline = maxOf(subtitle[FirstBaseline], regional[FirstBaseline])
      val subtitleY = baseline - subtitle[FirstBaseline]
      val regionalY = baseline - regional[FirstBaseline]
      val labelHeight = maxOf(subtitleY + subtitle.height, regionalY + regional.height)
      val controlHeight = maxOf(title.height, switch.height, info.height)
      val height = labelHeight + controlHeight
      val regionalX = leftWidth + gap
      layout(constraints.maxWidth, height) {
        subtitle.placeRelative(0, subtitleY)
        regional.placeRelative(regionalX + (regionalWidth - regional.width) / 2, regionalY)
        title.placeRelative(0, labelHeight + (controlHeight - title.height) / 2)
        switch.placeRelative(regionalX + (regionalWidth - switch.width) / 2, height - switch.height)
        info.placeRelative(constraints.maxWidth - info.width, height - info.height)
      }
    }
  }
}
