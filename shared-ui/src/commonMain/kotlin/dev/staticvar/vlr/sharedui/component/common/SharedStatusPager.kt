/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.sharedui.mascot.PauseCardMascots
import kotlinx.coroutines.flow.drop

@Composable
public fun SharedStatusPager(
  tabs: List<PrismTab>,
  selectedTabId: String,
  onTabSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  pageContent: @Composable (tabId: String, selectTab: (String) -> Unit) -> Unit,
) {
  val tabIds = tabs.map { it.id }
  val pagerState = rememberPagerState(
    initialPage = tabIds.indexOf(selectedTabId).coerceAtLeast(0),
    pageCount = { tabIds.size },
  )
  val currentSelection by rememberUpdatedState(selectedTabId)
  val selectTab by rememberUpdatedState(onTabSelected)
  var syncingSelection by remember { mutableStateOf(false) }

  LaunchedEffect(selectedTabId, tabIds) {
    val page = tabIds.indexOf(selectedTabId)
    if (page >= 0 && (page != pagerState.currentPage || pagerState.currentPageOffsetFraction != 0f || pagerState.isScrollInProgress)) {
      syncingSelection = true
      try {
        pagerState.animateScrollToPage(page)
      } finally {
        syncingSelection = false
      }
    }
  }

  LaunchedEffect(pagerState, tabIds) {
    snapshotFlow {
      Triple(pagerState.settledPage, pagerState.isScrollInProgress, syncingSelection)
    }.drop(1).collect { (page, scrolling, syncing) ->
      val tabId = tabIds.getOrNull(page)
      if (!scrolling && !syncing && tabId != null && tabId != currentSelection) {
        selectTab(tabId)
      }
    }
  }

  PauseCardMascots(pagerState.isScrollInProgress)
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismTabs(
      tabs = tabs,
      selectedTabId = tabIds.getOrElse(pagerState.currentPage) { selectedTabId },
      onTabSelected = { selectTab(it.id) },
    )
    HorizontalPager(
      state = pagerState,
      key = { tabIds[it] },
      modifier = Modifier.weight(1f).fillMaxSize(),
    ) { page ->
      pageContent(tabIds[page], selectTab)
    }
  }
}
