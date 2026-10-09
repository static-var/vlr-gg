/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.new_in_val_esports
import vlr.feature_about.generated.resources.release_1_1_1_introduction
import vlr.feature_about.generated.resources.release_1_1_1_ios_live_description
import vlr.feature_about.generated.resources.release_1_1_1_ios_live_title
import vlr.feature_about.generated.resources.release_1_1_1_predictions_description
import vlr.feature_about.generated.resources.release_1_1_1_predictions_title
import vlr.feature_about.generated.resources.release_1_1_1_stability_description
import vlr.feature_about.generated.resources.release_1_1_1_stability_title
import vlr.feature_about.generated.resources.release_1_1_1_team_form_description
import vlr.feature_about.generated.resources.release_1_1_1_team_form_title
import vlr.feature_about.generated.resources.see_what_s_new
import vlr.feature_about.generated.resources.what_s_new

/** Release notes shipped with the app, available without a network connection. */
public object BundledRelease {
  public const val id: String = "val-esports-1.1.1"
  public val title: StringResource = Res.string.what_s_new
  public val introduction: StringResource =
    Res.string.release_1_1_1_introduction
  public val bannerTitle: StringResource = Res.string.new_in_val_esports
  public val bannerAction: StringResource = Res.string.see_what_s_new

  public val highlights: List<Highlight> = listOf(
    Highlight(
      title = Res.string.release_1_1_1_predictions_title,
      description = Res.string.release_1_1_1_predictions_description,
    ),
    Highlight(
      title = Res.string.release_1_1_1_team_form_title,
      description = Res.string.release_1_1_1_team_form_description,
    ),
    Highlight(
      title = Res.string.release_1_1_1_ios_live_title,
      description = Res.string.release_1_1_1_ios_live_description,
      platform = ReleasePlatform.Ios,
    ),
    Highlight(
      title = Res.string.release_1_1_1_stability_title,
      description = Res.string.release_1_1_1_stability_description,
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
