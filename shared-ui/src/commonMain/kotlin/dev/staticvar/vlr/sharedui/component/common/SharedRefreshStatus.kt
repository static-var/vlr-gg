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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.prism.Prism

@Composable
public fun SharedRefreshStatus(
  isRefreshing: Boolean,
  errorMessage: String?,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
  errorDetails: String? = null,
  hasContent: Boolean = true,
) {
  val status = when {
    !LocalIsOnline.current && hasContent -> RefreshStatus.Failed("No internet connection", null)
    !LocalIsOnline.current -> RefreshStatus.Idle
    isRefreshing -> RefreshStatus.Refreshing
    errorMessage != null -> RefreshStatus.Failed(errorMessage, errorDetails)
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
        SharedLoadingIndicator()
      }
      is RefreshStatus.Failed -> SharedLoadError(
        errorMessage = currentStatus.message,
        errorDetails = currentStatus.details,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM),
      )
    }
  }
}

private sealed interface RefreshStatus {
  data object Idle : RefreshStatus
  data object Refreshing : RefreshStatus
  data class Failed(val message: String, val details: String?) : RefreshStatus
}
