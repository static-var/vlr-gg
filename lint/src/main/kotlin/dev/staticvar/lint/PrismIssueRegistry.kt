/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API

public class PrismIssueRegistry : IssueRegistry() {
  override val issues = listOf(MaterialTypographyDetector.ISSUE, MaterialColorSchemeDetector.ISSUE)

  override val api: Int = CURRENT_API
  override val minApi: Int = CURRENT_API
}
