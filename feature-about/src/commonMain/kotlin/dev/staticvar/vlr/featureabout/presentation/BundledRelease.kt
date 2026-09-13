/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.runtime.Immutable

/** Release notes shipped with the app, available without a network connection. */
public object BundledRelease {
  public const val id: String = "valorant-esports-launch"
  public const val title: String = "What's new?"
  public const val introduction: String =
    "Your esports companion has a new name. Explore the latest improvements in Valorant Esports."
  public const val bannerTitle: String = "New in Valorant Esports"
  public const val bannerAction: String = "See what's new"

  public val highlights: List<Highlight> = listOf(
    Highlight(
      title = "Meet Valorant Esports",
      description = "The app you knew as VLR is now Valorant Esports, with a refreshed look built around your matches and events.",
    ),
    Highlight(
      title = "Match and event details, redesigned",
      description = "Find schedules, results, brackets, standings, and the details that decided each match in clearer layouts.",
    ),
    Highlight(
      title = "Favorites in one place",
      description = "Favorite teams, players, matches, and events, then see the competition you care about on Home.",
    ),
    Highlight(
      title = "Favorite match widgets",
      description = "Keep favorite matches on your home screen with live scores and upcoming match times.",
    ),
    Highlight(
      title = "Built for bigger Android screens",
      platform = ReleasePlatform.Android,
      description = "Tablet and foldable layouts make better use of the extra room for easier browsing.",
    ),
  ).filter { it.platform == null || it.platform == releasePlatform }

  @Immutable
  public data class Highlight(
    public val title: String,
    public val description: String,
    public val platform: ReleasePlatform? = null,
  )
}

public enum class ReleasePlatform { Android, Ios }

internal expect val releasePlatform: ReleasePlatform
