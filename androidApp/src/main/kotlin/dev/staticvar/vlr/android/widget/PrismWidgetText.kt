/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle

internal enum class PrismTextTone { Content, Secondary, Accent }

@Composable
internal fun PrismWidgetText(
  text: String,
  size: Int,
  tone: PrismTextTone = PrismTextTone.Content,
  maxLines: Int = 1,
  align: TextAlign = TextAlign.Start,
  description: String? = null,
  modifier: GlanceModifier = GlanceModifier,
) {
  Text(
    text = text,
    modifier = if (description == null) modifier else modifier.semantics { contentDescription = description },
    maxLines = maxLines,
    style = TextStyle(
      color = when (tone) {
        PrismTextTone.Content -> GlanceTheme.colors.onSurface
        PrismTextTone.Secondary -> GlanceTheme.colors.onSurfaceVariant
        PrismTextTone.Accent -> GlanceTheme.colors.primary
      },
      fontSize = size.sp,
      fontFamily = FontFamily("sans-serif-condensed"),
      textAlign = align,
    ),
  )
}
