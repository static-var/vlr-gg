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
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardVariant
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant
import androidx.compose.ui.tooling.preview.PreviewParameter

@PrismPreview
@Composable
internal fun PrismAccordionPreview(
  @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant,
) {
  PrismTheme(variant = variant) {
    LazyColumn(
      modifier = Modifier.fillMaxWidth().background(Prism.color.background),
      contentPadding = PaddingValues(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingL),
    ) {
      // Outlined Variant
      item {
        Text("Outlined Variant", style = Prism.typography.label, color = Prism.color.labelColor)
      }
      item {
        PrismAccordion(
          modifier = Modifier.fillMaxWidth(),
          variant = PrismAccordionVariant.Outlined,
          header = {
            Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
              Text("Team roster", style = Prism.typography.bodyLarge)
              Text("Flat border, no elevation", style = Prism.typography.caption, color = Prism.color.labelColor)
            }
          },
        ) {
          Text("Outlined accordions have clear boundaries", style = Prism.typography.bodyLarge)
          Text("Perfect for lists and structured data", style = Prism.typography.bodySmall)
        }
      }

      // Filled Variant
      item {
        Text("Filled Variant", style = Prism.typography.label, color = Prism.color.labelColor)
      }
      item {
        PrismAccordion(
          modifier = Modifier.fillMaxWidth(),
          variant = PrismAccordionVariant.Filled,
          header = {
            Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
              Text("Settings", style = Prism.typography.bodyLarge)
              Text("Filled background, flat surface", style = Prism.typography.caption, color = Prism.color.labelColor)
            }
          },
        ) {
          Text("Filled accordions have a subtle background", style = Prism.typography.bodyLarge)
          Text("Ideal for settings or grouped options", style = Prism.typography.bodySmall)
        }
      }

      // Minimal Variant
      item {
        Text("Minimal Variant", style = Prism.typography.label, color = Prism.color.labelColor)
      }
      item {
        PrismAccordion(
          modifier = Modifier.fillMaxWidth(),
          variant = PrismAccordionVariant.Minimal,
          header = {
            Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
              Text("FAQ", style = Prism.typography.bodyLarge)
              Text("No border, transparent", style = Prism.typography.caption, color = Prism.color.labelColor)
            }
          },
        ) {
          Text("Minimal accordions blend into the background", style = Prism.typography.bodyLarge)
          Text("Best for subtle, secondary content", style = Prism.typography.bodySmall)
        }
      }

      // Controlled example
      item {
        Text("Controlled - Expanded", style = Prism.typography.label, color = Prism.color.labelColor)
      }
      item {
        var expanded by remember { mutableStateOf(true) }
        PrismAccordion(
          modifier = Modifier.fillMaxWidth(),
          variant = PrismAccordionVariant.Outlined,
          header = {
            Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
              Text("Controlled accordion", style = Prism.typography.bodyLarge)
              Text("State is owned externally", style = Prism.typography.caption, color = Prism.color.labelColor)
            }
          },
          expanded = expanded,
          onExpandedChange = { expanded = it },
        ) {
          Text("This accordion is controlled externally", style = Prism.typography.bodyLarge)
          Text("Expand/collapse is fully flat", style = Prism.typography.bodySmall)
        }
      }

      // Nested content example
      item {
        Text("With Nested Cards", style = Prism.typography.label, color = Prism.color.labelColor)
      }
      item {
        PrismAccordion(
          modifier = Modifier.fillMaxWidth(),
          variant = PrismAccordionVariant.Filled,
          header = {
            Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
              Text("Match cards", style = Prism.typography.bodyLarge)
              Text("Filled variant with nested cards", style = Prism.typography.caption, color = Prism.color.labelColor)
            }
          },
          expanded = true,
        ) {
          PrismCard(
            modifier = Modifier.fillMaxWidth(),
            variant = PrismCardVariant.Outlined,
          ) {
            Column(
              verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
            ) {
              Text("Paper Rex 13 - 10 FNATIC", style = Prism.typography.bodyLarge)
              Text("Lotus · Upper bracket final", style = Prism.typography.bodySmall, color = Prism.color.labelColor)
            }
          }
          PrismCard(
            modifier = Modifier.fillMaxWidth(),
            variant = PrismCardVariant.Outlined,
          ) {
            Column(
              verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
            ) {
              Text("Player to watch", style = Prism.typography.bodyLarge)
              Text("Forsaken · 24/13/5 · ACS 285", style = Prism.typography.bodySmall, color = Prism.color.labelColor)
            }
          }
        }
      }

      // Extra bottom padding to ensure proper animation measurement for the last accordion
      item {
        Spacer(modifier = Modifier.height(Prism.dimens.spacingXl))
      }
    }
  }
}
