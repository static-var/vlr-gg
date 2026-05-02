/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.accordion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun PrismAccordionPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    LazyColumn(
      modifier = Modifier.fillMaxWidth().background(Prism.color.background),
      contentPadding = PaddingValues(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingL),
    ) {
      item { AccordionSectionLabel("Outlined Style") }
      item {
        BasicAccordionPreview(PrismAccordionStyle.Outlined, "Outlined", "Team roster", "Flat border, no elevation")
      }
      item { AccordionSectionLabel("Filled Style") }
      item {
        BasicAccordionPreview(PrismAccordionStyle.Filled, "Filled", "Settings", "Filled background, flat surface")
      }
      item { AccordionSectionLabel("Minimal Style") }
      item { BasicAccordionPreview(PrismAccordionStyle.Minimal, "Minimal", "FAQ", "No border, transparent") }
      item { AccordionSectionLabel("Controlled - Expanded") }
      item { ControlledAccordionPreview() }
      item { AccordionSectionLabel("With Nested Cards") }
      item { NestedCardsAccordionPreview() }
      item { Spacer(modifier = Modifier.height(Prism.dimens.spacingXl)) }
    }
  }
}

@Composable
private fun AccordionSectionLabel(text: String) {
  Text(text, style = Prism.typography.label, color = Prism.color.labelColor)
}

@Composable
private fun BasicAccordionPreview(style: PrismAccordionStyle, styleName: String, title: String, subtitle: String) {
  PrismAccordion(
    modifier = Modifier.fillMaxWidth(),
    style = style,
    header = { AccordionHeader(title = title, subtitle = subtitle) },
  ) {
    Text(
      "${styleName.lowercase().replaceFirstChar {
        it.uppercase()
      }} accordions show style-driven visuals",
      style = Prism.typography.bodyLarge,
    )
    Text("Useful for structured content groups", style = Prism.typography.bodySmall)
  }
}

@Composable
private fun ControlledAccordionPreview() {
  var expanded by remember { mutableStateOf(true) }
  PrismAccordion(
    modifier = Modifier.fillMaxWidth(),
    style = PrismAccordionStyle.Outlined,
    header = { AccordionHeader("Controlled accordion", "State is owned externally") },
    expanded = expanded,
    onExpandedChange = { expanded = it },
  ) {
    Text("This accordion is controlled externally", style = Prism.typography.bodyLarge)
    Text("Expand/collapse is fully flat", style = Prism.typography.bodySmall)
  }
}

@Composable
private fun NestedCardsAccordionPreview() {
  PrismAccordion(
    modifier = Modifier.fillMaxWidth(),
    style = PrismAccordionStyle.Filled,
    header = { AccordionHeader("Match cards", "Filled style with nested cards") },
    expanded = true,
  ) {
    AccordionPreviewCard("Paper Rex 13 - 10 FNATIC", "Lotus · Upper bracket final")
    AccordionPreviewCard("Player to watch", "Forsaken · 24/13/5 · ACS 285")
  }
}

@Composable
private fun AccordionHeader(title: String, subtitle: String) {
  Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
    Text(title, style = Prism.typography.bodyLarge)
    Text(subtitle, style = Prism.typography.caption, color = Prism.color.labelColor)
  }
}

@Composable
private fun AccordionPreviewCard(title: String, subtitle: String) {
  PrismCard(
    modifier = Modifier.fillMaxWidth(),
    style = PrismCardStyle.Outlined,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      Text(title, style = Prism.typography.bodyLarge)
      Text(subtitle, style = Prism.typography.bodySmall, color = Prism.color.labelColor)
    }
  }
}
