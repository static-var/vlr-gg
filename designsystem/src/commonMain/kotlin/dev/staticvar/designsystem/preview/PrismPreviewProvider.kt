/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import dev.staticvar.designsystem.prism.PrismVariant

/**
 * Preview parameter provider for Prism theme variants.
 *
 * Automatically iterates through all [PrismVariant] enum entries,
 * generating previews for each theme. When new variants are added
 * to the enum, they automatically appear in previews.
 */
public class PrismPreviewProvider : PreviewParameterProvider<PrismVariant> {
  override val values: Sequence<PrismVariant> = sequence { yieldAll(PrismVariant.entries) }
}
