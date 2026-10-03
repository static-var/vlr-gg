/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.staticvar.designsystem.prism.PrismTheme
import org.jetbrains.compose.resources.stringResource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.shared_loading
import vlr.shared_ui.generated.resources.shared_refreshing
import vlr.shared_ui.generated.resources.shared_retry
import vlr.shared_ui.generated.resources.shared_show_details

@RunWith(AndroidJUnit4::class)
class SharedOfflineRefreshTest {
  @get:Rule
  val compose = createAndroidComposeRule<SharedOfflineRefreshTestActivity>()

  @Test
  fun offlineRefreshIsClickableAndShowsProgressUntilTheRequestCompletes() {
    var refreshing by mutableStateOf(false)
    var refreshCount = 0
    lateinit var loadingLabel: String
    lateinit var refreshingLabel: String
    lateinit var retryLabel: String
    compose.setContent {
      PrismTheme {
        CompositionLocalProvider(LocalIsOnline provides false) {
          loadingLabel = stringResource(Res.string.shared_loading)
          refreshingLabel = stringResource(Res.string.shared_refreshing)
          retryLabel = stringResource(Res.string.shared_retry)
          Column {
            SharedRefreshButton(
              isLoading = false,
              isRefreshing = refreshing,
              hasContent = true,
              onRefresh = {
                refreshCount++
                refreshing = true
              },
              modifier = Modifier.testTag("refresh"),
            )
            SharedRefreshStatus(
              isRefreshing = refreshing,
              errorMessage = null,
              onRefresh = {
                refreshCount++
                refreshing = true
              },
            )
          }
        }
      }
    }

    compose.onNodeWithTag("refresh").assertIsEnabled().performClick()
    compose.onNodeWithTag("refresh").assertIsNotEnabled()
      .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, refreshingLabel))
    compose.onNodeWithContentDescription(loadingLabel).assertIsDisplayed()
    compose.onNodeWithText(retryLabel).assertDoesNotExist()
    compose.runOnIdle {
      assertEquals(1, refreshCount)
      refreshing = false
    }
    compose.onNodeWithTag("refresh").assertIsEnabled()
    compose.onNodeWithText(retryLabel).assertIsEnabled().performClick()
    compose.runOnIdle { assertEquals(2, refreshCount) }
  }

  @Test
  fun offlineRequestFailurePreservesInspectableDetails() {
    var errorMessage by mutableStateOf<String?>(null)
    var errorDetails by mutableStateOf<String?>(null)
    lateinit var detailsLabel: String
    compose.setContent {
      PrismTheme {
        CompositionLocalProvider(LocalIsOnline provides false) {
          detailsLabel = stringResource(Res.string.shared_show_details)
          SharedRefreshStatus(
            isRefreshing = false,
            errorMessage = errorMessage,
            errorDetails = errorDetails,
            onRefresh = {},
          )
        }
      }
    }

    compose.onNodeWithText(detailsLabel).assertDoesNotExist()
    compose.runOnIdle {
      errorMessage = "Request failed"
      errorDetails = "Connection timed out"
    }
    compose.onNodeWithText(detailsLabel).assertIsEnabled().performClick()
    compose.onNodeWithText("Request failed").assertIsDisplayed()
    compose.onNodeWithText("Connection timed out").assertIsDisplayed()
  }

  @Test
  fun offlineEmptyErrorOffersRetryAndShowsActualLoading() {
    var loading by mutableStateOf(false)
    var retryCount = 0
    lateinit var retryLabel: String
    lateinit var loadingLabel: String
    compose.setContent {
      PrismTheme {
        CompositionLocalProvider(LocalIsOnline provides false) {
          retryLabel = stringResource(Res.string.shared_retry)
          loadingLabel = stringResource(Res.string.shared_loading)
          if (loading) {
            SharedScreenLoading(label = "Loading matches")
          } else {
            SharedLoadError(
              errorMessage = "Request failed",
              errorDetails = null,
              centered = true,
              onRefresh = {
                retryCount++
                loading = true
              },
            )
          }
        }
      }
    }

    compose.onNodeWithText(retryLabel).assertIsEnabled().performClick()
    compose.onNodeWithText("Loading matches").assertIsDisplayed()
    compose.onNodeWithContentDescription(loadingLabel).assertIsDisplayed()
    compose.runOnIdle { assertEquals(1, retryCount) }
  }
}

class SharedOfflineRefreshTestActivity : ComponentActivity()
