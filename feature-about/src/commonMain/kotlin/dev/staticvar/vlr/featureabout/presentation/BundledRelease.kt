/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.favorite_match_widgets
import vlr.feature_about.generated.resources.favorites_in_spotlight
import vlr.feature_about.generated.resources.new_in_val_esports
import vlr.feature_about.generated.resources.release_introduction
import vlr.feature_about.generated.resources.release_spotlight_description
import vlr.feature_about.generated.resources.release_widgets_description
import vlr.feature_about.generated.resources.see_what_s_new
import vlr.feature_about.generated.resources.what_s_new

/** Release notes shipped with the app, available without a network connection. */
public object BundledRelease {
  public const val id: String = "val-esports-1.0.2"
  public val title: StringResource = Res.string.what_s_new
  public val introduction: StringResource =
    Res.string.release_introduction
  public val bannerTitle: StringResource = Res.string.new_in_val_esports
  public val bannerAction: StringResource = Res.string.see_what_s_new

  public val highlights: List<Highlight> = listOf(
    Highlight(
      title = Res.string.favorite_match_widgets,
      description = Res.string.release_widgets_description,
    ),
    Highlight(
      title = Res.string.favorites_in_spotlight,
      platform = ReleasePlatform.Ios,
      description = Res.string.release_spotlight_description,
    ),
  ).filter { it.platform == null || it.platform == releasePlatform }

  @Immutable
  public data class Highlight(
    public val title: StringResource,
    public val description: StringResource,
    public val platform: ReleasePlatform? = null,
  )
}

public enum class ReleasePlatform { Android, Ios }

internal expect val releasePlatform: ReleasePlatform
