package dev.staticvar.designsystem.component.carousel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardVariant
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant
import androidx.compose.ui.tooling.preview.PreviewParameter

@PrismPreview
@Composable
internal fun PrismCarouselPreview(
  @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant,
) {
  PrismTheme(variant = variant) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(Prism.color.background)
          .verticalScroll(rememberScrollState())
          .padding(vertical = Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXl),
    ) {
      Text(
        "Hero Carousel",
        style = Prism.typography.sectionTitle,
        color = Prism.color.titleColor,
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
      Text(
        "Single focal item with hard borders",
        style = Prism.typography.caption,
        color = Prism.color.labelColor,
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
      PrismCarousel(
        itemCount = 5,
        variant = PrismCarouselVariant.Hero,
        modifier = Modifier.fillMaxWidth(),
      ) { page ->
        PrismCard(
          modifier =
            Modifier.fillMaxWidth()
              .height(200.dp)
              .padding(horizontal = Prism.dimens.spacingM),
          variant = PrismCardVariant.Outlined,
        ) {
          Text("Featured match ${page + 1}", style = Prism.typography.cardTitle)
          Text("BRUTALIST HERO PANEL", style = Prism.typography.caption, color = Prism.color.labelColor)
        }
      }

      Text(
        "Multibrowse Carousel",
        style = Prism.typography.sectionTitle,
        color = Prism.color.titleColor,
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
      Text(
        "Center-focused cards with side peeks",
        style = Prism.typography.caption,
        color = Prism.color.labelColor,
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
      PrismCarousel(
        itemCount = 4,
        variant = PrismCarouselVariant.Multibrowse,
        modifier = Modifier.fillMaxWidth(),
      ) { page ->
        PrismCard(
          modifier =
            Modifier.fillMaxWidth()
              .height(180.dp)
              .padding(horizontal = Prism.dimens.spacingS),
          variant = PrismCardVariant.Filled,
        ) {
          Text("Standings block ${page + 1}", style = Prism.typography.cardTitle)
          Text("CENTER EMPHASIS", style = Prism.typography.caption, color = Prism.color.labelColor)
        }
      }

      Text(
        "Uncontained Carousel",
        style = Prism.typography.sectionTitle,
        color = Prism.color.titleColor,
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
      Text(
        "Compact rail for quick scan",
        style = Prism.typography.caption,
        color = Prism.color.labelColor,
        modifier = Modifier.padding(horizontal = Prism.dimens.spacingM),
      )
      PrismCarousel(
        itemCount = 6,
        variant = PrismCarouselVariant.Uncontained,
        modifier = Modifier.fillMaxWidth(),
        pageSpacing = Prism.dimens.spacingS,
      ) { page ->
        PrismCard(
          modifier =
            Modifier.fillMaxWidth()
              .height(120.dp)
              .padding(horizontal = Prism.dimens.spacingXs),
          variant = PrismCardVariant.Outlined,
        ) {
          Text("Item ${page + 1}", style = Prism.typography.button)
          Text("RAIL", style = Prism.typography.caption, color = Prism.color.labelColor)
        }
      }
    }
  }
}
