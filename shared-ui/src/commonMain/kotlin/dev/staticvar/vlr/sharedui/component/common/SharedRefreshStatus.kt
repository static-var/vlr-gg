/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.prism.Prism

@Composable
public fun SharedRefreshStatus(
  isRefreshing: Boolean,
  errorMessage: String?,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val status = when {
    isRefreshing -> RefreshStatus.Refreshing
    errorMessage != null -> RefreshStatus.Failed(errorMessage)
    else -> RefreshStatus.Idle
  }
  val animation = Prism.anim.standard
  AnimatedContent(
    targetState = status,
    modifier = modifier.fillMaxWidth(),
    contentAlignment = Alignment.TopStart,
    transitionSpec = {
      (fadeIn(animation.floatSpec()) togetherWith fadeOut(animation.floatSpec())).using(
        SizeTransform { _, _ -> tween(durationMillis = animation.durationMillis, easing = animation.easing) },
      )
    },
    label = "refresh_status",
  ) { currentStatus ->
    when (currentStatus) {
      RefreshStatus.Idle -> Box(Modifier.fillMaxWidth())
      RefreshStatus.Refreshing -> Column(
        modifier = Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM),
      ) {
        LinearProgressIndicator(
          modifier = Modifier.fillMaxWidth(),
          color = Prism.color.accent,
          trackColor = Prism.color.accentSubtle,
        )
      }
      is RefreshStatus.Failed -> Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = Prism.dimens.spacingM)
          .padding(horizontal = Prism.dimens.spacingM, vertical = Prism.dimens.spacingS)
          .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = currentStatus.message,
          modifier = Modifier.weight(1f),
          color = Prism.color.danger,
          style = Prism.typography.bodySmall,
        )
        PrismButton(onClick = onRefresh, style = PrismButtonStyle.Tertiary) {
          Text("Retry")
        }
      }
    }
  }
}

private sealed interface RefreshStatus {
  data object Idle : RefreshStatus
  data object Refreshing : RefreshStatus
  data class Failed(val message: String) : RefreshStatus
}
