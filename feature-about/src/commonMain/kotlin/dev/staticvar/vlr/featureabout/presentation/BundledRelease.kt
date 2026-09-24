/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.new_in_val_esports
import vlr.feature_about.generated.resources.release_1_0_5_appearance_description
import vlr.feature_about.generated.resources.release_1_0_5_appearance_title
import vlr.feature_about.generated.resources.release_1_0_5_favorites_description
import vlr.feature_about.generated.resources.release_1_0_5_favorites_title
import vlr.feature_about.generated.resources.release_1_0_5_introduction
import vlr.feature_about.generated.resources.release_1_0_5_live_android_description
import vlr.feature_about.generated.resources.release_1_0_5_live_ios_description
import vlr.feature_about.generated.resources.release_1_0_5_live_title
import vlr.feature_about.generated.resources.release_1_0_5_polish_description
import vlr.feature_about.generated.resources.release_1_0_5_polish_title
import vlr.feature_about.generated.resources.release_1_0_5_search_description
import vlr.feature_about.generated.resources.release_1_0_5_search_title
import vlr.feature_about.generated.resources.release_1_0_5_spotlight_description
import vlr.feature_about.generated.resources.release_1_0_5_spotlight_title
import vlr.feature_about.generated.resources.release_languages_description
import vlr.feature_about.generated.resources.release_languages_title
import vlr.feature_about.generated.resources.see_what_s_new
import vlr.feature_about.generated.resources.what_s_new

/** Release notes shipped with the app, available without a network connection. */
public object BundledRelease {
  public const val id: String = "val-esports-1.0.5"
  public val title: StringResource = Res.string.what_s_new
  public val introduction: StringResource =
    Res.string.release_1_0_5_introduction
  public val bannerTitle: StringResource = Res.string.new_in_val_esports
  public val bannerAction: StringResource = Res.string.see_what_s_new

  public val highlights: List<Highlight> = listOf(
    Highlight(
      title = Res.string.release_1_0_5_live_title,
      description = Res.string.release_1_0_5_live_ios_description,
      platform = ReleasePlatform.Ios,
    ),
    Highlight(
      title = Res.string.release_1_0_5_live_title,
      description = Res.string.release_1_0_5_live_android_description,
      platform = ReleasePlatform.Android,
    ),
    Highlight(
      title = Res.string.release_1_0_5_search_title,
      description = Res.string.release_1_0_5_search_description,
    ),
    Highlight(
      title = Res.string.release_1_0_5_appearance_title,
      description = Res.string.release_1_0_5_appearance_description,
    ),
    Highlight(
      title = Res.string.release_1_0_5_favorites_title,
      description = Res.string.release_1_0_5_favorites_description,
    ),
    Highlight(
      title = Res.string.release_languages_title,
      description = Res.string.release_languages_description,
    ),
    Highlight(
      title = Res.string.release_1_0_5_spotlight_title,
      description = Res.string.release_1_0_5_spotlight_description,
      platform = ReleasePlatform.Ios,
    ),
    Highlight(
      title = Res.string.release_1_0_5_polish_title,
      description = Res.string.release_1_0_5_polish_description,
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
