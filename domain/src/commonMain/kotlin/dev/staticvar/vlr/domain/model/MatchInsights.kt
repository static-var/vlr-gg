/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

data class MatchInsights(
  val prediction: MatchPrediction? = null,
  val preMatchSummary: MatchSummary? = null,
  val postMatchSummary: MatchSummary? = null,
)

data class MatchSummary(val text: String, val generatedAt: String)
