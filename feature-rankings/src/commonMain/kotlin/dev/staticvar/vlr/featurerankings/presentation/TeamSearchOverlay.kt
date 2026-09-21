/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismIconButton
import dev.staticvar.designsystem.component.button.PrismIconButtonSize
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.TeamSearchResult
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon
import dev.staticvar.vlr.sharedui.component.common.SharedScreenLoading
import org.jetbrains.compose.resources.stringResource
import vlr.feature_rankings.generated.resources.Res
import vlr.feature_rankings.generated.resources.close_team_search
import vlr.feature_rankings.generated.resources.no_teams_found
import vlr.feature_rankings.generated.resources.search_teams
import vlr.feature_rankings.generated.resources.search_teams_hint
import vlr.feature_rankings.generated.resources.searching_teams
import vlr.feature_rankings.generated.resources.try_another_team_name

@Composable
internal fun TeamSearchOverlay(
  state: TeamSearchUiState,
  onClose: () -> Unit,
  onQueryChanged: (String) -> Unit,
  onRetry: () -> Unit,
  onTeamSelected: (String) -> Unit,
) {
  val keyboard = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current
  AnimatedVisibility(
    visible = state.isOpen,
    enter = expandIn(tween(280), expandFrom = Alignment.TopEnd, initialSize = { IntSize(0, 0) }) + fadeIn(tween(160)),
    exit = shrinkOut(tween(280), shrinkTowards = Alignment.TopEnd, targetSize = { IntSize(0, 0) }) + fadeOut(tween(200)),
    modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.TopEnd),
  ) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
      withFrameNanos { }
      focusRequester.requestFocus()
      keyboard?.show()
    }
    Surface(modifier = Modifier.fillMaxSize(), color = Prism.color.background) {
      Column(modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = Prism.dimens.spacingM)) {
        PrismScreenTitleBar {
          val searchDescription = stringResource(Res.string.search_teams)
          BasicTextField(
            value = state.query,
            onValueChange = onQueryChanged,
            modifier = Modifier.weight(1f).focusRequester(focusRequester)
              .semantics { contentDescription = searchDescription },
            textStyle = Prism.typography.cardTitle.copy(color = Prism.color.titleColor),
            cursorBrush = SolidColor(Prism.color.accent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            decorationBox = { input ->
              Box(modifier = Modifier.padding(vertical = Prism.dimens.spacingM)) {
                if (state.query.isEmpty()) {
                  Text(
                    text = stringResource(Res.string.search_teams),
                    style = Prism.typography.cardTitle,
                    color = Prism.color.labelColor,
                  )
                }
                input()
              }
            },
          )
          PrismIconButton(
            icon = TeamSearchCloseIcon,
            contentDescription = stringResource(Res.string.close_team_search),
            size = PrismIconButtonSize.Toolbar,
            onClick = {
              focusManager.clearFocus()
              keyboard?.hide()
              onClose()
            },
          )
        }
        Text(
          text = stringResource(Res.string.search_teams_hint),
          style = Prism.typography.label,
          color = Prism.color.labelColor,
          modifier = Modifier.padding(bottom = Prism.dimens.spacingM),
        )
        when (val results = state.results) {
          TeamSearchResults.Idle -> Unit
          TeamSearchResults.Loading -> SharedScreenLoading(
            label = stringResource(Res.string.searching_teams),
            modifier = Modifier.fillMaxSize(),
          )
          is TeamSearchResults.Error -> SharedLoadError(
            errorMessage = results.message,
            errorDetails = null,
            onRefresh = onRetry,
            centered = true,
            modifier = Modifier.fillMaxSize(),
          )
          is TeamSearchResults.Success -> {
            if (results.teams.isEmpty()) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = Prism.dimens.spacingM),
                verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
              ) {
                Text(stringResource(Res.string.no_teams_found), style = Prism.typography.cardTitle, color = Prism.color.titleColor)
                Text(stringResource(Res.string.try_another_team_name), style = Prism.typography.bodySmall, color = Prism.color.labelColor)
              }
            } else {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
              ) {
                items(results.teams, key = { it.teamId }) { team ->
                  TeamSearchItem(team = team, onClick = {
                    focusManager.clearFocus()
                    keyboard?.hide()
                    onTeamSelected(team.teamId)
                  })
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TeamSearchItem(team: TeamSearchResult, onClick: () -> Unit) {
  PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined, onClick = onClick) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(team.teamName, style = Prism.typography.cardTitle, color = Prism.color.titleColor)
        team.shortName?.takeIf { it.isNotBlank() && !it.equals(team.teamName, ignoreCase = true) }?.let {
          Text(it, style = Prism.typography.bodySmall, color = Prism.color.labelColor)
        }
      }
      SharedNetworkIcon(
        imageUrl = team.teamLogo,
        contentDescription = team.teamName,
        size = PrismIconSize.Large,
        style = PrismIconStyle.Plain,
        parentBackground = PrismCardStyle.Outlined.containerColor,
        tint = PrismIconTint.None,
      )
    }
  }
}

internal val TeamSearchIcon: ImageVector = ImageVector.Builder(
  name = "TeamSearch", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
).apply {
  path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
    moveTo(16f, 10f)
    arcTo(6f, 6f, 0f, true, true, 4f, 10f)
    arcTo(6f, 6f, 0f, true, true, 16f, 10f)
    moveTo(14.5f, 14.5f)
    lineTo(21f, 21f)
  }
}.build()

private val TeamSearchCloseIcon: ImageVector = ImageVector.Builder(
  name = "TeamSearchClose", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
).apply {
  path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
    moveTo(6f, 6f)
    lineTo(18f, 18f)
    moveTo(18f, 6f)
    lineTo(6f, 18f)
  }
}.build()
